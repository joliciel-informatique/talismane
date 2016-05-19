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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.parser.ParserRegexBasedCorpusReaderImpl;

/**
 * A reader for the Penn-to-Dependency corpus, automatically converted from constituent trees to dependencies as
 * per Richard Johansson and Pierre Nugues,
 * Extended Constituent-to-dependency Conversion for English,
 * Proceedings of NODALIDA 2007,
 * May 25-26, 2007, Tartu, Estonia,
 * <a href="http://nlp.cs.lth.se/software/treebank_converter/">http://nlp.cs.lth.se/software/treebank_converter/</a>
 * 
 * @author Assaf Urieli
 *
 */
public class PennDepReader extends ParserRegexBasedCorpusReaderImpl {
    @SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(PennDepReader.class);
	private static final String DEFAULT_CONLL_REGEX = "%INDEX%\\t%TOKEN%\\t.*\\t%POSTAG%\\t.*\\t.*\\t.*\\t.*\\t%GOVERNOR%\\t%LABEL%";
    
    private boolean keepCompoundPosTags = false;
    private Set<String> punctuationMarks = new HashSet<String>();
    private Set<String> symbolNouns = new HashSet<String>();
    private Set<String> currencyNouns = new HashSet<String>();
    
	public PennDepReader(File conllFile, Charset charset) throws IOException {
		super(conllFile, charset);
		this.initialize();
	}

	public PennDepReader(File conllFile, String encoding) throws IOException {
		this(conllFile, Charset.forName(encoding));
	}
	
	public PennDepReader(Reader reader) throws IOException {
		super(reader);
		this.initialize();
	}

	private void initialize() {
		this.setRegex(DEFAULT_CONLL_REGEX);
		String[] puncts = new String[] {",", ".", ";", ":", "(", ")", "''", "``", "-", "/", "!", "?", "<", ">", "&", "*", "+", "-", "="};
		for (String punct : puncts) {
			punctuationMarks.add(punct);
		}
		String[] symbols = new String[] {"%"};
		for (String symbol : symbols) {
			symbolNouns.add(symbol);
		}
		String[] currencies = new String[] {"$", "£", "¥", "#"};
		for (String currency : currencies) {
			currencyNouns.add(currency);
		}
		
	}


	@Override
	protected boolean checkDataLine(ParseDataLine dataLine) {
		return true;
	}

	@Override
	protected void updateDataLine(List<ParseDataLine> dataLines, int index) {
		ParseDataLine dataLine = dataLines.get(index);
		if (punctuationMarks.contains(dataLine.getPosTagCode())) {
			dataLine.setPosTagCode("P");
		} else if (currencyNouns.contains(dataLine.getPosTagCode())) {
			dataLine.setPosTagCode("NNS");
			if (dataLine.getPosTagCode().equals("#")) {
				dataLine.setWord("£");
				dataLine.getToken().setText("£");
			}
		} else if (symbolNouns.contains(dataLine.getPosTagCode())) {
			dataLine.setPosTagCode("NN");
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
