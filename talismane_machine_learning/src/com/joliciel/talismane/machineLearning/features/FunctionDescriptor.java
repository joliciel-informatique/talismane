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
	 * Add an argument to this descriptor (at the end of the argument list).
	 * If the argument is a FunctionDescriptor, will get added as is.
	 * Otherwise, will get added as FunctionDescriptor wrapper to the argument provided.
	 * @param argument
	 */
	public void addArgument(Object argument);
	
	/**
	 * Like addArgument(Object), but adds the argument at a particular index.
	 * @param index
	 * @param argument
	 */
	public void addArgument(int index, Object argument);
	
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
	 * The name of the group to which this descriptor belongs, or null otherwise.
	 * Several descriptors can be grouped together in a group, for mutual handling
	 * downstream by other descriptors.
	 * @return
	 */
	public String getGroupName();
	public void setGroupName(String groupName);
	
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
	
	/**
	 * Deep clone this function descriptor.
	 * @return
	 */
	public FunctionDescriptor cloneDescriptor();
	
	/**
	 * Replace a given named parameter (basically a placeholder) within this function descriptor
	 * by an actual argument value (a function descriptor to put instead of the placeholder).
	 * @param parameterName
	 * @param argument
	 */
	public void replaceParameter(String parameterName, FunctionDescriptor argument);
	
	/**
	 * Was this descriptor defined at the top level (a line of its own), or is it
	 * inside another descriptor.
	 * @return
	 */
	public boolean isTopLevelDescriptor();
}
