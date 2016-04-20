///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Safety Data -CFH
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.regex.vm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.joliciel.talismane.regex.Match;
import com.joliciel.talismane.regex.bytecode.CompiledRegex;
import com.joliciel.talismane.regex.bytecode.InCharSet;
import com.joliciel.talismane.regex.bytecode.InNamedClass;
import com.joliciel.talismane.regex.bytecode.Instruction;
import com.joliciel.talismane.regex.bytecode.MatchFound;
import com.joliciel.talismane.regex.bytecode.SomeChar;
import com.joliciel.talismane.regex.vm.ThreadManager.ReThread;
import com.joliciel.talismane.regex.vm.ThreadManager.ThreadQueue;
import com.joliciel.talismane.utils.CharPredicate;

import gnu.trove.set.TCharSet;
import gnu.trove.set.hash.TCharHashSet;

/**
 * A regular expression virtual machine, inspired by https://swtch.com/~rsc/regexp/.
 * 
 * This implementation is non-recursive and non-bactracking, allowing for quite decent performances and for running in
 * bounded memory.
 * 
 * {@link ReThread}s are executed in lock-step, so that no backtracking is required. Threads are discarded as soon as:
 * <ul>
 * <li>an instruction in the thread does not match,</li>
 * <li>or a thread with higher priority encountered a {@link MatchFound}.</li>
 * </ul>
 * 
 * @author Lucas Satabin
 *
 */
public final class VM {

	private final static TCharSet punctuation = new TCharHashSet("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toCharArray());

	private final static Map<String, CharPredicate> namedClasses;

	private VM() {
	}

	static {
		namedClasses = new HashMap<>();
		namedClasses.put("Lower", Character::isLowerCase);
		namedClasses.put("Upper", Character::isUpperCase);
		namedClasses.put("ASCII", c -> c >= 0x00 && c <= 0x7f);
		namedClasses.put("Alpha", Character::isAlphabetic);
		namedClasses.put("Digit", Character::isDigit);
		namedClasses.put("Alnum", c -> Character.isAlphabetic(c) || Character.isDigit(c));
		namedClasses.put("Punct", punctuation::contains);
	}

	public static Match execute(CompiledRegex program, CharSequence input, int startIdx) {

		int[] saved = new int[program.nbSaved * 2];
		Arrays.fill(saved, -1);

		Match match = null;

		ThreadManager manager = new ThreadManager(program.instructions, input);

		ThreadQueue current = new ThreadQueue();
		ThreadQueue next = new ThreadQueue();

		manager.schedule(manager.allocate(0, saved, false), startIdx, current);

		boolean cont = true;
		for (int idx = startIdx; cont && idx <= input.length(); idx++) {
			// for each character in the input string, execute the current
			// instruction for each thread.
			boolean discard = false;
			for (ReThread t : current) {
				if (discard) {
					manager.free(t);
					continue;
				}
				Instruction inst = program.instructions[t.pc];
				switch (inst.opcode) {
				case SOME_CHAR:
					if (idx < input.length() && ((SomeChar) inst).c == input.charAt(idx)) {
						t.pc++;
						manager.schedule(t, idx + 1, next);
					} else {
						manager.free(t);
					}
					break;
				case ANY_CHAR:
					t.pc++;
					manager.schedule(t, idx + 1, next);
					break;
				case CHAR_SET:
					if (idx < input.length() && ((InCharSet) inst).contains(input.charAt(idx))) {
						t.pc++;
						manager.schedule(t, idx + 1, next);
					} else {
						manager.free(t);
					}
					break;
				case NAMED_CLASS:
					String name = ((InNamedClass) inst).name;
					CharPredicate pred = namedClasses.get(name);
					if (pred != null) {
						if (pred.test(input.charAt(idx))) {
							t.pc++;
							manager.schedule(t, idx + 1, next);
						} else {
							manager.free(t);
						}
					} else {
						throw new RuntimeException("Unknown class name " + name);
					}
					break;
				case MATCH:
					// save the end pointer
					System.arraycopy(t.saved, 0, saved, 0, t.saved.length);
					int id = ((MatchFound) inst).id;
					match = new Match(startIdx, idx, input, saved, id);
					// discard lower priority threads in the current step
					discard = true;
					break;
				case JUMP:
				case PREDICATE:
				case SAVE:
				case SPLIT:
					throw new RuntimeException("opcode " + inst.opcode + " should have been handled by scheduler");
				}
			}
			// set the next thread list as current and go on
			current = next;
			next = new ThreadQueue();

			cont = !current.isEmpty();
		}
		return match;
	}

}
