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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FunctionDescriptorImpl implements FunctionDescriptor {
	private static final Logger LOG = LoggerFactory.getLogger(FunctionDescriptorImpl.class);
	FeatureServiceInternal featureServiceInternal;
	String functionName = null;
	List<FunctionDescriptor> arguments = new ArrayList<FunctionDescriptor>();
	Object object = null;
	String descriptorName = null;
	boolean binaryOperator = false;
	boolean empty = false;
	FunctionDescriptor parent;
	String groupName = null;

	public String getFunctionName() {
		return functionName;
	}

	public void setFunctionName(String functionName) {
		if (this.object!=null) 
			throw new RuntimeException("Descriptor cannot be both a literal and a function");

		this.functionName = functionName;
		if (functionName.length()==0)
			empty=true;
	}

	public List<FunctionDescriptor> getArguments() {
		return arguments;
	}

	@Override
	public void addArgument(int index, Object argument) {
		if (argument == null)
			throw new RuntimeException("Cannot add a null argument!");
		
		FunctionDescriptor argumentFunction = null;
		if (argument instanceof FunctionDescriptor)
			argumentFunction = (FunctionDescriptor) argument;
		else
			argumentFunction = this.featureServiceInternal.getFunctionDescriptorForObject(argument);
		
		if (index<0)
			arguments.add(argumentFunction);
		else
			arguments.add(index, argumentFunction);
		
		if (LOG.isTraceEnabled())
			LOG.trace("Add argument: " + this.toString());
		
		((FunctionDescriptorImpl)argumentFunction).setParent(this);
	}
	
	@Override
	public void addArgument(Object argument) {
		this.addArgument(-1, argument);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.functionName!=null) {
			sb.append(this.functionName);
			sb.append("(");
		} else if (this.object!=null) {
			if (this.object instanceof String)
				sb.append("\"" + this.object.toString() + "\"");
			else
				sb.append(this.object.toString());
		} if (this.arguments.size()>0) {
			boolean firstArgument = true;
			for (FunctionDescriptor argument : arguments) {
				if (!firstArgument)
					sb.append(",");
				sb.append(argument.toString());
				firstArgument = false;
			}
		}
		if (this.functionName!=null) {
			sb.append(")");
		}
		return sb.toString();
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		if (this.functionName!=null)
			throw new RuntimeException("Descriptor cannot be both a function and a literal");
		this.object = object;
	}
	
	public void setObjectAsString(String argumentString) {
		if (this.object==null) {
			if (argumentString.contains(".")) {
				try {
					double doubleArgument = Double.parseDouble(argumentString);
					this.setObject(doubleArgument);
				} catch (NumberFormatException nfe) {
					// nothing to do here
				}
			}
		}
		
		if (this.object==null) {
			try {
				int intArgument = Integer.parseInt(argumentString);
				this.setObject(intArgument);
			} catch (NumberFormatException nfe) {
				// nothing to do here
			}
		}
		
		if (this.object==null) {
			if (argumentString.equals("true")) {
				this.setObject(true);
			} else if (argumentString.equals("false")) {
				this.setObject(false);
			}
		}
		
		if (this.object==null) {
			// assume it's a function not followed by parentheses
			this.functionName = argumentString;
		}
	}

	public FeatureServiceInternal getFeatureServiceInternal() {
		return featureServiceInternal;
	}

	public void setFeatureServiceInternal(
			FeatureServiceInternal featureServiceInternal) {
		this.featureServiceInternal = featureServiceInternal;
	}

	public String getDescriptorName() {
		return descriptorName;
	}

	public void setDescriptorName(String descriptorName) {
		this.descriptorName = descriptorName;
	}

	@Override
	public boolean isFunction() {
		return this.object==null;
	}

	public boolean isBinaryOperator() {
		return binaryOperator;
	}

	public void setBinaryOperator(boolean binaryOperator) {
		this.binaryOperator = binaryOperator;
	}

	@Override
	public boolean isEmpty() {
		return empty;
	}

	public FunctionDescriptor getParent() {
		return parent;
	}

	public void setParent(FunctionDescriptor parent) {
		this.parent = parent;
	}

	@Override
	public FunctionDescriptor cloneDescriptor() {
		FunctionDescriptorImpl descriptor = new FunctionDescriptorImpl();
		if (this.isFunction()) {
			descriptor.setFunctionName(this.getFunctionName());
			if (this.isBinaryOperator())
				descriptor.setBinaryOperator(true);
			for (FunctionDescriptor argument : this.getArguments()) {
				descriptor.addArgument(argument.cloneDescriptor());
			}
		} else {
			descriptor.setObject(this.getObject());
		}
		descriptor.setGroupName(this.getGroupName());
		return descriptor;
	}

	@Override
	public void replaceParameter(String parameterName,
			FunctionDescriptor argument) {
		List<Integer> replaceIndexes = new ArrayList<Integer>();
		
		int i = 0;
		for (FunctionDescriptor descriptor : this.getArguments()) {
			if (descriptor.isFunction() && descriptor.getFunctionName().equals(parameterName)) {
				replaceIndexes.add(i);
			}
			i++;
		}
		for (int index : replaceIndexes) {
			this.getArguments().remove(index);
			this.getArguments().add(index, argument);
		}
		i = 0;
		for (FunctionDescriptor descriptor : this.getArguments()) {
			if (replaceIndexes.contains(i))
				continue;
			descriptor.replaceParameter(parameterName, argument);
			i++;
		}
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	@Override
	public boolean isTopLevelDescriptor() {
		return this.getParent()==null;
	}

}
