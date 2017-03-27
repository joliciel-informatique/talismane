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
import java.util.ArrayList;

/**
 * Used for monitoring progress of a thread.
 * 
 * @author Assaf Urieli
 *
 */
public class MultiTaskProgressMonitor implements ProgressMonitor {
  double percentComplete;
  String currentAction = "";
  Object[] currentArguments = null;
  double currentPercent;
  ProgressMonitor currentMonitor;
  Exception exception;
  boolean finished;

  public void startTask(ProgressMonitor monitor, double percentAllotted) {
    currentPercent = percentAllotted;
    this.currentMonitor = monitor;
  }

  public void endTask() {
    this.percentComplete += currentPercent;
    this.currentMonitor = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.joliciel.jochre.util.IProgressMonitor#getPercentComplete()
   */
  @Override
  public double getPercentComplete() {
    double realPercentComplete = percentComplete;
    if (currentMonitor != null)
      realPercentComplete += (currentPercent * currentMonitor.getPercentComplete());
    return realPercentComplete;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.joliciel.jochre.util.IProgressMonitor#getCurrentAction()
   */
  @Override
  public String getCurrentAction() {
    return this.currentAction;
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
    this.finished = true;
  }

  @Override
  public boolean isFinished() {
    if (this.finished)
      return true;
    return (this.percentComplete >= 1);
  }

  public void setFinished(boolean finished) {
    this.finished = finished;
    this.percentComplete = 1;
  }

  public Object[] getCurrentArguments() {
    return currentArguments;
  }

  @Override
  public List<MessageResource> getCurrentActions() {
    List<MessageResource> list = new ArrayList<MessageResource>();
    if (this.currentAction.length() > 0)
      list.add(new MessageResource(this.currentAction, this.currentArguments));
    if (this.currentMonitor != null)
      list.addAll(this.currentMonitor.getCurrentActions());
    return list;
  }
}
