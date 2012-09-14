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
package com.joliciel.talismane.tokeniser;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.ArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.tokeniser.features.TokenFeatureService;
import com.joliciel.talismane.tokeniser.features.TokeniserContext;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;
import com.joliciel.talismane.tokeniser.patterns.TokenPatternMatch;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternManager;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternService;
import com.joliciel.talismane.utils.CorpusEvent;
import com.joliciel.talismane.utils.CorpusEventStream;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.util.PerformanceMonitor;

/**
 * A MaxEnt event stream for tokenising, using patterns to identify intervals that need to be examined.
 * An interval is simply the space between two tokens.
 * This reduces the tokeniser decision to binary decision: separate or join.
 * By convention, a feature being tested on a token is assumed to test the interval between the token and the one preceding it.
 * @author Assaf Urieli
 *
 */
class TokeniserEventStream implements CorpusEventStream {
    private static final Log LOG = LogFactory.getLog(TokeniserEventStream.class);
	TokenFeatureService tokenFeatureService;
	TokeniserService tokeniserService;
	TokeniserPatternService tokeniserPatternService;

    TokeniserAnnotatedCorpusReader corpusReader;
    Set<TokeniserContextFeature<?>> tokeniserContextFeatures;
	List<TaggedToken<TokeniserDecision>> tokensToCheck;
	int currentIndex;
	TokeniserDecisionTagSequence currentHistory = null;
	private List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();

	TokeniserPatternManager tokeniserPatternManager = null;

	public TokeniserEventStream(TokeniserAnnotatedCorpusReader corpusReader,
			Set<TokeniserContextFeature<?>> tokeniserContextFeatures) {
		this.corpusReader = corpusReader;
		this.tokeniserContextFeatures = tokeniserContextFeatures;
	}

	@Override
	public boolean hasNext() {
		PerformanceMonitor.startTask("TokeniserEventStream.hasNext");
		try {
			if (tokensToCheck!=null) {
				if (currentIndex==tokensToCheck.size()) {
					tokensToCheck = null;
				}
			}
			while (tokensToCheck==null) {
				if (this.corpusReader.hasNextSentence()) {
					List<Integer> tokenSplits = new ArrayList<Integer>();
					String sentence = this.corpusReader.nextSentence(tokenSplits);
					LOG.debug("Sentence: " + sentence);
					TokenSequence tokenSequence = this.tokeniserService.getTokenSequence(sentence, Tokeniser.SEPARATORS);
					for (TokenFilter tokenFilter : this.tokenFilters) {
						tokenFilter.apply(tokenSequence);
					}
					
					List<TaggedToken<TokeniserDecision>> currentSentence = this.getTaggedTokens(tokenSequence, tokenSplits);
					currentHistory = this.tokeniserService.getTokeniserDecisionTagSequence(sentence, tokenSequence.size());
					
					// check if anything matches each pattern
					Set<Token> patternMatchingTokens = new TreeSet<Token>();
					for (TokenPattern parsedPattern : this.getTokeniserPatternManager().getParsedTestPatterns()) {
						List<TokenPatternMatch> tokenPatternMatches = parsedPattern.match(tokenSequence);
						for (TokenPatternMatch tokenPatternMatch : tokenPatternMatches) {
							if (LOG.isTraceEnabled())
								LOG.trace("Matched pattern: " + parsedPattern + ": " + tokenPatternMatch.getTokenSequence());
							patternMatchingTokens.addAll(tokenPatternMatch.getTokensToCheck());
						}
					} // next pattern
					
					if (patternMatchingTokens.size()>0) {
						tokensToCheck = new ArrayList<TaggedToken<TokeniserDecision>>();
						for (Token token : patternMatchingTokens) {
							for (TaggedToken<TokeniserDecision> taggedToken : currentSentence) {
								if (taggedToken.getToken().equals(token))
									tokensToCheck.add(taggedToken);
							}
						}
						
						currentIndex = 0;
						if (tokensToCheck.size()==0) {
							tokensToCheck = null;
						}
					} else {
						tokensToCheck = null;
					}
				} else {
					break;
				}
			}
			
			return tokensToCheck!=null;
		} finally {
			PerformanceMonitor.endTask("TokeniserEventStream.hasNext");
		}
	}

