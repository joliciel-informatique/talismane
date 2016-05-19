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
package com.joliciel.talismane.extensions.standoff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalEntryReader;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParserAnnotatedCorpusReader;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.UnknownPosTagException;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.tokeniser.PretokenisedSequence;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;

public class StandoffReader implements ParserAnnotatedCorpusReader {
    private static final Logger LOG = LoggerFactory.getLogger(StandoffReader.class);
	private int maxSentenceCount = 0;
	private int startSentence = 0;
	private int sentenceCount = 0;
	private int lineNumber = 1;
	private int includeIndex = -1;
	private int excludeIndex = -1;
	private int crossValidationSize = 0;
	
	private ParserService parserService;
	private PosTaggerService posTaggerService;
	private TokeniserService tokeniserService;
	private TokenFilterService tokenFilterService;
	private MachineLearningService machineLearningService;
	
	ParseConfiguration configuration = null;
	private int sentenceIndex = 0;
	
	private List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();
	private List<TokenSequenceFilter> tokenSequenceFilters = new ArrayList<TokenSequenceFilter>();
	private List<PosTagSequenceFilter> posTagSequenceFilters = new ArrayList<PosTagSequenceFilter>();
	private TokenSequenceFilter tokenFilterWrapper = null;
	
	private Map<String, StandoffToken> tokenMap = new HashMap<String, StandoffReader.StandoffToken>();
	private Map<String, StandoffRelation> relationMap = new HashMap<String, StandoffReader.StandoffRelation>();
	private Map<String, StandoffRelation> idRelationMap = new HashMap<String, StandoffReader.StandoffRelation>();
	private Map<String, String> notes = new HashMap<String, String>();
	
	private List<List<StandoffToken>> sentences = new ArrayList<List<StandoffReader.StandoffToken>>();
	
