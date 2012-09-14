///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.talismane.utils.util;

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class PerformanceMonitor {
	private static final Log LOG = LogFactory.getLog(PerformanceMonitor.class);
	private static Deque<Task> tasks = new ArrayDeque<Task>();
	private static DecimalFormat decFormat = (DecimalFormat) DecimalFormat.getNumberInstance(Locale.US);

	public static void start() {
		startTask("root");
	    decFormat.applyPattern("##0.00");
	}
	
	public static void startTask(String name) {
		Task currentHead = tasks.peek();
		Task task = null;
		if (currentHead==null) {
			task = new Task(name);			
		} else {
			task = currentHead.subTasks.get(name);
			if (task==null) {
				task = new Task(name);
				currentHead.subTasks.put(name, task);
			}
		}
		task.startTime = new Date().getTime();
		
		tasks.push(task);
	}
	
	public static void endTask(String name) {
		Task task = tasks.pop();
		if (!task.name.equals(name)) {
			LOG.error("Mismatched task start and end: " + task.name + ", " + name);
			return;
		}
		long totalTime = (new Date().getTime()) - task.startTime;
		task.totalTime += totalTime;
	}
	
	public static void end() {
		Task root = tasks.peekFirst();
		endTask("root");
		if (root.totalTime==0) {
			long totalTime = (new Date().getTime()) - root.startTime;
			root.totalTime = totalTime;
		}
		logPerformance(root, 0, root, root.totalTime);
	}
	
	static void logPerformance(Task task, int depth, Task parent, long rootTotalTime) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i<depth; i++)
			sb.append("-");
		if (depth>0) sb.append(" ");
		sb.append(task.name);
		sb.append(": ");
		sb.append(task.totalTime);
		sb.append(" (");
		sb.append(decFormat.format((double) task.totalTime * 100.0 / (double) parent.totalTime));
		sb.append("%)");
		sb.append(" (root: ");
		sb.append(decFormat.format((double) task.totalTime * 100.0 / (double) rootTotalTime));
		sb.append("%)");
		LOG.info(sb.toString());
		for (Task subTask : task.subTasks.values()) {
			logPerformance(subTask, depth+1, task, rootTotalTime);
		}
	}
	
	private static class Task {
		public String name;
		public Map<String,Task> subTasks = new HashMap<String,Task>();
		public long startTime;
		public long totalTime = 0;
		
		public Task(String name) {
			this.name = name;
		}

		@Override
		public int hashCode() {
			return this.name.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Task other = (Task) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
		
	}
}