	@Override
	public Map<String, Object> getAttributes() {
		Map<String,Object> attributes = new TreeMap<String, Object>();
		attributes.put("eventStream", this.getClass().getSimpleName());		
		attributes.put("corpusReader", corpusReader.getClass().getSimpleName());
				
		List<String> tokenFilterNames = new ArrayList<String>();
		for (TokenFilter tokenFilter : tokenFilters) {
			tokenFilterNames.add(tokenFilter.getClass().getSimpleName());
		}
		attributes.put("Token filters", tokenFilterNames);
		attributes.putAll(corpusReader.getCharacteristics());
		
		return attributes;
	}

	@Override
	public CorpusEvent next() {
		PerformanceMonitor.startTask("TokeniserEventStream.next");
		try {
			CorpusEvent event = null;
			if (this.hasNext()) {
				TaggedToken<TokeniserDecision> taggedToken = tokensToCheck.get(currentIndex++);
				TokeniserContext context = new TokeniserContext(taggedToken.getToken(), currentHistory);
				
				LOG.debug("next event, token: " + taggedToken.getToken().getText());
				List<FeatureResult<?>> tokenFeatureResults = new ArrayList<FeatureResult<?>>();
				PerformanceMonitor.startTask("TokeniserEventStream.next - check features");
				try {
					for (TokeniserContextFeature<?> tokeniserContextFeature : tokeniserContextFeatures) {
						FeatureResult<?> featureResult = tokeniserContextFeature.check(context);
						if (featureResult!=null) {
							tokenFeatureResults.add(featureResult);
							if (LOG.isTraceEnabled()) {
								LOG.trace(featureResult.toString());
							}
						}
					}
				} finally {
					PerformanceMonitor.endTask("TokeniserEventStream.next - check features");
				}
				
				String classification = taggedToken.getTag().name();
				event = new CorpusEvent(tokenFeatureResults, classification);
				
				currentHistory.add(taggedToken);
				if (currentIndex==tokensToCheck.size()) {
					tokensToCheck = null;
				}
			}
			return event;
		} finally {
			PerformanceMonitor.endTask("TokeniserEventStream.next");
		}
	}

	public TokenFeatureService getTokenFeatureService() {
		return tokenFeatureService;
	}

	public void setTokenFeatureService(TokenFeatureService tokenFeatureService) {
		this.tokenFeatureService = tokenFeatureService;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public List<TaggedToken<TokeniserDecision>> getTaggedTokens(TokenSequence tokenSequence, List<Integer> tokenSplits) {
		List<TaggedToken<TokeniserDecision>> taggedTokens = new ArrayList<TaggedToken<TokeniserDecision>>();
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			TokeniserDecision decision = TokeniserDecision.DOES_NOT_SEPARATE;
			if (tokenSplits.contains(token.getStartIndex()))
				decision = TokeniserDecision.DOES_SEPARATE;
			TaggedToken<TokeniserDecision> taggedToken = this.getTokeniserService().getTaggedToken(token, decision, 1.0);
			taggedTokens.add(taggedToken);
		}
		return taggedTokens;
	}
	
	public TokeniserPatternManager getTokeniserPatternManager() {
		return tokeniserPatternManager;
	}

	public void setTokeniserPatternManager(
			TokeniserPatternManager tokeniserPatternManager) {
		this.tokeniserPatternManager = tokeniserPatternManager;
	}

	public TokeniserPatternService getTokeniserPatternService() {
		return tokeniserPatternService;
	}

	public void setTokeniserPatternService(
			TokeniserPatternService tokeniserPatternService) {
		this.tokeniserPatternService = tokeniserPatternService;
	}

	public List<TokenFilter> getTokenFilters() {
		return tokenFilters;
	}

	public void setTokenFilters(List<TokenFilter> tokenFilters) {
		this.tokenFilters = tokenFilters;
	}
}
