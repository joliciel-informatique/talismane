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

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.tokeniser.PretokenisedSequence;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;

class PosTagRegexBasedCorpusReaderImpl implements
		PosTagRegexBasedCorpusReader {
	private String regex = PosTagRegexBasedCorpusReader.DEFAULT_REGEX;
	private Pattern pattern;
	private Scanner scanner;
	private PosTagSequence sentence = null;
	private List<TokenSequenceFilter> tokenFilters = new ArrayList<TokenSequenceFilter>();
	private int tokenGroupIndex = 0;
	private int posTagGroupIndex = 0;
	private int lineNumber = 0;
	
	private PosTaggerServiceInternal posTaggerServiceInternal;
	private TokeniserService tokeniserService;
	
	public PosTagRegexBasedCorpusReaderImpl(Reader reader) {
		this.scanner = new Scanner(reader);
	}
	
	@Override
	public boolean hasNextPosTagSequence() {
		while (sentence==null) {
			PretokenisedSequence tokenSequence = tokeniserService.getEmptyPretokenisedSequence();
			List<PosTag> posTags = new ArrayList<PosTag>();
			boolean hasLine = false;
			if (!scanner.hasNextLine())
				break;
			while (scanner.hasNextLine()&&sentence==null) {
				String line = scanner.nextLine().replace("\r", "");
				lineNumber++;
				if (line.length()==0) {
					if (!hasLine)
						continue;
					
					tokenSequence.cleanSlate();
					for (TokenSequenceFilter tokenFilter : this.tokenFilters) {
						tokenFilter.apply(tokenSequence);
					}
					
					sentence = posTaggerServiceInternal.getPosTagSequence(tokenSequence, tokenSequence.size());
					int i = 0;
					PosTagSet posTagSet = TalismaneSession.getPosTagSet();
    				for (PosTag posTag : posTags) {
    					Token token = tokenSequence.get(i++);
    					if (tokenSequence.getTokensAdded().contains(token)) {
    						Decision<PosTag> nullDecision = posTagSet.createDefaultDecision(PosTag.NULL_POS_TAG);
    						PosTaggedToken emptyToken = posTaggerServiceInternal.getPosTaggedToken(token, nullDecision);
    						sentence.addPosTaggedToken(emptyToken);
    						token = tokenSequence.get(i++);
    					}
    					Decision<PosTag> corpusDecision = posTagSet.createDefaultDecision(posTag);
    					PosTaggedToken posTaggedToken = posTaggerServiceInternal.getPosTaggedToken(token, corpusDecision);
    					sentence.addPosTaggedToken(posTaggedToken);
    				}
				} else {
					hasLine = true;
					Matcher matcher = this.getPattern().matcher(line);
					if (!matcher.matches())
						throw new TalismaneException("Didn't match pattern on line " + lineNumber);
					
					if (matcher.groupCount()!=2) {
						throw new TalismaneException("Expected 2 matches (but found " + matcher.groupCount() + ") on line " + lineNumber);
					}
					
					String token = matcher.group(tokenGroupIndex);
					String posTagCode = matcher.group(posTagGroupIndex);
					this.addToken(tokenSequence, token);
					
    				PosTagSet posTagSet = TalismaneSession.getPosTagSet();
    				PosTag posTag = null;
    				try {
    					posTag = posTagSet.getPosTag(posTagCode);
    				} catch (UnknownPosTagException upte) {
    					throw new TalismaneException("Unknown posTag on line " + lineNumber + ": " + posTagCode);
    				}
    				posTags.add(posTag);
				}
			}
		}
		return (sentence!=null);
	}

	void addToken(PretokenisedSequence pretokenisedSequence, String token) {
		if (token.equals("_")) {
			pretokenisedSequence.addToken("");
		} else {
			if (pretokenisedSequence.size()==0) {
				// do nothing
			} else if (pretokenisedSequence.get(pretokenisedSequence.size()-1).getText().endsWith("'")) {
				// do nothing
			} else if (token.equals(".")||token.equals(",")||token.equals(")")||token.equals("]")) {
				// do nothing
			} else {
				// add a space
				pretokenisedSequence.addToken(" ");
			}
			pretokenisedSequence.addToken(token.replace("_", " "));
		}
	}
	
	@Override
	public PosTagSequence nextPosTagSequence() {
		PosTagSequence nextSentence = sentence;
		sentence = null;
		return nextSentence;
	}

	@Override
	public void addTokenSequenceFilter(TokenSequenceFilter tokenFilter) {
		this.tokenFilters.add(tokenFilter);
	}
	
	@Override
	public String getRegex() {
		return regex;
	}

	public Pattern getPattern() {
		if (this.pattern == null) {
			int tokenPos = regex.indexOf("TOKEN");
			int posTagPos = regex.indexOf("POSTAG");
			if (tokenPos<0)
				throw new TalismaneException("The regex must contain the string \"TOKEN\"");
			if (posTagPos<0)
				throw new TalismaneException("The regex must contain the string \"POSTAG\"");
			
			if (tokenPos<posTagPos) {
				tokenGroupIndex = 1;
				posTagGroupIndex = 2;
			} else {
				tokenGroupIndex = 2;
				posTagGroupIndex = 1;
			}
			
			String regexWithGroups = regex.replace("TOKEN", "(.*)");
			regexWithGroups = regexWithGroups.replace("POSTAG", "(.+)");
			this.pattern = Pattern.compile(regexWithGroups);
		}
		return pattern;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public PosTaggerServiceInternal getPosTaggerServiceInternal() {
		return posTaggerServiceInternal;
	}

	public void setPosTaggerServiceInternal(
			PosTaggerServiceInternal posTaggerServiceInternal) {
		this.posTaggerServiceInternal = posTaggerServiceInternal;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

}
