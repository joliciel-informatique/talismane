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

interface FeatureServiceInternal extends FeatureService {
	/**
	 * Get a function descriptor that simply wraps an object,
	 * however, this argument is described by the argument provided as a string.
	 * If it can be parsed as an int, and doesn't contain a ".", the wrapped object will be an int.
	 * If it can be parsed as a double, the wrapped object will be a double.
	 * If it can be parsed as a boolean, the wrapped object will be a boolean.
	 * Otherwise, assumes it's a function name.
	 * @param argument
	 * @return
	 */
	public FunctionDescriptor getFunctionDescriptorForArgument(String argument);
	
	/**
	 * Get a function descriptor that simply wraps an object, rather than an actual function.
	 * @param object
	 * @return
	 */
	public FunctionDescriptor getFunctionDescriptorForObject(Object object);

}
