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
package com.joliciel.talismane.machineLearning;

import java.io.Serializable;
import java.util.List;

import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * An external resource which returns a (possibly empty) list of weighted classes matching
 * a list of key elements.
 * @author Assaf Urieli
 *
 */
public interface ExternalResource extends Serializable {
	/**
	 * A unique name for this resource.
	 * @return
	 */
	public String getName();
	
	/**
	 * Return the class corresponding to the key elements provided.
	 * If {@link #isMultivalued()}, returns a class at random.
	 * @param keyElements
	 * @return
	 */
	public String getResult(List<String> keyElements);

	/**
	 * Return the weighted classes corresponding the the key elements provided.
	 * The list can be empty or null if no classes are found.
	 * If not {@link #isMultivalued()}, returns the single class with a weight of 1.
	 * @param keyElements
	 * @return
	 */
	public List<WeightedOutcome<String>> getResults(List<String> keyElements);
	
	/**
	 * Can there be more than one class per key.
	 * @return
	 */
	public boolean isMultivalued();

}
