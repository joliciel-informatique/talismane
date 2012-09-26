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
 * A corpus reader that expects one pos-tagged token per line,
 * and analyses the line content based on a regex supplied during construction.<br/>
 * The regex needs to contain two capturing groups: one for the token and one for the postag code.<br/>
 * These groups need to be indicated in the regex expression by the strings "TOKEN" and "POSTAG". These values will be replaced
 * by (.*) for the token, and (.+) for the postag.<br/>
 * @author Assaf Urieli
 *
 */
public interface PosTagRegexBasedCorpusReader extends PosTagAnnotatedCorpusReader {
	/**
	 * The default regex (if none is set).
	 */
	public static final String DEFAULT_REGEX = ".*\\tTOKEN\\t.*\\tPOSTAG\\t.*\\t.*\\t";

	/**
	 * The regex used to find the various data items.
	 * @return
	 */
	public String getRegex();
	public void setRegex(String regex);

}