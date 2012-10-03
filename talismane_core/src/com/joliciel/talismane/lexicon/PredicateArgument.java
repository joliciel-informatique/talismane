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
package com.joliciel.talismane.lexicon;

import java.util.Set;

/**
 * A single argument in a lexical entry's predicate-argument structure.
 * @author Assaf Urieli
 *
 */
public interface PredicateArgument {
	/**
	 * The function of the argument (e.g. Subject, Direct Object, etc.)
	 * @return
	 */
	public String getFunction();
	
	/**
	 * Whether or not this argument is optional.
	 * @return
	 */
	public boolean isOptional();
	
	/**
	 * The possible realisations of this argument (e.g. nominative clitic, noun phrase, etc.)
	 * @return
	 */
	public Set<String> getRealisations();
	
	/**
	 * This argument's index.
	 * @return
	 */
	public int getIndex();
}
