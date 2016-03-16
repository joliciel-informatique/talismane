///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Monitors the performance of various sub-tasks within an application run.<br/>
 * Configuration file keys:<br/>
 * <ul>
 * <li>talismane.monitoring.activated=true/false : whether the entire system is activated (default = false)</li>
 * <li>talismane.monitoring.default=true/false : whether individual monitors are active by default</li>
 * <li>talismane.monitor.[package name] : whether this whole package is active by default - sub-packages override parent package settings for their classes</li>
 * <li>talismane.monitor.[package + class name] : whether this specific class is active</li>
 * </ul>
 * @author Assaf Urieli
 *
 */
public class PerformanceMonitor {
	private static final Log LOG = LogFactory.getLog(PerformanceMonitor.class);
	private static final CSVFormatter CSV = new CSVFormatter();
	
	private static Deque<Task> tasks = new ArrayDeque<Task>();
	private static Map<String,TaskStats> taskStatMap = new HashMap<String, TaskStats>();
	private static DecimalFormat decFormat = (DecimalFormat) DecimalFormat.getNumberInstance(Locale.US);
	private static Task root = null;
	private static boolean activated = false;
	private static boolean defaultActive = false;
	private static Map<String,Boolean> activeMonitors = new HashMap<String, Boolean>();
	private static Map<String,PerformanceMonitor> monitors = new HashMap<String, PerformanceMonitor>();
	private static PerformanceMonitor rootMonitor = null;
	private static String filePath = null;
	
	private static final String ACTIVE_PREFIX = "talismane.monitor.";
	private static final String ALL_ACTIVE = "talismane.monitoring.activated";
	private static final String DEFAULT_ACTIVE = "talismane.monitoring.default";
	private static final String FILE_PATH = "talismane.monitoring.file";
	
