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

class FeatureServiceImpl implements FeatureServiceInternal {

	@Override
	public FunctionDescriptor getFunctionDescriptor(String functionName) {
		FunctionDescriptorImpl descriptor = new FunctionDescriptorImpl();
		descriptor.setFeatureServiceInternal(this);
		descriptor.setFunctionName(functionName);
		return descriptor;
	}

	@Override
	public FunctionDescriptor getFunctionDescriptorForObject(Object object) {
		FunctionDescriptorImpl descriptor = new FunctionDescriptorImpl();
		descriptor.setFeatureServiceInternal(this);
		descriptor.setObject(object);
		return descriptor;
	}

	@Override
	public FunctionDescriptor getFunctionDescriptorForArgument(String argument) {
		FunctionDescriptorImpl descriptor = new FunctionDescriptorImpl();
		descriptor.setFeatureServiceInternal(this);
		descriptor.setObjectAsString(argument);
		return descriptor;
	}

	@Override
	public FunctionDescriptorParser getFunctionDescriptorParser() {
		FunctionDescriptorParserImpl parser = new FunctionDescriptorParserImpl();
		parser.setFeatureServiceInternal(this);
		return parser;
	}
}
