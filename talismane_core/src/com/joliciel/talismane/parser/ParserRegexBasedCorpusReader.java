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
package com.joliciel.talismane.parser;

import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;

/**
 * A corpus reader that expects one pos-tagged token with dependency info per line,
 * and analyses the line content based on a regex supplied during construction.
 * The regex needs to contain the following five capturing groups, indicated by the following strings:<br/>
 * <li>%INDEX%: a unique index for a given token (typically just a sequential index)</li>
 * <li>%TOKEN%: the token - note that we assume CoNLL formatting (with underscores for spaces and for empty tokens). The sequence &amp;und; should be used for true underscores.</li>
 * <li>%POSTAG%: the token's pos-tag</li>
 * <li>%LABEL%: the dependency label governing this token</li>
 * <li>%GOVERNOR%: the index of the token governing this token - a value of 0 indicates an invisible "root" token as a governor</li>
 * It can optionally contain the following capturing groups as well:<br/>
 * <li>%FILENAME%: the file containing the token</li>
 * <li>%ROW%: the row containing the token</li>
 * <li>%COLUMN%: the column on which the token starts</li>
 * <li>%END_ROW%: the row containing the token's end</li>
 * <li>%END_COLUMN%: the column just after the token end</li>
 * The token placeholder will be replaced by (.*). Other placeholders will be replaced by (.+) meaning no empty strings allowed.
 * @author Assaf Urieli
 *
 */
public interface ParserRegexBasedCorpusReader extends ParserAnnotatedCorpusReader,
	PosTagAnnotatedCorpusReader, TokeniserAnnotatedCorpusReader, SentenceDetectorAnnotatedCorpusReader {
	/**
	 * The default regex (if none is set) - corresponds to the CONLL format with the pos-tag at column 4 rather than 5.
	 */
	public static final String DEFAULT_REGEX = "%INDEX%\\t%TOKEN%\\t.*\\t%POSTAG%\\t.*\\t.*\\t%NON_PROJ_GOVERNOR%\\t%NON_PROJ_LABEL%\\t%GOVERNOR%\\t%LABEL%";
	
	/**
	 * The regex used to find the various data items.
	 */
	public String getRegex();
	public void setRegex(String regex);
	
	/**
	 * Should an attempt be made to the predict the transitions that led to this configuration,
	 * or should dependencies simply be added with null transitions.
	 */
	public boolean isPredictTransitions();
	public void setPredictTransitions(boolean predictTransitions);
	
	/**
	 * If the reader is opened based on a directory, the name of a file to exclude when training.
	 */
	public String getExcludeFileName();
	public void setExcludeFileName(String excludeFileName);

}