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
package com.joliciel.talismane.utils.features;

import java.util.List;

/**
 * An interface allowing a class to act as a container for name-to-featureClass maps.
 * @author Assaf Urieli
 *
 */
public interface FeatureClassContainer {
	/**
	 * Add a feature class mapping for a given name.
	 * Note that a name can be mapped to multiple classes, but that only one of these classes at most will be used
	 * at runtime.
	 * The class used at runtime will be the first class added using addFeatureClass which has a constructor
	 * corresponding to the arguments provided.
	 * @param name
	 * @param featureClass
	 */
	public void addFeatureClass(String name, @SuppressWarnings("rawtypes") Class<? extends Feature> featureClass);
	
	/**
	 * Return the feature classes currently mapped to the name provided.
	 * @param name
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<Class<? extends Feature>> getFeatureClasses(String name);
}
