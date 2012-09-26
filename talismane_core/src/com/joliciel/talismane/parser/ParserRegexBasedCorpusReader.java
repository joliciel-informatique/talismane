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

/**
 * A corpus reader that expects one pos-tagged token with dependency info per line,
 * and analyses the line content based on a regex supplied during construction.
 * The regex needs to contain exactly five capturing groups, indicated by the following strings:<br/>
 * <li>INDEX: a unique index for a given token (typically just a sequential index)</li>
 * <li>TOKEN: the token</li>
 * <li>POSTAG: the token's pos-tag</li>
 * <li>LABEL: the dependency label governing this token</li>
 * <li>GOVERNOR: the index of the token governing this token - a value of 0 indicates an invisible "root" token as a governor</li>
 * The strings will (.*) for the token, and (.+) for all others.
 * @author Assaf Urieli
 *
 */
public interface ParserRegexBasedCorpusReader extends ParserAnnotatedCorpusReader {
	/**
	 * The default regex (if none is set) - corresponds to the CONLL format.
	 */
	public static final String DEFAULT_REGEX = "INDEX\\tTOKEN\\t.*\\tPOSTAG\\t.*\\t.*\\tGOVERNOR\\tLABEL\\t_\\t_";
	
	/**
	 * The regex used to find the various data items.
	 * @return
	 */
	public String getRegex();
	public void setRegex(String regex);

}