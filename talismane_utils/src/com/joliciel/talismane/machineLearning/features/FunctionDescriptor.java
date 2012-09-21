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
package com.joliciel.talismane.machineLearning.features;

import java.util.List;

/**
 * A textual descriptor of a function, used for parsing a list of textual function descriptors.
 * @author Assaf Urieli
 *
 */
public interface FunctionDescriptor {
	/**
	 * Returns true if function, false if literal.
	 * @return
	 */
	public boolean isFunction();
	
	/**
	 * The function name when this descriptor describes a function,
	 * otherwise null.
	 * @return
	 */
	public String getFunctionName();
	public void setFunctionName(String functionName);
	
	/**
	 * The list of arguments when this descriptor describes a function.
	 * @return
	 */
	public List<FunctionDescriptor> getArguments();
	
	/**
	 * Add an argument to this descriptor.
	 * If the argument is a FunctionDescriptor, will get added as is.
	 * Otherwise, will get added as FunctionDescriptor wrapper to the argument provided.
	 * @param argument
	 */
	public void addArgument(Object argument);
	
	/**
	 * When this function descriptor describes an object, rather
	 * than a function, the object it describes. Otherwise null.
	 * @return
	 */
	public Object getObject();
	
	/**
	 * A name given to this descriptor, or null otherwise.
	 * @return
	 */
	public String getDescriptorName();
	public void setDescriptorName(String descriptorName);
	
	/**
	 * Is this descriptor a binary operator?
	 * @return
	 */
	public boolean isBinaryOperator();
	public void setBinaryOperator(boolean binaryOperator);
	
	/**
	 * Is this descriptor an empty function (without a name, typically corresponding to open parenthesis).
	 * @return
	 */
	public boolean isEmpty();
	
	/**
	 * This function descriptor's parent.
	 * @return
	 */
	public FunctionDescriptor getParent();
}
