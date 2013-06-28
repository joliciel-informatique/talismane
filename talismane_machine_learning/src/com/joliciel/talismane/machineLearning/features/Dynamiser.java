///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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

import com.joliciel.talismane.utils.compiler.DynamicCompiler;

/**
 * Manages the construction of dynamic classes which flatten out
 * the feature structure into declarative code.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public interface Dynamiser<T> {
	/**
	 * Return the next available class index (to ensure class name uniqueness).
	 * @return
	 */
	public int nextClassIndex();
	
	/**
	 * Get a compiler to be used for compiling classes.
	 * @return
	 */
	public DynamicCompiler getCompiler();
	
	/**
	 * Get a new builder for a given feature.
	 * @param rootFeature the feature to build.
	 * @return
	 */
	public DynamicSourceCodeBuilder<T> getBuilder(Feature<T,?> rootFeature);
	
	public Class<?> getOutcomeType(Feature<T,?> feature);
	
	public Class<T> getContextClass();
}
