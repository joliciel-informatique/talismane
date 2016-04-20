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
package com.joliciel.talismane.regex.compiler;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import com.joliciel.talismane.regex.Flags;
import com.joliciel.talismane.regex.bytecode.AnyChar;
import com.joliciel.talismane.regex.bytecode.CharRange;
import com.joliciel.talismane.regex.bytecode.CompiledRegex;
import com.joliciel.talismane.regex.bytecode.InCharSet;
import com.joliciel.talismane.regex.bytecode.InNamedClass;
import com.joliciel.talismane.regex.bytecode.Instruction;
import com.joliciel.talismane.regex.bytecode.Jump;
import com.joliciel.talismane.regex.bytecode.MatchFound;
import com.joliciel.talismane.regex.bytecode.Save;
import com.joliciel.talismane.regex.bytecode.SomeChar;
import com.joliciel.talismane.regex.bytecode.Split;
import com.joliciel.talismane.regex.bytecode.WordBoundaryPredicate;
import com.joliciel.talismane.regex.parser.Capture;
import com.joliciel.talismane.regex.parser.Char;
import com.joliciel.talismane.regex.parser.CharSet;
import com.joliciel.talismane.regex.parser.NamedClass;
import com.joliciel.talismane.regex.parser.Node;
import com.joliciel.talismane.regex.parser.Parser;
import com.joliciel.talismane.regex.parser.PostOrderFolder;
import com.joliciel.talismane.regex.parser.Repeated;
import com.joliciel.talismane.regex.vm.VM;
import com.joliciel.talismane.resources.WordListFinder;

/**
 * Compile regular expression {@link Node}s into {@link VM} {@link Instruction}s. The implementation of the compiler
 * works in constant stack to avoid overflow on bug regular expressions (typically with a lot of alternatives).
 * 
 * Depending on the configuration, the compiler can persist the compiled result so that it can be loaded later on to
 * avoid having to recompile the regular expression. This is particularly useful when the regular expression represents
 * big resources with a lot of words.
 * 
 * @author Lucas Satabin
 *
 */
public class Compiler {

	private final boolean caseSensitive;
	private final boolean diacriticSensitive;
	private final boolean autoWordBoundaries;
	private final WordListFinder wordListFinder;

	private class CompilerWalker extends PostOrderFolder {

		public int nbSaved = 0;
		public Stack<InstructionBag> instructions = new Stack<>();
		private final int id;

		public CompilerWalker(int id) {
			this.id = id;
		}

