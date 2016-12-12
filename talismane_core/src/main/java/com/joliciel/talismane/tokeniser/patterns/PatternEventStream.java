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
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.features.TokenPatternMatchFeature;

/**
 * An event stream for tokenising, using patterns to identify potential
 * compounds that need to be examined. This reduces the tokeniser decision to
 * binary decision: separate or join. Unlike the Interval stream, we generate
 * one event per pattern match. The advantage is that inconsistent compounds
 * become virtually impossible, even lower down on the beam.
 * 
 * @author Assaf Urieli
 */
public class PatternEventStream implements ClassificationEventStream {
	private static final Logger LOG = LoggerFactory.getLogger(PatternEventStream.class);
	private final TokeniserAnnotatedCorpusReader corpusReader;
	private final Set<TokenPatternMatchFeature<?>> tokenPatternMatchFeatures;

	private final TokeniserPatternManager tokeniserPatternManager;

	private final TalismaneSession session;

	private List<TokeniserOutcome> currentOutcomes;
	private List<TokenPatternMatch> currentPatternMatches;
	private int currentIndex;

	public PatternEventStream(TokeniserAnnotatedCorpusReader corpusReader, Set<TokenPatternMatchFeature<?>> tokenPatternMatchFeatures,
			TokeniserPatternManager tokeniserPatternManager, TalismaneSession session) {
		this.corpusReader = corpusReader;
		this.tokenPatternMatchFeatures = tokenPatternMatchFeatures;
		this.tokeniserPatternManager = tokeniserPatternManager;
		this.session = session;
	}

	@Override
	public boolean hasNext() {
		if (currentPatternMatches != null) {
			if (currentIndex == currentPatternMatches.size()) {
				currentPatternMatches = null;
			}
		}
		while (currentPatternMatches == null) {
			if (this.corpusReader.hasNextTokenSequence()) {
				currentPatternMatches = new ArrayList<TokenPatternMatch>();
				currentOutcomes = new ArrayList<TokeniserOutcome>();
				currentIndex = 0;

				TokenSequence realSequence = corpusReader.nextTokenSequence();

				List<Integer> tokenSplits = realSequence.getTokenSplits();
				String text = realSequence.getSentence().getText().toString();
				LOG.debug("Sentence: " + text);
				Sentence sentence = new Sentence(text, session);

				TokenSequence tokenSequence = new TokenSequence(sentence, session);
				tokenSequence.findDefaultTokens();

				List<TokeniserOutcome> defaultOutcomes = this.tokeniserPatternManager.getDefaultOutcomes(tokenSequence);

				List<TaggedToken<TokeniserOutcome>> currentSentence = this.getTaggedTokens(tokenSequence, tokenSplits);

				// check if anything matches each pattern
				for (TokenPattern parsedPattern : this.tokeniserPatternManager.getParsedTestPatterns()) {
					List<TokenPatternMatchSequence> tokenPatternMatches = parsedPattern.match(tokenSequence);
					for (TokenPatternMatchSequence tokenPatternMatchSequence : tokenPatternMatches) {
						if (LOG.isTraceEnabled())
							LOG.trace("Matched pattern: " + parsedPattern + ": " + tokenPatternMatchSequence.getTokenSequence());

						// check if entire pattern is separated or joined
						TokeniserOutcome outcome = null;
						TokeniserOutcome defaultOutcome = null;
						boolean haveMismatch = false;
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
							if (outcome == null) {
								outcome = taggedToken.getTag();
								defaultOutcome = defaultOutcomes.get(token.getIndexWithWhiteSpace());
							} else if (taggedToken.getTag() != outcome) {
								// this should only happen when two patterns
								// overlap:
								// e.g. "aussi bien que" and "bien que", or
								// "plutot que" and "plutot que de"
								// AND the outer pattern is separated, while
								// the inner pattern is joined
								LOG.debug("Mismatch in pattern: " + tokenPatternMatch + ", " + taggedToken);
								haveMismatch = true;
							}
						}
						currentPatternMatches.add(tokenPatternMatch);

						if (haveMismatch) {
							currentOutcomes.add(defaultOutcome);
						} else {
							currentOutcomes.add(outcome);
						}

					}
				} // next pattern

				if (currentPatternMatches.size() == 0) {
					currentPatternMatches = null;
					currentOutcomes = null;
				}
			} else {
				break;
			}
		}

		return currentPatternMatches != null;
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
			TokenPatternMatch tokenPatternMatch = currentPatternMatches.get(currentIndex);
			TokeniserOutcome outcome = currentOutcomes.get(currentIndex);
			String classification = outcome.name();

			LOG.debug("next event, pattern match: " + tokenPatternMatch.toString() + ", outcome:" + classification);
			List<FeatureResult<?>> tokenFeatureResults = new ArrayList<FeatureResult<?>>();
			for (TokenPatternMatchFeature<?> feature : tokenPatternMatchFeatures) {
				RuntimeEnvironment env = new RuntimeEnvironment();
				FeatureResult<?> featureResult = feature.check(tokenPatternMatch, env);
				if (featureResult != null) {
					tokenFeatureResults.add(featureResult);
					if (LOG.isTraceEnabled()) {
						LOG.trace(featureResult.toString());
					}
				}
			}

			event = new ClassificationEvent(tokenFeatureResults, classification);

			currentIndex++;
			if (currentIndex == currentPatternMatches.size()) {
				currentPatternMatches = null;
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
