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
package com.joliciel.talismane.sentenceAnnotators;

import java.io.Serializable;

/**
 * A place-holder that will be replaced by a proper token when tokenising.
 * 
 * @author Assaf Urieli
 *
 */
public class TokenPlaceholder implements Serializable {
	private static final long serialVersionUID = 1L;
	private String replacement;

	public TokenPlaceholder(String replacement) {
		this.replacement = replacement;
	}

	/**
	 * The replacement text for this placeholder.
	 */
	public String getReplacement() {
		return this.replacement;
	}

	@Override
	public String toString() {
		return "TokenPlaceholder [replacement=" + replacement + "]";
	}
}