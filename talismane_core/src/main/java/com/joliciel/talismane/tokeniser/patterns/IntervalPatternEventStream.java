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
package com.joliciel.talismane.tokeniser.patterns;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.ClassificationEvent;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.tokeniser.TaggedToken;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokenisedAtomicTokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.features.TokenFeatureService;
import com.joliciel.talismane.tokeniser.features.TokeniserContext;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * An event stream for tokenising, using patterns to identify intervals that need to be examined.
 * An interval is simply the space between two tokens.
 * This reduces the tokeniser decision to binary decision: separate or join.
 * Unlike the Compound event stream, we create one event per token interval inside a pattern match.
 * By convention, a feature being tested on a token is assumed to test the interval between the token and the one preceding it.
 * @author Assaf Urieli
 *
 */
class IntervalPatternEventStream implements ClassificationEventStream {
    private static final Log LOG = LogFactory.getLog(IntervalPatternEventStream.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(IntervalPatternEventStream.class);
    
    private TokenFeatureService tokenFeatureService;
	private TokenFilterService tokenFilterService;
	private TokeniserService tokeniserService;
	private TokeniserPatternService tokeniserPatternService;
	private FilterService filterService;
	private FeatureService featureService;
	
	private MachineLearningService machineLearningService;

	private TokeniserAnnotatedCorpusReader corpusReader;
    private Set<TokeniserContextFeature<?>> tokeniserContextFeatures;
    private List<TaggedToken<TokeniserOutcome>> tokensToCheck;
	private int currentIndex;
	private TokenisedAtomicTokenSequence currentHistory = null;

	private TokeniserPatternManager tokeniserPatternManager = null;
	private TokenSequenceFilter tokenFilterWrapper = null;

	public IntervalPatternEventStream(TokeniserAnnotatedCorpusReader corpusReader,
			Set<TokeniserContextFeature<?>> tokeniserContextFeatures) {
		this.corpusReader = corpusReader;
		this.tokeniserContextFeatures = tokeniserContextFeatures;
	}

	@Override
	public boolean hasNext() {
		MONITOR.startTask("hasNext");
		try {
			if (tokensToCheck!=null) {
				if (currentIndex==tokensToCheck.size()) {
					tokensToCheck = null;
				}
			}
			while (tokensToCheck==null) {
				if (this.corpusReader.hasNextTokenSequence()) {
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
					currentHistory = this.tokeniserService.getTokenisedAtomicTokenSequence(sentence, tokenSequence.size());
					
					// check if anything matches each pattern
					Set<Token> patternMatchingTokens = new TreeSet<Token>();
					for (TokenPattern parsedPattern : this.getTokeniserPatternManager().getParsedTestPatterns()) {
						List<TokenPatternMatchSequence> tokenPatternMatches = parsedPattern.match(tokenSequence);
						for (TokenPatternMatchSequence tokenPatternMatch : tokenPatternMatches) {
							if (LOG.isTraceEnabled())
								LOG.trace("Matched pattern: " + parsedPattern + ": " + tokenPatternMatch.getTokenSequence());
							patternMatchingTokens.addAll(tokenPatternMatch.getTokensToCheck());
						}
					} // next pattern
					
					if (patternMatchingTokens.size()>0) {
						tokensToCheck = new ArrayList<TaggedToken<TokeniserOutcome>>();
						for (Token token : patternMatchingTokens) {
							for (TaggedToken<TokeniserOutcome> taggedToken : currentSentence) {
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
			MONITOR.endTask();
		}
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String,String> attributes = new LinkedHashMap<String, String>();
		attributes.put("eventStream", this.getClass().getSimpleName());		
		attributes.put("corpusReader", corpusReader.getClass().getSimpleName());
				
		attributes.putAll(corpusReader.getCharacteristics());
		
		return attributes;
	}

	@Override
	public ClassificationEvent next() {
		MONITOR.startTask("next");
		try {
			ClassificationEvent event = null;
			if (this.hasNext()) {
				TaggedToken<TokeniserOutcome> taggedToken = tokensToCheck.get(currentIndex++);
				TokeniserContext context = new TokeniserContext(taggedToken.getToken(), currentHistory);
				
				LOG.debug("next event, token: " + taggedToken.getToken().getText());
				List<FeatureResult<?>> tokenFeatureResults = new ArrayList<FeatureResult<?>>();
				MONITOR.startTask("check features");
				try {
					for (TokeniserContextFeature<?> tokeniserContextFeature : tokeniserContextFeatures) {
						RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
						FeatureResult<?> featureResult = tokeniserContextFeature.check(context, env);
						if (featureResult!=null) {
							tokenFeatureResults.add(featureResult);
							if (LOG.isTraceEnabled()) {
								LOG.trace(featureResult.toString());
							}
						}
					}
				} finally {
					MONITOR.endTask();
				}
				
				String classification = taggedToken.getTag().name();
				event = this.machineLearningService.getClassificationEvent(tokenFeatureResults, classification);
				
				currentHistory.add(taggedToken);
				if (currentIndex==tokensToCheck.size()) {
					tokensToCheck = null;
				}
			}
			return event;
		} finally {
			MONITOR.endTask();
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
			TokeniserOutcome outcome = TokeniserOutcome.JOIN;
			if (tokenSplits.contains(token.getStartIndex()))
				outcome = TokeniserOutcome.SEPARATE;
			Decision decision = this.machineLearningService.createDefaultDecision(outcome.name());
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
