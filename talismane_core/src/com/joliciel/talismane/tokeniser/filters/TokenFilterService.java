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

public interface TokenFilterService {
	public static final String TOKEN_FILTER_DESCRIPTOR_KEY = "token_filter";
	public static final String TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY = "token_sequence_filter";
	
	/**
	 * Like {@link #getTokenRegexFilter(String, int, String)}, but the group defaults to 0.
	 * @param regex
	 * @param replacement
	 * @return
	 */
	public TokenFilter getTokenRegexFilter(String regex, String replacement);
	
	/**
	 * Get a token filter that applies a regex to a given text, and marks an matches found
	 * as tokens.<br/>
	 * In addition, if a replacement is provided, the text of the token will be set to the
	 * value of the replacement.<br/>
	 * If the surroundings of the token are important, the token itself can be indicated
	 * via a group in the regex.
	 * @param regex the regex to look for
	 * @param groupIndex the group index within the regex corresponding to the token being searched for
	 * @param replacement the text to be assigned to this token - can refer to groups in the regex via standard $1, $2 usage
	 * @return
	 */
	public TokenFilter getTokenRegexFilter(String regex, int groupIndex, String replacement);
	
	/**
	 * Gets a TokenSequenceFilter corresponding to a given descriptor.
	 * The descriptor should contain the class name, followed by any arguments, separated by tabs.
	 * @param descriptor
	 * @return
	 */
	public TokenSequenceFilter getTokenSequenceFilter(String descriptor);
	
	/**
	 * Gets a TokenFilter corresponding to a given descriptor.
	 * The descriptor should contain the class name, followed by any arguments, separated by tabs.
	 * @param descriptor
	 * @return
	 */
	public TokenFilter getTokenFilter(String descriptor);
	
	/**
	 * Get a TokenSequenceFilter that wraps a list of token filters.
	 * While it won't re-assign any token boundaries, it will check each TokenFilter against
	 * each individual token, and if a match is found, will replace the text.
	 * @param tokenFilter
	 * @return
	 */
	public TokenSequenceFilter getTokenSequenceFilter(List<TokenFilter> tokenFilters);
}
