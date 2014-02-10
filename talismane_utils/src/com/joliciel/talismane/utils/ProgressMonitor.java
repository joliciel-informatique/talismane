///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.utils;

import java.util.List;

/**
 * Used for monitoring progress of tasks, so that percent complete and current action
 * can be displayed to the user in a multi-threaded environment.
 * @author Assaf Urieli
 *
 */
public interface ProgressMonitor {

	/**
	 * The percent of the task completed.
	 * @return
	 */
	public abstract double getPercentComplete();

	/**
	 * Get a List of current actions (resource strings)
	 * @return
	 */
	public abstract List<MessageResource> getCurrentActions();
	
	/**
	 * The current action being performed.
	 * @return
	 */
	public abstract String getCurrentAction();
	
	/**
	 * The arguments of the current action being performed.
	 * @return
	 */
	public abstract Object[] getCurrentArguments();
	
	/**
	 * The unhandled exception that was thrown, if any, during the task being monitored.
	 * @return
	 */
	public abstract Exception getException();
	
	/**
	 * Is the task finished?
	 * @return
	 */
	public boolean isFinished();

}