		@Override
		protected void doNode(Node n) {
			switch (n.type) {
			// kind of poor man's pattern matching...
			case CHAR:
				char c = ((Char) n).character;
				if (caseSensitive && diacriticSensitive) {
					// char <c>
					instructions.push(new InstructionBag(new SomeChar(c)));
				} else {
					// in_charset <set>
					// compute the non accentuated version of this character
					char cNoAccent = Normalizer.normalize("" + c, Form.NFD)
							.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").charAt(0);
					SortedSet<CharRange> chars = new TreeSet<>();
					chars.add(new CharRange(c));
					if (!caseSensitive) {
						chars.add(new CharRange(Character.toLowerCase(c)));
						chars.add(new CharRange(Character.toUpperCase(c)));
						// consider non accentuated upper-case version of the
						// character
						chars.add(new CharRange(Character.toUpperCase(cNoAccent)));
					}
					if (!diacriticSensitive) {
						chars.add(new CharRange(cNoAccent));
						if (!caseSensitive) {
							// upper-case was already added in the previous
							// if-block
							chars.add(new CharRange(Character.toLowerCase(cNoAccent)));
						}
					}
					instructions.push(new InstructionBag(new InCharSet(chars)));
				}
				break;
			case CHARSET:
				// in_charset <set>
				instructions.push(new InstructionBag(new InCharSet(((CharSet) n).charset)));
				break;
			case DOT:
				// any
				instructions.push(new InstructionBag(AnyChar.INSTANCE));
				break;
			case EMPTY:
				// match
				if (id == -1) {
					instructions.push(new InstructionBag(MatchFound.ANONYMOUS));
				} else {
					instructions.push(new InstructionBag(new MatchFound(id)));
				}
				break;
			case WORD_BOUNDARY:
				// word boundary predicate
				instructions.push(new InstructionBag(WordBoundaryPredicate.INSTANCE));
				break;
			case CONCAT:
				// codes for e1
				// codes for e2
				InstructionBag snd = instructions.pop();
				InstructionBag fst = instructions.peek();
				fst.concat(snd);
				break;
			case REPEATED:
				final Repeated r = (Repeated) n;
				final int min = r.min;
				final int max = r.max;
				final InstructionBag repeated = instructions.peek().copy();
				if (max == -1) {
					// open repetition
					if (min > 0) {
						// codes for e (1)
						// ...
						// L1: codes for e (min)
						// split L1, L3
						// L3:
						InstructionBag b = instructions.peek();
						for (int i = 1; i < min; i++) {
							b.concat(repeated);
						}
						Split split = new Split(-repeated.size(), 1);
						b.append(split);
					} else {
						// L1: split L2, L3
						// L2: codes for e
						// jmp L1
						// L3:
						InstructionBag b = instructions.peek();
						Split split = new Split(1, 2 + b.size());
						Jump jmp = new Jump(-1 - b.size());
						b.prepend(split);
						b.append(jmp);
					}
				} else {
					// bounded repetition
					// codes for e (1)
					// ...
					// codes for e (min)
					// split L1, Lend
					// L1: codes for e (min + 1)
					// split L2, Lend
					// L2: codes for e (min + 2)
					// ...
					// Lmax: codes for e (max)
					// Lend:
					instructions.pop();
					InstructionBag b = new InstructionBag();
					for (int i = 1; i <= min; i++) {
						b.concat(repeated);
					}
					for (int i = min; i < max; i++) {
						Split split = new Split(1, (max - i) * (repeated.size() + 1));
						b.append(split);
						b.concat(repeated);
					}
					instructions.push(b);
				}
				break;
			case ALTERNATIVE:
				// split L1, L2
				// L1: codes for e1
				// jmp L3
				// L2: codes for e2
				// L3:
				snd = instructions.pop();
				fst = instructions.peek();
				Split split = new Split(1, 2 + fst.size());
				Jump jmp = new Jump(1 + snd.size());
				fst.prepend(split);
				fst.append(jmp);
				fst.concat(snd);
				break;
			case CAPTURE:
				// save 2*k
				// codes for e
				// save 2*k+1
				int nb = ((Capture) n).nb;
				InstructionBag b = instructions.peek();
				Save save1 = new Save(2 * nb);
				Save save2 = new Save(2 * nb + 1);
				b.prepend(save1);
				b.append(save2);
				nbSaved = Math.max(nbSaved, nb + 1);
				break;
			case NAMED_CLASS:
				// in_named_class name
				instructions.push(new InstructionBag(new InNamedClass(((NamedClass) n).name)));
				break;
			}
		}

	}

	public Compiler(WordListFinder wordListFinder) {
		this(0, wordListFinder);
	}

	public Compiler(int flags, WordListFinder wordListFinder) {
		this.caseSensitive = (flags & Flags.CASE_INSENSITIVE) == 0;
		this.diacriticSensitive = (flags & Flags.DIACRITICS_INSENSITIVE) == 0;
		this.autoWordBoundaries = (flags & Flags.AUTO_WORD_BOUNDARIES) == Flags.AUTO_WORD_BOUNDARIES;
		this.wordListFinder = wordListFinder;
	}

	public CompiledRegex compile(String regex) {
		return compile(new Parser(regex, wordListFinder).parse(), -1);
	}

	public CompiledRegex compile(String regex, int id) {
		return compile(new Parser(regex, wordListFinder).parse(), id);
	}

	public CompiledRegex compile(Node node) {
		return compile(node, -1);
	}

	public CompiledRegex compile(Node node, int id) {

		CompilerWalker walker = new CompilerWalker(id);

		// generate instructions
		walker.fold(node);

		InstructionBag prog = walker.instructions.pop();

		if (autoWordBoundaries) {
			// TODO add word boundary predicates where needed
		}

		return new CompiledRegex(walker.nbSaved, prog.compile(id));

	}

}
