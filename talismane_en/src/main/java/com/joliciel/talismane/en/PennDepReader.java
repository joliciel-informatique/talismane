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
package com.joliciel.talismane.en;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.corpus.CorpusLine;
import com.joliciel.talismane.corpus.CorpusLine.CorpusElement;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.typesafe.config.Config;

/**
 * A reader for the Penn-to-Dependency corpus, automatically converted from
 * constituent trees to dependencies as per Richard Johansson and Pierre Nugues,
 * Extended Constituent-to-dependency Conversion for English, Proceedings of
 * NODALIDA 2007, May 25-26, 2007, Tartu, Estonia,
 * <a href="http://nlp.cs.lth.se/software/treebank_converter/">http://nlp.cs.lth
 * .se/software/treebank_converter/</a> <br/>
 * Can be used in the configuration file by setting corpus-reader paths to
 * com.joliciel.talismane.en.PennDepReader
 * 
 * @author Assaf Urieli
 *
 */
public class PennDepReader extends ParserRegexBasedCorpusReader {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(PennDepReader.class);
	private static final String DEFAULT_CONLL_REGEX = "%INDEX%\\t%TOKEN%\\t.*\\t%POSTAG%\\t.*\\t.*\\t.*\\t.*\\t%GOVERNOR%\\t%LABEL%";

	private boolean keepCompoundPosTags = false;
	private Set<String> punctuationMarks = new HashSet<String>();
	private Set<String> symbolNouns = new HashSet<String>();
	private Set<String> currencyNouns = new HashSet<String>();

	public PennDepReader(Reader reader, Config config, TalismaneSession talismaneSession) throws IOException {
		super(DEFAULT_CONLL_REGEX, reader, config, talismaneSession);

		String[] puncts = new String[] { ",", ".", ";", ":", "(", ")", "''", "``", "-", "/", "!", "?", "<", ">", "&", "*", "+", "-", "=" };
		for (String punct : puncts) {
			punctuationMarks.add(punct);
		}
		String[] symbols = new String[] { "%" };
		for (String symbol : symbols) {
			symbolNouns.add(symbol);
		}
		String[] currencies = new String[] { "$", "£", "¥", "#" };
		for (String currency : currencies) {
			currencyNouns.add(currency);
		}

	}

	@Override
	protected boolean checkDataLine(CorpusLine dataLine) {
		return true;
	}

	@Override
	protected void updateDataLine(List<CorpusLine> dataLines, int index) {
		CorpusLine dataLine = dataLines.get(index);
		String posTagCode = dataLine.getElement(CorpusElement.POSTAG);
		if (punctuationMarks.contains(posTagCode)) {
			dataLine.setElement(CorpusElement.POSTAG, "P");
		} else if (currencyNouns.contains(posTagCode)) {
			dataLine.setElement(CorpusElement.POSTAG, "NNS");
			if (posTagCode.equals("#")) {
				dataLine.setElement(CorpusElement.TOKEN, "£");
			}
		} else if (symbolNouns.contains(posTagCode)) {
			dataLine.setElement(CorpusElement.POSTAG, "NN");
		}
	}

	public boolean isKeepCompoundPosTags() {
		return keepCompoundPosTags;
	}

	public void setKeepCompoundPosTags(boolean keepCompoundPosTags) {
		this.keepCompoundPosTags = keepCompoundPosTags;
	}

	@Override
	protected String readWord(String rawWord) {
		return rawWord;
	}

}
