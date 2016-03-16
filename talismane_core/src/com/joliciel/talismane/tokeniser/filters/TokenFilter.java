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
package com.joliciel.talismane.tokeniser.filters;

import java.util.List;

/**
 * A filter that takes raw text, and finds tokens in the text (which are indicated by placeholders).<br/>
 * Note that, in addition to indicating tokens, it is possible to stipulate that
 * a sentence boundary will never be detected inside a placeholder.<br/>
 * Note that the tokeniser might still join the "atomic tokens" defined by the token filter
 * into larger tokens.
 * @author Assaf Urieli
 *
 */
public interface TokenFilter {
	/**
	 * Analyse the sentence, and provide placeholders for any tokens that will have to be formed.
	 */
	List<TokenPlaceholder> apply(String text);
}
