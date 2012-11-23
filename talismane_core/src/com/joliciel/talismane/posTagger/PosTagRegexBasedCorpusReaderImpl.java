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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
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
	private static final String TOKEN_PLACEHOLDER = "%TOKEN%";
	private static final String POSTAG_PLACEHOLDER = "%POSTAG%";
	private static final String FILENAME_PLACEHOLDER = "%FILENAME%";
	private static final String ROW_PLACEHOLDER = "%ROW%";
	private static final String COLUMN_PLACEHOLDER = "%COLUMN%";
	private Pattern pattern;
	private Scanner scanner;
	private PosTagSequence sentence = null;
	private List<TokenSequenceFilter> tokenFilters = new ArrayList<TokenSequenceFilter>();

	private int lineNumber = 0;
	private Map<String, Integer> placeholderIndexMap = new HashMap<String, Integer>();
	
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
					
					if (matcher.groupCount()!=placeholderIndexMap.size()) {
						throw new TalismaneException("Expected " + placeholderIndexMap.size() + " matches (but found " + matcher.groupCount() + ") on line " + lineNumber);
					}
					
					String word =  matcher.group(placeholderIndexMap.get(TOKEN_PLACEHOLDER));
					String posTagCode = matcher.group(placeholderIndexMap.get(POSTAG_PLACEHOLDER));
					Token token = this.addToken(tokenSequence, word);
					if (placeholderIndexMap.containsKey(FILENAME_PLACEHOLDER)) 
						token.setFileName(matcher.group(placeholderIndexMap.get(FILENAME_PLACEHOLDER)));
					if (placeholderIndexMap.containsKey(ROW_PLACEHOLDER)) 
						token.setLineNumber(Integer.parseInt(matcher.group(placeholderIndexMap.get(ROW_PLACEHOLDER))));
					if (placeholderIndexMap.containsKey(COLUMN_PLACEHOLDER)) 
						token.setColumnNumber(Integer.parseInt(matcher.group(placeholderIndexMap.get(COLUMN_PLACEHOLDER))));
					
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

	Token addToken(PretokenisedSequence pretokenisedSequence, String tokenText) {
		Token token = null;
		if (tokenText.equals("_")) {
			token = pretokenisedSequence.addToken("");
		} else {
			token = pretokenisedSequence.addToken(tokenText.replace("_", " "));
		}
		return token;
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
			int tokenPos = regex.indexOf(TOKEN_PLACEHOLDER);
			if (tokenPos<0)
				throw new TalismaneException("The regex must contain the string \"" + TOKEN_PLACEHOLDER + "\"");
			
			int posTagPos = regex.indexOf(POSTAG_PLACEHOLDER);
			if (posTagPos<0)
				throw new TalismaneException("The regex must contain the string \"" + POSTAG_PLACEHOLDER + "\"");
				
			int filenamePos = regex.indexOf(FILENAME_PLACEHOLDER);
			int rowNumberPos = regex.indexOf(ROW_PLACEHOLDER);
			int columnNumberPos = regex.indexOf(COLUMN_PLACEHOLDER);
			Map<Integer, String> placeholderMap = new TreeMap<Integer, String>();
			placeholderMap.put(tokenPos, TOKEN_PLACEHOLDER);
			placeholderMap.put(posTagPos, POSTAG_PLACEHOLDER);

			if (filenamePos>=0)
				placeholderMap.put(filenamePos, FILENAME_PLACEHOLDER);
			if (rowNumberPos>=0)
				placeholderMap.put(rowNumberPos, ROW_PLACEHOLDER);
			if (columnNumberPos>=0)
				placeholderMap.put(columnNumberPos, COLUMN_PLACEHOLDER);
			
			int i = 1;
			for (String placeholderName : placeholderMap.values()) {
				placeholderIndexMap.put(placeholderName, i++);
			}
			
			String regexWithGroups = regex.replace(TOKEN_PLACEHOLDER, "(.*)");
			regexWithGroups = regexWithGroups.replace(POSTAG_PLACEHOLDER, "(.+)");
			regexWithGroups = regexWithGroups.replace(FILENAME_PLACEHOLDER, "(.+)");
			regexWithGroups = regexWithGroups.replace(ROW_PLACEHOLDER, "(.+)");
			regexWithGroups = regexWithGroups.replace(COLUMN_PLACEHOLDER, "(.+)");
			
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
