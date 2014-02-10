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
package com.joliciel.talismane.posTagger;

import java.io.Serializable;
import java.util.Locale;
import java.util.Set;

import com.joliciel.talismane.machineLearning.DecisionFactory;

/**
 * A tag set to be used for pos tagging.
 * @author Assaf Urieli
 *
 */
public interface PosTagSet extends Serializable, DecisionFactory<PosTag> {
	/**
	 * Name of this posTagSet.
	 * @return
	 */
	public String getName();
	
	/**
	 * The locale to which this PosTagSet applies.
	 * @return
	 */
	public Locale getLocale();
	
	/**
	 * Returns the full tagset.
	 */
	Set<PosTag> getTags();
	
	/**
	 * Return the PosTag corresponding to a given code.
	 * @param code
	 */
	PosTag getPosTag(String code);
}
