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

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

import com.joliciel.talismane.regex.bytecode.Instruction;
import com.joliciel.talismane.regex.bytecode.Jump;
import com.joliciel.talismane.regex.bytecode.Predicate;
import com.joliciel.talismane.regex.bytecode.Save;
import com.joliciel.talismane.regex.bytecode.Split;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * A manager that allows for scheduling threads. First scheduled ones have higher priority and will be executed before
 * threads scheduled next in this queue.
 * 
 * There are at most {@code n} threads in a queue where {@code n} is the number of instructions in the program. If a
 * thread is already present in the queue for a given program instruction, no other thread will be subsequently
 * scheduled in this queue for this instruction.
 * 
 * @author Lucas Satabin
 *
 */
final class ThreadManager {

	private final Instruction[] program;
	private final CharSequence input;

	private ReThread free = null;

	public ThreadManager(Instruction[] program, CharSequence input) {
		this.program = program;
		this.input = input;
	}

	/**
	 * Schedules the thread in this {@link ThreadManager}. Control {@link Instruction}s such as {@link Split},
	 * {@link Jump}, {@link Save} and {@link Predicate} which do not consume any input are executed right away and next
	 * instructions in the threads are scheduled recursively. This ensure that the priority between threads is
	 * respected.
	 */
	public void schedule(ReThread thread, int idx, ThreadQueue threads) {
		// stack holding the thread to schedule
		Stack<ReThread> toSchedule = new Stack<>();
		toSchedule.push(thread);
		while (!toSchedule.isEmpty()) {
			ReThread t = toSchedule.pop();
			Instruction inst = program[t.pc];
			switch (inst.opcode) {
			case SPLIT: {
				Split split = (Split) inst;
				// schedule the second thread with second target pc
				ReThread t1 = allocate(split.next2, t.saved, true);
				toSchedule.push(t1);
				// schedule the first thread with first target pc
				t.pc = split.next1;
				toSchedule.push(t);
				break;
			}
			case JUMP: {
				// schedule the thread with pc at target
				Jump jump = (Jump) inst;
				t.pc = jump.next;
				toSchedule.push(t);
				break;
			}
			case SAVE: {
				// save the current position
				Save save = (Save) inst;
				if (t.forked) {
					t.saved = Arrays.copyOf(t.saved, t.saved.length);
					t.forked = false;
				}
				t.saved[save.nb] = idx;
				t.pc++;
				toSchedule.push(t);
				break;
			}
			case PREDICATE: {
				// if predicate holds, then schedule next instruction in
				// thread without consuming any character from the input
				Predicate pred = (Predicate) inst;
				if (pred.apply(input, idx)) {
					t.pc++;
					toSchedule.push(t);
				}
				break;
			}
			default:
				// other instruction, just push it into the queue
				threads.offer(t);
			}
		}
	}

	public ReThread allocate(int pc, int[] saved, boolean forked) {
		final ReThread t;
		if (free == null) {
			t = new ReThread(pc, saved, forked);
		} else {
			t = free;
			t.pc = pc;
			t.saved = saved;
			t.forked = forked;
			free = t.next;
			t.next = null;
		}
		return t;
	}

	public void free(ReThread thread) {
		if (free != null) {
			thread.next = free;
		}
		free = thread;
	}

	/**
	 * A thread keeps track of the current instruction to execute and the saved position so far.
	 * 
	 * @author Lucas Satabin
	 *
	 */
	public static class ReThread {
		public int pc;
		public int[] saved;
		public boolean forked;
		ReThread next = null;

		private ReThread(int pc, int[] saved, boolean forked) {
			this.pc = pc;
			this.saved = saved;
			this.forked = forked;
		}

	}

	public static class ThreadQueue extends AbstractQueue<ReThread> {

		private final TIntSet pcs = new TIntHashSet();
		private final LinkedList<ReThread> internal = new LinkedList<>();

		@Override
		public boolean offer(ReThread t) {
			if (pcs.contains(t.pc)) {
				return false;
			} else {
				pcs.add(t.pc);
				return internal.offer(t);
			}
		}

		@Override
		public ReThread poll() {
			if (internal.isEmpty()) {
				return null;
			} else {
				ReThread t = internal.poll();
				pcs.remove(t.pc);
				return t;
			}
		}

		@Override
		public ReThread peek() {
			return internal.peek();
		}

		@Override
		public Iterator<ReThread> iterator() {
			return internal.iterator();
		}

		@Override
		public int size() {
			return internal.size();
		}

	}

}
