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
package com.joliciel.talismane.parser;

import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * A dependency arc, indicating a dependency relationship between two pos-tagged tokens,
 * one of which is the head and one of which is the dependent, with a particular functional label (e.g. subject, direct object, etc.).
 * @author Assaf Urieli
 *
 */
public interface DependencyArc extends Comparable<DependencyArc> {

	/**
	 * The head (or governor) of this dependency arc.
	 * @return
	 */
	public PosTaggedToken getHead();
	
	/**
	 * The dependent of this dependency arc.
	 * @return
	 */
	public PosTaggedToken getDependent();
	
	/**
	 * The functional label of this dependency arc.
	 * @return
	 */
	public String getLabel();

	/**
	 * The probability associated with this dependency arc.
	 * @return
	 */
	public double getProbability();
	public void setProbability(double probability);
	
	/**
	 * A comment regarding this depenency arc annotation.
	 * @return
	 */
	public String getComment();
	public void setComment(String comment);
}
