///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
import java.util.ArrayList;

/**
 * Used for monitoring progress of a thread.
 * @author Assaf Urieli
 *
 */
public class SimpleProgressMonitor implements ProgressMonitor {
	double percentComplete;
	String currentAction;
	Object[] currentArguments = null;
	Exception exception;
	boolean finished = false;
	
	/* (non-Javadoc)
	 * @see com.joliciel.jochre.util.IProgressMonitor#getPercentComplete()
	 */
	@Override
	public double getPercentComplete() {
		return percentComplete;
	}
	public void setPercentComplete(double percentComplete) {
		this.percentComplete = percentComplete;
	}
	
	
	@Override
	public List<MessageResource> getCurrentActions() {
		List<MessageResource> list = new ArrayList<MessageResource>();
		if (this.currentAction.length()>0)
			list.add(new MessageResource(this.currentAction, this.currentArguments));
		return list;
	}

	/* (non-Javadoc)
	 * @see com.joliciel.jochre.util.IProgressMonitor#getCurrentAction()
	 */
	@Override
	public String getCurrentAction() {
		return currentAction;
	}
	public void setCurrentAction(String currentAction) {
		this.currentAction = currentAction;
		this.currentArguments = null;
	}
	
	public void setCurrentAction(String currentAction, Object[] arguments) {
		this.currentAction = currentAction;
		this.currentArguments = arguments;
	}
	public Exception getException() {
		return exception;
	}
	public void setException(Exception exception) {
		this.exception = exception;
	}
	@Override
	public boolean isFinished() {
		if (this.finished||this.exception!=null)
			return true;
		return (this.percentComplete>=1);
	}
	public void setFinished(boolean finished) {
		this.finished = finished;
		this.percentComplete=1;
	}
	public Object[] getCurrentArguments() {
		return currentArguments;
	}

	
	
}
