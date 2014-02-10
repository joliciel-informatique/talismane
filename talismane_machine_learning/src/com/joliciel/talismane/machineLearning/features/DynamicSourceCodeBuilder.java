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

/**
 * Builds "flattened" declarative source code for a given feature,
 * and makes it possible to retrieve the declarative version.
 * If feature.addDynamicSourceCode() returns false, will "dynamise" the
 * feature, that is, recursively replace its arguments with flattened dynamic versions,
 * where an argument is determined as a get/set method pair that refers to a Feature.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public interface DynamicSourceCodeBuilder<T> {
	/**
	 * Adds an argument to the source code and assigns it a value,
	 * returning the argument name to be used in the remainder of the source code.
	 */
	public String addFeatureVariable(Feature<T,?> feature, String nameBase);
	
	/**
	 * Append a line to the source code.
	 * @param string
	 */
	public void append(String string);
	
	/**
	 * Indent the source code by a tab.
	 */
	public void indent();
	
	/**
	 * Outdent the source code by a tab.
	 */
	public void outdent();
	
	/**
	 * Get the dynamic (or dynamised) feature constructed by this builder.
	 * @return
	 */
	public Feature<T,?> getFeature();
	
	/**
	 * Returns a unique variable name given a base.
	 * @return
	 */
	public String getVarName(String base);
	
	/**
	 * Add a class to be imported.
	 * @param importClass
	 */
	public void addImport(Class<?> importClass);
}