	private String name;
	private String simpleName;
	private boolean active = false;
	private static int numStarts = 0;
	
	
	/**
	 * Must be called at start of the application run.<br/>
	 * Uses a config properties file.<br/>
	 * Lines starting with # will be skipped.
	 */
	public static void start(File configFile) {
		try {
			numStarts++;
			if (numStarts>1)
				return;
			
			if (configFile==null)
				return;
			
			Properties props = new Properties();
			props.load(new FileReader(configFile));
			
			for (Object keyObj : props.keySet()) {
				String key = (String) keyObj;
				if (key.equals(ALL_ACTIVE)) {
					boolean active = props.getProperty(key).equalsIgnoreCase("true");
					activated = active;
				} else if (key.equals(DEFAULT_ACTIVE)) {
					boolean active = props.getProperty(key).equalsIgnoreCase("true");
					defaultActive = active;
				} else if (key.equals(FILE_PATH)) {
					filePath = props.getProperty(key);
				} else if (key.startsWith(ACTIVE_PREFIX)) {
					String name = key.substring(ACTIVE_PREFIX.length());
					boolean active = props.getProperty(key).equalsIgnoreCase("true");
					activeMonitors.put(name, active);
				}
			}
			
			rootMonitor = new PerformanceMonitor("[[ROOT]]","ROOT");
			rootMonitor.setActive(true);

			if (monitors.size()>0) {
				for (PerformanceMonitor monitor : monitors.values()) {
					monitor.updateActive();
				}
			}
			rootMonitor.startTask("root");
		    decFormat.applyPattern("##0.00");
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	private PerformanceMonitor(String name, String simpleName) {
		this.name = name;
		this.simpleName = simpleName;
		this.updateActive();
	}
	
	private void updateActive() {
		if (!activated) {
			this.active = false;
			return;
		}
		String test = this.name;
		boolean isActive = defaultActive;
		while (test.length()>0) {
			Boolean active = activeMonitors.get(test);
			if (active!=null) {
				isActive = active;
				break;
			}
			int dotIndex = test.lastIndexOf('.');
			if (dotIndex<0)
				break;
			if (dotIndex>=0) {
				test = test.substring(0,dotIndex);
			}
		}
		this.active = isActive;
	}
	
	/**
	 * Get a monitor for the class provided.
	 */
	public static PerformanceMonitor getMonitor(@SuppressWarnings("rawtypes") Class clazz) {
		return getMonitor(clazz.getCanonicalName(), clazz.getSimpleName());
	}
	
	/**
	 * Get a monitor for the name provided.
	 */
	public static PerformanceMonitor getMonitor(String name, String simpleName) {
		PerformanceMonitor monitor = monitors.get(name);
		if (monitor==null) {
			monitor = new PerformanceMonitor(name, simpleName);
			monitors.put(name, monitor);
		}
		return monitor;
		
	}
	/**
	 * Indicates that a particular task is starting.
	 * It's safest to place a try block immediately after the startTask, and place the corresponding endTask
	 * in the finally block.
	 */
	public void startTask(String taskName) {
		if (!active)
			return;
		Task currentHead = tasks.peek();
		Task task = null;
		String localName = this.simpleName + "." + taskName;
		if (currentHead==null) {
			task = new Task(localName);
			if (root == null)
				root = task;
		} else {
			task = currentHead.subTasks.get(localName);
			if (task==null) {
				task = new Task(localName);
				currentHead.subTasks.put(localName, task);
			}
		}
		task.subTaskDuration = 0;
		
		task.startTime = System.currentTimeMillis();
		
		tasks.push(task);
	}
	
	/**
	 * Indicates that a particular task is ending.
	 * It's safest to place this in the finally block of a try block starting immediately after the corresponding startTask.
	 */
	public void endTask() {
		if (!active)
			return;
		Task task = tasks.pop();
		String localName = task.name;

		long duration = System.currentTimeMillis() - task.startTime;

		task.totalTime += duration;
		
		TaskStats taskStats = taskStatMap.get(localName);
		if (taskStats==null) {
			taskStats = new TaskStats(localName);
			taskStatMap.put(localName, taskStats);
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
		numStarts--;
		if (numStarts>0)
			return;
		if (!activated)
			return;
		
		rootMonitor.endTask();
		if (root.totalTime==0) {
			long duration = System.currentTimeMillis() - root.startTime;
			root.totalTime = duration;
		}
		logPerformance(root, 0, root, root.totalTime);
		
		if (filePath!=null) {
			try {
				Writer csvFileWriter = null;
				File csvFile = new File(filePath);
				
				csvFile.delete();
				csvFile.createNewFile();
				csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
				PerformanceMonitor.writePerformanceCSV(csvFileWriter);
				csvFileWriter.flush();
				csvFileWriter.close();
			} catch (Exception e) {
				LogUtils.logError(LOG, e);
			}
		}
	}
	
	static void logPerformance(Task task, int depth, Task parent, long rootTotalTime) {
		if (!activated)
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
	 */
	public static void writePerformanceCSV(Writer csvWriter) {
		if (!activated)
			return;
		Set<TaskStats> taskStatSet = new TreeSet<TaskStats>(taskStatMap.values());
		try {
			csvWriter.write(CSV.format("name")
					+ CSV.format("call count")
					+ CSV.format("total time (s)")
					+ CSV.format("%")
					+ CSV.format("average time (ms)")
					+ CSV.format("total time with subtasks (s)")
					+ CSV.format("%") + "\n");
			for (TaskStats stats : taskStatSet) {
				csvWriter.write(CSV.format(stats.name));
				csvWriter.write(CSV.format(stats.callCount));
				csvWriter.write(CSV.format((double)stats.totalTime / 1000));
				csvWriter.write(CSV.format(((double)stats.totalTime / (double) root.totalTime) * 100.0 ));
				csvWriter.write(CSV.format(((double)stats.totalTime / (double) stats.callCount) ));
				csvWriter.write(CSV.format((double)stats.totalTimeWithSubTasks / 1000));
				csvWriter.write(CSV.format(((double)stats.totalTimeWithSubTasks / (double) root.totalTime) * 100.0 ));
				csvWriter.write("\n");
			}
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
		}
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public static boolean isActivated() {
		return activated;
	}

	public static void setActivated(boolean active) {
		PerformanceMonitor.activated = active;
	}

	public static String getFilePath() {
		return filePath;
	}

	public static void setFilePath(String filePath) {
		PerformanceMonitor.filePath = filePath;
	}
}
