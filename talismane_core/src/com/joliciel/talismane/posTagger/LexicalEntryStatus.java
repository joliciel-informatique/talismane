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
package com.joliciel.talismane.posTagger;

/**
 * A status, used only to select a single entry in the case of homographs,
 * to indicate which lemma should be given preference.
 * 
 * @author Assaf Urieli
 *
 */
public enum LexicalEntryStatus {
	/**
	 * Used when no homograph exists, or when this is the most likely homograph.
	 */
	NEUTRAL,
	
	/**
	 * This is a somewhat arbitrary decision, as between "fils" (for fil) and "fils" for (for fils) in French.
	 */
	SOMEWHAT_UNLIKELY,
	
	/**
	 * Gives a definite preference to another homograph.
	 */
	UNLIKELY,
	
	/**
	 * This entry should be ignored - it is not a true homograph.
	 */
	WRONG;
	
	
}
