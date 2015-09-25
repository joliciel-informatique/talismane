///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Joliciel Informatique
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
package com.joliciel.talismane.filters;

/**
 * A sentence tag which is used to add arbitrary attributes to tokens found in this sentence.
 * @author Assaf Urieli
 *
 */
public class SentenceTag {
	private int startIndex;
	private int endIndex;
	private String attribute;
	private String value;
	
	public SentenceTag(int startIndex, String attribute, String value) {
		super();
		this.startIndex = startIndex;
		this.endIndex = -1;
		this.attribute = attribute;
		this.value = value;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}

	public String getAttribute() {
		return attribute;
	}

	public String getValue() {
		return value;
	}
}
