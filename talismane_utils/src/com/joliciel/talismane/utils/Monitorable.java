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

/**
 * A class whose progress can be monitored using a ProgressMonitor.
 * @author Assaf Urieli
 *
 */
public interface Monitorable {
	/**
	 * To be called just before a particular task is started.
	 * Returns a ProgressMonitor for this particular task.
	 * @param taskName the name of the task being started.
	 * @return
	 */
	public ProgressMonitor monitorTask();
	
}
