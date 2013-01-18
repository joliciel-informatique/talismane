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
package com.joliciel.talismane.utils;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Monitors the performance of various sub-tasks within an application run.
 * @author Assaf Urieli
 *
 */
public class PerformanceMonitor {
	private static final Log LOG = LogFactory.getLog(PerformanceMonitor.class);
	private static Deque<Task> tasks = new ArrayDeque<Task>();
	private static Map<String,TaskStats> taskStatMap = new HashMap<String, TaskStats>();
	private static DecimalFormat decFormat = (DecimalFormat) DecimalFormat.getNumberInstance(Locale.US);
	private static Task root = null;
	private static boolean active = false;
	
	/**
	 * Must be called at the very start of the application run.
	 */
	public static void start() {
		if (!active)
			return;
		startTask("root");
	    decFormat.applyPattern("##0.00");
	}
	
	/**
	 * Indicates that a particular task is starting.
	 * It's safest to place a try block immediately after the startTask, and place the corresponding endTask
	 * in the finally block.
	 * @param name
	 */
	public static void startTask(String name) {
		if (!active)
			return;
		Task currentHead = tasks.peek();
		Task task = null;
		if (currentHead==null) {
			task = new Task(name);
			if (root == null)
				root = task;
		} else {
			task = currentHead.subTasks.get(name);
			if (task==null) {
				task = new Task(name);
				currentHead.subTasks.put(name, task);
			}
		}
		task.subTaskDuration = 0;
		
		task.startTime = new Date().getTime();
		
		tasks.push(task);
	}
	
	/**
	 * Indicates that a particular task is ending.
	 * Must correspond to a startTask for the same task.
	 * It's safest to place this in the finally block of a try block starting immediately after the corresponding startTask.
	 * @param name
	 */
	public static void endTask(String name) {
		if (!active)
			return;
		Task task = tasks.pop();
		if (!task.name.equals(name)) {
			LOG.error("Mismatched task start and end: " + task.name + ", " + name);
			return;
		}
		long duration = (new Date().getTime()) - task.startTime;

		task.totalTime += duration;
		
		TaskStats taskStats = taskStatMap.get(name);
		if (taskStats==null) {
			taskStats = new TaskStats(name);
			taskStatMap.put(name, taskStats);
		}
		taskStats.callCount = taskStats.callCount + 1;
		
		long durationWithoutSubtasks = duration - task.subTaskDuration;
		taskStats.totalTime = taskStats.totalTime + durationWithoutSubtasks;
		taskStats.totalTimeWithSubTasks = taskStats.totalTimeWithSubTasks + duration;
		
		Task parent = tasks.peek();
		if (parent!=null) {
			parent.subTaskDuration += duration;
		}
	}
	
	/**
	 * Must be called at the end of the application run, preferably in a finally block.
	 */
	public static void end() {
		if (!active)
			return;
		endTask("root");
		if (root.totalTime==0) {
			long duration = (new Date().getTime()) - root.startTime;
			root.totalTime = duration;
		}
		logPerformance(root, 0, root, root.totalTime);
	}
	
	static void logPerformance(Task task, int depth, Task parent, long rootTotalTime) {
		if (!active)
			return;
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
		public long subTaskDuration = 0;
		
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
	
	private static class TaskStats implements Comparable<TaskStats> {
		public String name;
		public long totalTime;
		public long totalTimeWithSubTasks;
		public int callCount;
		
		public TaskStats(String name) {
			this.name = name;
		}

		@Override
		public int compareTo(TaskStats o) {
			if (this.totalTime<o.totalTime) {
				return 1;
			} else if (this.totalTime>o.totalTime) {
				return -1;
			} else {
				int nameCompare = this.name.compareTo(o.name);
				if (nameCompare!=0) return nameCompare;
				return this.hashCode()-o.hashCode();
			}
		}
	}
	
	/**
	 * Write the performance measurements to a CSV file.
	 * @param csvWriter
	 */
	public static void writePerformanceCSV(Writer csvWriter) {
		if (!active)
			return;
		Set<TaskStats> taskStatSet = new TreeSet<TaskStats>(taskStatMap.values());
		try {
			csvWriter.write(CSVFormatter.format("name")
					+ CSVFormatter.format("call count")
					+ CSVFormatter.format("total time (s)")
					+ CSVFormatter.format("%")
					+ CSVFormatter.format("average time (ms)")
					+ CSVFormatter.format("total time with subtasks (s)")
					+ CSVFormatter.format("%") + "\n");
			for (TaskStats stats : taskStatSet) {
				csvWriter.write(CSVFormatter.format(stats.name));
				csvWriter.write(CSVFormatter.format(stats.callCount));
				csvWriter.write(CSVFormatter.format((double)stats.totalTime / 1000));
				csvWriter.write(CSVFormatter.format(((double)stats.totalTime / (double) root.totalTime) * 100.0 ));
				csvWriter.write(CSVFormatter.format(((double)stats.totalTime / (double) stats.callCount) ));
				csvWriter.write(CSVFormatter.format((double)stats.totalTimeWithSubTasks / 1000));
				csvWriter.write(CSVFormatter.format(((double)stats.totalTimeWithSubTasks / (double) root.totalTime) * 100.0 ));
				csvWriter.write("\n");
			}
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
		}
	}

	public static boolean isActive() {
		return active;
	}

	public static void setActive(boolean active) {
		PerformanceMonitor.active = active;
	}
	
	
}
