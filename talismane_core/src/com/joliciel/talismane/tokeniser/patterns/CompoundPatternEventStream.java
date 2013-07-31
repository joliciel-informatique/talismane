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
package com.joliciel.talismane.tokeniser.patterns;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.CorpusEvent;
import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.tokeniser.TaggedToken;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserDecisionFactory;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.features.TokenFeatureService;
import com.joliciel.talismane.tokeniser.features.TokenPatternMatchFeature;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * An event stream for tokenising, using patterns to identify potential compounds that need to be examined.
 * This reduces the tokeniser decision to binary decision: separate or join.
 * Unlike the Interval stream, we generate one event per pattern match.
 * The advantage is that inconsistent compounds become virtually impossible, even lower down on the beam.
 * @author Assaf Urieli
 */
class CompoundPatternEventStream implements CorpusEventStream {
    private static final Log LOG = LogFactory.getLog(CompoundPatternEventStream.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(CompoundPatternEventStream.class);
    
    private TokenFeatureService tokenFeatureService;
	private TokenFilterService tokenFilterService;
	private TokeniserService tokeniserService;
	private TokeniserPatternService tokeniserPatternService;
	private FilterService filterService;
	private FeatureService featureService;
	
	private MachineLearningService machineLearningService;

	private TokeniserAnnotatedCorpusReader corpusReader;
    private Set<TokenPatternMatchFeature<?>> tokenPatternMatchFeatures;
    private List<TokeniserOutcome> currentOutcomes;
    private List<TokenPatternMatch> currentPatternMatches;
	private int currentIndex;

	private TokeniserPatternManager tokeniserPatternManager = null;
	private TokeniserDecisionFactory tokeniserDecisionFactory = new TokeniserDecisionFactory();
	private TokenSequenceFilter tokenFilterWrapper = null;

	public CompoundPatternEventStream(TokeniserAnnotatedCorpusReader corpusReader,
			Set<TokenPatternMatchFeature<?>> tokenPatternMatchFeatures) {
		this.corpusReader = corpusReader;
		this.tokenPatternMatchFeatures = tokenPatternMatchFeatures;
	}

	@Override
	public boolean hasNext() {
		MONITOR.startTask("hasNext");
		try {
			if (currentPatternMatches!=null) {
				if (currentIndex==currentPatternMatches.size()) {
					currentPatternMatches = null;
				}
			}
			while (currentPatternMatches==null) {
				if (this.corpusReader.hasNextTokenSequence()) {
					currentPatternMatches = new ArrayList<TokenPatternMatch>();
					currentOutcomes = new ArrayList<TokeniserOutcome>();
					currentIndex = 0;
					
					TokenSequence realSequence = corpusReader.nextTokenSequence();
					
					List<Integer> tokenSplits = realSequence.getTokenSplits();
					String text = realSequence.getText();
					LOG.debug("Sentence: " + text);
					Sentence sentence = filterService.getSentence(text);
					
					TokenSequence tokenSequence = this.tokeniserService.getTokenSequence(sentence, Tokeniser.SEPARATORS);
					for (TokenSequenceFilter tokenSequenceFilter : this.corpusReader.getTokenSequenceFilters()) {
						tokenSequenceFilter.apply(tokenSequence);
					}
					if (tokenFilterWrapper==null) {
						tokenFilterWrapper = tokenFilterService.getTokenSequenceFilter(this.corpusReader.getTokenFilters());
					}
					tokenFilterWrapper.apply(tokenSequence);
					
					List<TaggedToken<TokeniserOutcome>> currentSentence = this.getTaggedTokens(tokenSequence, tokenSplits);
					
					// check if anything matches each pattern
					for (TokenPattern parsedPattern : this.getTokeniserPatternManager().getParsedTestPatterns()) {
						List<TokenPatternMatchSequence> tokenPatternMatches = parsedPattern.match(tokenSequence);
						for (TokenPatternMatchSequence tokenPatternMatchSequence : tokenPatternMatches) {
							if (LOG.isTraceEnabled())
								LOG.trace("Matched pattern: " + parsedPattern + ": " + tokenPatternMatchSequence.getTokenSequence());
							
							// check if entire pattern is separated or joined
							TokeniserOutcome outcome = null;
							TokenPatternMatch tokenPatternMatch = null;
							for (Token token : tokenPatternMatchSequence.getTokensToCheck()) {
								if (tokenPatternMatch == null) {
									for (TokenPatternMatch patternMatch : tokenPatternMatchSequence.getTokenPatternMatches()) {
										if (patternMatch.getToken().equals(token)) {
											tokenPatternMatch = patternMatch;
											break;
										}
									}
								}
								TaggedToken<TokeniserOutcome> taggedToken = currentSentence.get(token.getIndexWithWhiteSpace());
								if (outcome==null) {
									outcome = taggedToken.getTag();
								} else if (outcome!=taggedToken.getTag()) {
									// not generally expecting this, except in the case of
									// two imbricated compounds, e.g. "aussi bien que", where
									// the outer one is separated and the inner one joined
									LOG.info("incosistent compound tokenisation");
								}
							}
							currentPatternMatches.add(tokenPatternMatch);
							currentOutcomes.add(outcome);
							
						}
					} // next pattern
					
					if (currentPatternMatches.size()==0) {
						currentPatternMatches = null;
						currentOutcomes = null;
					}
				} else {
					break;
				}
			}
			
			return currentPatternMatches!=null;
		} finally {
			MONITOR.endTask("hasNext");
		}
	}

	@Override
	public Map<String, Object> getAttributes() {
		Map<String,Object> attributes = new LinkedHashMap<String, Object>();
		attributes.put("eventStream", this.getClass().getSimpleName());		
		attributes.put("corpusReader", corpusReader.getClass().getSimpleName());
				
		attributes.putAll(corpusReader.getCharacteristics());
		
		return attributes;
	}

	@Override
	public CorpusEvent next() {
		MONITOR.startTask("next");
		try {
			CorpusEvent event = null;
			if (this.hasNext()) {
				TokenPatternMatch tokenPatternMatch = currentPatternMatches.get(currentIndex);
				TokeniserOutcome outcome = currentOutcomes.get(currentIndex);
				String classification = outcome.name();
				
				LOG.debug("next event, pattern match: " + tokenPatternMatch.toString() + ", outcome:" + classification);
				List<FeatureResult<?>> tokenFeatureResults = new ArrayList<FeatureResult<?>>();
				MONITOR.startTask("check features");
				try {
					for (TokenPatternMatchFeature<?> feature : tokenPatternMatchFeatures) {
						RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
						FeatureResult<?> featureResult = feature.check(tokenPatternMatch, env);
						if (featureResult!=null) {
							tokenFeatureResults.add(featureResult);
							if (LOG.isTraceEnabled()) {
								LOG.trace(featureResult.toString());
							}
						}
					}
				} finally {
					MONITOR.endTask("check features");
				}
				
				event = this.machineLearningService.getCorpusEvent(tokenFeatureResults, classification);
				
				currentIndex++;
				if (currentIndex==currentPatternMatches.size()) {
					currentPatternMatches = null;
				}
			}
			return event;
		} finally {
			MONITOR.endTask("next");
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

	public List<TaggedToken<TokeniserOutcome>> getTaggedTokens(TokenSequence tokenSequence, List<Integer> tokenSplits) {
		List<TaggedToken<TokeniserOutcome>> taggedTokens = new ArrayList<TaggedToken<TokeniserOutcome>>();
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			TokeniserOutcome outcome = TokeniserOutcome.DOES_NOT_SEPARATE;
			if (tokenSplits.contains(token.getStartIndex()))
				outcome = TokeniserOutcome.DOES_SEPARATE;
			Decision<TokeniserOutcome> decision = this.tokeniserDecisionFactory.createDefaultDecision(outcome);
			TaggedToken<TokeniserOutcome> taggedToken = this.getTokeniserService().getTaggedToken(token, decision);
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

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	public FilterService getFilterService() {
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}

	public TokenFilterService getTokenFilterService() {
		return tokenFilterService;
	}

	public void setTokenFilterService(TokenFilterService tokenFilterService) {
		this.tokenFilterService = tokenFilterService;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}
	
	
}
