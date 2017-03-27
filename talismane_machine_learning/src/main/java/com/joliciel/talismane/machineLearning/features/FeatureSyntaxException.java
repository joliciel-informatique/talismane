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
package com.joliciel.talismane.machineLearning.features;

import com.joliciel.talismane.utils.JolicielException;

/**
 * Occurs when the feature descriptor syntax cannot be parsed.
 * 
 * @author Assaf Urieli
 *
 */
public class FeatureSyntaxException extends JolicielException {
  private static final long serialVersionUID = -3315622182688781983L;
  private FunctionDescriptor descriptor;
  private FunctionDescriptor topLevelDescriptor;

  public FeatureSyntaxException(String message, FunctionDescriptor descriptor, FunctionDescriptor topLevelDescriptor) {
    super(message + ": " + descriptor.toString() + " in top-level descriptor: " + topLevelDescriptor.getDescriptorName());
    this.descriptor = descriptor;
    this.topLevelDescriptor = topLevelDescriptor;
  }

  public FunctionDescriptor getDescriptor() {
    return descriptor;
  }

  public FunctionDescriptor getTopLevelDescriptor() {
    return topLevelDescriptor;
  }

  public void setTopLevelDescriptor(FunctionDescriptor topLevelDescriptor) {
    this.topLevelDescriptor = topLevelDescriptor;
  }

}
