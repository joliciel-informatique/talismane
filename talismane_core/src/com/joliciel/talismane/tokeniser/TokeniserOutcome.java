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
package com.joliciel.talismane.tokeniser;

/**
 * A classification that can be assigned the tokeniser to an interval between two atomic tokens.
 * @author Assaf Urieli
 *
 */
public enum TokeniserOutcome implements TokenTag {
	/**
	 * The current interval does not separate the atomic tokens on either side.
	 */
	JOIN,
	
	/**
	 * The current interval separates the atomic tokens on either side.
	 */
	SEPARATE;
	
	@Override
	public String getCode() {
		return this.name();
	}
}