	public StandoffReader(TalismaneSession talismaneSession, Scanner scanner) {
		PosTagSet posTagSet = talismaneSession.getPosTagSet();
		
		Map<Integer,StandoffToken> sortedTokens = new TreeMap<Integer, StandoffReader.StandoffToken>();
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.startsWith("T")) {
				
				String[] parts = line.split("[\\t]");
				String id = parts[0];
				String[] posTagParts = parts[1].split(" ");
				String posTagCode = posTagParts[0].replace('_', '+');
				int startPos = Integer.parseInt(posTagParts[1]);
				String text = parts[2];
				
  				PosTag posTag = null;
  				if (posTagCode.equalsIgnoreCase(PosTag.ROOT_POS_TAG_CODE)) {
  					posTag = PosTag.ROOT_POS_TAG;
  				} else {
    				try {
    					posTag = posTagSet.getPosTag(posTagCode);
    				} catch (UnknownPosTagException upte) {
    					throw new TalismaneException("Unknown posTag on line " + lineNumber + ": " + posTagCode);
    				}
  				}
  				
  				StandoffToken token = new StandoffToken();
  				token.posTag = posTag;
  				token.text = text;
  				token.id = id;
  				
  				sortedTokens.put(startPos, token);
  				tokenMap.put(id, token);
  				
			} else if (line.startsWith("R")) {
				
				String[] parts = line.split("[\\t :]");
				String id = parts[0];
				String label = parts[1];
				String headId = parts[3];
				String dependentId = parts[5];
				StandoffRelation relation = new StandoffRelation();
				relation.fromToken = headId;
				relation.toToken = dependentId;
				relation.label = label;
				idRelationMap.put(id, relation);
				relationMap.put(dependentId, relation);
			} else if (line.startsWith("#")) {
				String[] parts = line.split("\t");
				String itemId = parts[1].substring("AnnotatorNotes ".length());
				String note = parts[2];
				notes.put(itemId, note);
			}
		}
		
		for (String itemId : notes.keySet()) {
			String comment = notes.get(itemId);
			if (itemId.startsWith("R")) {
				StandoffRelation relation = idRelationMap.get(itemId);
				relation.comment = comment;
			} else {
				StandoffToken token = tokenMap.get(itemId);
				token.comment = comment;
			}
		}
		
		List<StandoffToken> currentSentence = null;
		for (StandoffToken token : sortedTokens.values()) {
			if (token.text.equals("ROOT")) {
				if (currentSentence!=null)
					sentences.add(currentSentence);
				currentSentence = new ArrayList<StandoffReader.StandoffToken>();
			}
			currentSentence.add(token);
		}
		if (currentSentence!=null)
			sentences.add(currentSentence);
	}
	
	@Override
	public boolean hasNextConfiguration() {
		if (maxSentenceCount>0 && sentenceCount>=maxSentenceCount) {
			// we've reached the end, do nothing
		} else {
			if (configuration==null && sentenceIndex<sentences.size()) {
				
				PretokenisedSequence tokenSequence = this.getTokeniserService().getEmptyPretokenisedSequence();
				PosTagSequence posTagSequence = this.getPosTaggerService().getPosTagSequence(tokenSequence);
				Map<String,PosTaggedToken> idTokenMap = new HashMap<String, PosTaggedToken>();
				
				List<StandoffToken> tokens = sentences.get(sentenceIndex++);
				
				for (StandoffToken standoffToken : tokens) {
					Token token = tokenSequence.addToken(standoffToken.text);
					Decision posTagDecision = machineLearningService.createDefaultDecision(standoffToken.posTag.getCode());
					PosTaggedToken posTaggedToken = this.getPosTaggerService().getPosTaggedToken(token, posTagDecision);
					if (LOG.isTraceEnabled()) {
						LOG.trace(posTaggedToken.toString());
					}
					
					posTaggedToken.setComment(standoffToken.comment);
					
					posTagSequence.addPosTaggedToken(posTaggedToken);
					idTokenMap.put(standoffToken.id, posTaggedToken);
					LOG.debug("Found token " + standoffToken.id + ", " + posTaggedToken);					
				}
				
				tokenSequence.setWithRoot(true);							

				tokenSequence.cleanSlate();
				
				// first apply the token filters - which might replace the text of an individual token
				// with something else
				if (tokenFilterWrapper==null) {
					tokenFilterWrapper = this.getTokenFilterService().getTokenSequenceFilter(this.tokenFilters);
				}
				tokenFilterWrapper.apply(tokenSequence);
				
				for (TokenSequenceFilter tokenFilter : this.tokenSequenceFilters) {
					tokenFilter.apply(tokenSequence);
				}
				
				if (tokenSequence.getTokensAdded().size()>0) {
					throw new TalismaneException("Added tokens not currently supported by StandoffReader");
				}
				
				tokenSequence.finalise();
				
				configuration = this.getParserService().getInitialConfiguration(posTagSequence);

				for (StandoffToken standoffToken : tokens) {
					StandoffRelation relation = relationMap.get(standoffToken.id);
					if (relation!=null) {
						PosTaggedToken head = idTokenMap.get(relation.fromToken);
						PosTaggedToken dependent = idTokenMap.get(relation.toToken);
						if (head==null) {
							throw new TalismaneException("No token found for head id: " + relation.fromToken);
						}
						if (dependent==null) {
							throw new TalismaneException("No token found for dependent id: " + relation.toToken);
						}
						DependencyArc arc = configuration.addDependency(head, dependent, relation.label, null);
						arc.setComment(relation.comment);
					}
				}
	
			}
		}
		return (configuration!=null);
	}


	@Override
	public ParseConfiguration nextConfiguration() {
		ParseConfiguration nextConfiguration = null;
		if (this.hasNextConfiguration()) {
			nextConfiguration = configuration;
			configuration = null;
		}
		return nextConfiguration;
	}

	public void addTokenFilter(TokenFilter tokenFilter) {
		this.tokenFilters.add(tokenFilter);
	}
	
	@Override
	public void addTokenSequenceFilter(TokenSequenceFilter tokenFilter) {
		this.tokenSequenceFilters.add(tokenFilter);
	}

	@Override
	public void addPosTagSequenceFilter(
			PosTagSequenceFilter posTagSequenceFilter) {
		this.posTagSequenceFilters.add(posTagSequenceFilter);
	}

	@Override
	public Map<String, String> getCharacteristics() {
		Map<String,String> attributes = new LinkedHashMap<String, String>();
		return attributes;
	}

	@Override
	public LexicalEntryReader getLexicalEntryReader() {
		throw new RuntimeException("Not supported");
	}

	@Override
	public void setLexicalEntryReader(LexicalEntryReader lexicalEntryReader) {
		throw new RuntimeException("Not supported");
	}

	@Override
	public int getMaxSentenceCount() {
		return maxSentenceCount;
	}

	@Override
	public void setMaxSentenceCount(int maxSentenceCount) {
		this.maxSentenceCount = maxSentenceCount;
	}


	public TokenFilterService getTokenFilterService() {
		return tokenFilterService;
	}


	public void setTokenFilterService(TokenFilterService tokenFilterService) {
		this.tokenFilterService = tokenFilterService;
	}

	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}
	
	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public ParserService getParserService() {
		return parserService;
	}

	public void setParserService(ParserService parserService) {
		this.parserService = parserService;
	}
	
	private static final class StandoffToken {
		public PosTag posTag;
		public String text;
		public String id;
		public String comment = "";
	}
	
	private static final class StandoffRelation {
		public String label;
		public String fromToken;
		public String toToken;
		public String comment = "";
	}

	public int getIncludeIndex() {
		return includeIndex;
	}

	public void setIncludeIndex(int includeIndex) {
		this.includeIndex = includeIndex;
	}

	public int getExcludeIndex() {
		return excludeIndex;
	}

	public void setExcludeIndex(int excludeIndex) {
		this.excludeIndex = excludeIndex;
	}

	public int getCrossValidationSize() {
		return crossValidationSize;
	}

	public void setCrossValidationSize(int crossValidationSize) {
		this.crossValidationSize = crossValidationSize;
	}

	@Override
	public void rewind() {
		throw new TalismaneException("rewind operation not supported by " + this.getClass().getName());
	}

	public int getStartSentence() {
		return startSentence;
	}

	public void setStartSentence(int startSentence) {
		this.startSentence = startSentence;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}
	
	
}
