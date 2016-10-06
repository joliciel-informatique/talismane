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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.ClassificationEvent;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.tokeniser.TaggedToken;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokenisedAtomicTokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.features.TokeniserContext;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.filters.TokenFilterWrapper;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;

/**
 * An event stream for tokenising, using patterns to identify intervals that
 * need to be examined. An interval is simply the space between two tokens. This
 * reduces the tokeniser decision to binary decision: separate or join. Unlike
 * the Compound event stream, we create one event per token interval inside a
 * pattern match. By convention, a feature being tested on a token is assumed to
 * test the interval between the token and the one preceding it.
 * 
 * @author Assaf Urieli
 *
 */
public class IntervalPatternEventStream implements ClassificationEventStream {
	private static final Logger LOG = LoggerFactory.getLogger(IntervalPatternEventStream.class);

	private final TokeniserAnnotatedCorpusReader corpusReader;
	private final Set<TokeniserContextFeature<?>> tokeniserContextFeatures;

	private final TokeniserPatternManager tokeniserPatternManager;
	private final TokenSequenceFilter tokenFilterWrapper;

	private final TalismaneSession talismaneSession;

	private List<TaggedToken<TokeniserOutcome>> tokensToCheck;
	private int currentIndex;
	private TokenisedAtomicTokenSequence currentHistory = null;

	public IntervalPatternEventStream(TokeniserAnnotatedCorpusReader corpusReader, Set<TokeniserContextFeature<?>> tokeniserContextFeatures,
			TokeniserPatternManager tokeniserPatternManager, TalismaneSession talismaneSession) {
		this.corpusReader = corpusReader;
		this.tokeniserContextFeatures = tokeniserContextFeatures;
		this.tokeniserPatternManager = tokeniserPatternManager;
		this.talismaneSession = talismaneSession;
		this.tokenFilterWrapper = new TokenFilterWrapper(this.corpusReader.getTokenFilters());
	}

	@Override
	public boolean hasNext() {
		if (tokensToCheck != null) {
			if (currentIndex == tokensToCheck.size()) {
				tokensToCheck = null;
			}
		}
		while (tokensToCheck == null) {
			if (this.corpusReader.hasNextTokenSequence()) {
				TokenSequence realSequence = corpusReader.nextTokenSequence();

				List<Integer> tokenSplits = realSequence.getTokenSplits();
				String text = realSequence.getText();
				LOG.debug("Sentence: " + text);
				Sentence sentence = new Sentence(text, talismaneSession);

				TokenSequence tokenSequence = new TokenSequence(sentence, Tokeniser.SEPARATORS, talismaneSession);
				for (TokenSequenceFilter tokenSequenceFilter : this.corpusReader.getTokenSequenceFilters()) {
					tokenSequenceFilter.apply(tokenSequence);
				}
				tokenFilterWrapper.apply(tokenSequence);

				List<TaggedToken<TokeniserOutcome>> currentSentence = this.getTaggedTokens(tokenSequence, tokenSplits);
				currentHistory = new TokenisedAtomicTokenSequence(sentence, tokenSequence.size(), this.talismaneSession);

				// check if anything matches each pattern
				Set<Token> patternMatchingTokens = new TreeSet<Token>();
				for (TokenPattern parsedPattern : this.tokeniserPatternManager.getParsedTestPatterns()) {
					List<TokenPatternMatchSequence> tokenPatternMatches = parsedPattern.match(tokenSequence);
					for (TokenPatternMatchSequence tokenPatternMatch : tokenPatternMatches) {
						if (LOG.isTraceEnabled())
							LOG.trace("Matched pattern: " + parsedPattern + ": " + tokenPatternMatch.getTokenSequence());
						patternMatchingTokens.addAll(tokenPatternMatch.getTokensToCheck());
					}
				} // next pattern

				if (patternMatchingTokens.size() > 0) {
					tokensToCheck = new ArrayList<TaggedToken<TokeniserOutcome>>();
					for (Token token : patternMatchingTokens) {
						for (TaggedToken<TokeniserOutcome> taggedToken : currentSentence) {
							if (taggedToken.getToken().equals(token))
								tokensToCheck.add(taggedToken);
						}
					}

					currentIndex = 0;
					if (tokensToCheck.size() == 0) {
						tokensToCheck = null;
					}
				} else {
					tokensToCheck = null;
				}
			} else {
				break;
			}
		}

		return tokensToCheck != null;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = new LinkedHashMap<String, String>();
		attributes.put("eventStream", this.getClass().getSimpleName());
		attributes.put("corpusReader", corpusReader.getClass().getSimpleName());

		attributes.putAll(corpusReader.getCharacteristics());

		return attributes;
	}

	@Override
	public ClassificationEvent next() {
		ClassificationEvent event = null;
		if (this.hasNext()) {
			TaggedToken<TokeniserOutcome> taggedToken = tokensToCheck.get(currentIndex++);
			TokeniserContext context = new TokeniserContext(taggedToken.getToken(), currentHistory);

			LOG.debug("next event, token: " + taggedToken.getToken().getText());
			List<FeatureResult<?>> tokenFeatureResults = new ArrayList<FeatureResult<?>>();
			for (TokeniserContextFeature<?> tokeniserContextFeature : tokeniserContextFeatures) {
				RuntimeEnvironment env = new RuntimeEnvironment();
				FeatureResult<?> featureResult = tokeniserContextFeature.check(context, env);
				if (featureResult != null) {
					tokenFeatureResults.add(featureResult);
					if (LOG.isTraceEnabled()) {
						LOG.trace(featureResult.toString());
					}
				}
			}

			String classification = taggedToken.getTag().name();
			event = new ClassificationEvent(tokenFeatureResults, classification);

			currentHistory.add(taggedToken);
			if (currentIndex == tokensToCheck.size()) {
				tokensToCheck = null;
			}
		}
		return event;
	}

	public List<TaggedToken<TokeniserOutcome>> getTaggedTokens(TokenSequence tokenSequence, List<Integer> tokenSplits) {
		List<TaggedToken<TokeniserOutcome>> taggedTokens = new ArrayList<TaggedToken<TokeniserOutcome>>();
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			TokeniserOutcome outcome = TokeniserOutcome.JOIN;
			if (tokenSplits.contains(token.getStartIndex()))
				outcome = TokeniserOutcome.SEPARATE;
			Decision decision = new Decision(outcome.name());
			TaggedToken<TokeniserOutcome> taggedToken = new TaggedToken<>(token, decision, TokeniserOutcome.valueOf(decision.getOutcome()));

			taggedTokens.add(taggedToken);
		}
		return taggedTokens;
	}
}
