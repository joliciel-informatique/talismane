///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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
package com.joliciel.talismane.standoff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalEntryReader;
import com.joliciel.talismane.machineLearning.Decision;
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
    private static final Log LOG = LogFactory.getLog(StandoffReader.class);
	private int maxSentenceCount = 0;
	private int sentenceCount = 0;
	private int lineNumber = 1;
	
	private ParserService parserService;
	private PosTaggerService posTaggerService;
	private TokeniserService tokeniserService;
	private TokenFilterService tokenFilterService;
	
	ParseConfiguration configuration = null;
	private Scanner scanner = null;
	private String currentLine = null;
	
	private TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance();
	
	private List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();
	private List<TokenSequenceFilter> tokenSequenceFilters = new ArrayList<TokenSequenceFilter>();
	private List<PosTagSequenceFilter> posTagSequenceFilters = new ArrayList<PosTagSequenceFilter>();
	private TokenSequenceFilter tokenFilterWrapper = null;

	public StandoffReader(Scanner scanner) {
		this.scanner = scanner;
		if (scanner.hasNextLine()) {
			currentLine = scanner.nextLine();
		}
	}
	
	@Override
	public boolean hasNextConfiguration() {
		if (maxSentenceCount>0 && sentenceCount>=maxSentenceCount) {
			// we've reached the end, do nothing
		} else {
			if (configuration==null) {
				boolean tokensFinished = false;
				PretokenisedSequence tokenSequence = this.getTokeniserService().getEmptyPretokenisedSequence();
				PosTagSequence posTagSequence = this.getPosTaggerService().getPosTagSequence(tokenSequence, tokenSequence.size());
				Map<String,PosTaggedToken> idTokenMap = new HashMap<String, PosTaggedToken>();
				PosTagSet posTagSet = TalismaneSession.getPosTagSet();
				while (currentLine!=null) {
					String line = currentLine;
					
					if (line.startsWith("T")) {
						if (tokensFinished) {
							sentenceCount++;
							break;
						}
						String[] parts = line.split("[\\t]");
						String id = parts[0];
						String[] posTagParts = parts[1].split(" ");
						String posTagCode = posTagParts[0].replace('_', '+');
						String text = parts[2];
						Token token = tokenSequence.addToken(text);
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
		  				
						Decision<PosTag> posTagDecision = posTagSet.createDefaultDecision(posTag);
						PosTaggedToken posTaggedToken = this.getPosTaggerService().getPosTaggedToken(token, posTagDecision);
						if (LOG.isTraceEnabled()) {
							LOG.trace(posTaggedToken.toString());
						}
						
						posTagSequence.addPosTaggedToken(posTaggedToken);
						LOG.debug("Found token " + id + ", " + posTaggedToken);
						idTokenMap.put(id, posTaggedToken);	    				
					} else if (line.startsWith("R")) {
						if (!tokensFinished) {
							tokensFinished = true;
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
						
						}
						
						String[] parts = line.split("[\\t :]");
						String label = parts[1];
						String headId = parts[3];
						String dependentId = parts[5];
						PosTaggedToken head = idTokenMap.get(headId);
						PosTaggedToken dependent = idTokenMap.get(dependentId);
						if (head==null) {
							throw new TalismaneException("No token found for head id: " + headId);
						}
						if (dependent==null) {
							throw new TalismaneException("No token found for dependent id: " + dependentId);
						}
						configuration.addDependency(head, dependent, label, null);
					}
					currentLine = null;
					if (scanner.hasNextLine()) {
						currentLine = scanner.nextLine();
						lineNumber++;
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
	public Map<String, Object> getAttributes() {
		Map<String,Object> attributes = new LinkedHashMap<String, Object>();
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
		if (this.tokenFilterService==null) {
			this.tokenFilterService = locator.getTokenFilterServiceLocator().getTokenFilterService();
		}
		return tokenFilterService;
	}


	public void setTokenFilterService(TokenFilterService tokenFilterService) {
		this.tokenFilterService = tokenFilterService;
	}

	public PosTaggerService getPosTaggerService() {
		if (posTaggerService==null)
			posTaggerService = locator.getPosTaggerServiceLocator().getPosTaggerService();
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}
	
	public TokeniserService getTokeniserService() {
		if (tokeniserService==null)
			tokeniserService = locator.getTokeniserServiceLocator().getTokeniserService();
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public ParserService getParserService() {
		if (parserService==null)
			parserService = locator.getParserServiceLocator().getParserService();
		
		return parserService;
	}

	public void setParserService(ParserService parserService) {
		this.parserService = parserService;
	}
}
