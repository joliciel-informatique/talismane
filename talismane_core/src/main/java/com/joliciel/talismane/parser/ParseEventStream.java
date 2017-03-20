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
package com.joliciel.talismane.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.ClassificationEvent;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;

/**
 * A classification event stream for parse configurations.
 * 
 * @author Assaf Urieli
 *
 */
public class ParseEventStream implements ClassificationEventStream {
	private static final Logger LOG = LoggerFactory.getLogger(ParseEventStream.class);

	private final ParserAnnotatedCorpusReader corpusReader;
	private final Set<ParseConfigurationFeature<?>> parseFeatures;
	private final boolean skipImpossibleSentences;

	ParseConfiguration targetConfiguration;
	ParseConfiguration currentConfiguration;

	int currentIndex;
	int eventCount;

	public ParseEventStream(ParserAnnotatedCorpusReader corpusReader, Set<ParseConfigurationFeature<?>> parseFeatures, boolean skipImpossibleSentences) {
		this.corpusReader = corpusReader;
		this.parseFeatures = parseFeatures;
		this.skipImpossibleSentences = skipImpossibleSentences;
	}

	@Override
	public boolean hasNext() throws TalismaneException {
		while (targetConfiguration == null) {
			try {
				if (this.corpusReader.hasNextSentence()) {

					targetConfiguration = this.corpusReader.nextConfiguration();
					currentConfiguration = new ParseConfiguration(targetConfiguration.getPosTagSequence());
					currentIndex = 0;
					if (currentIndex == targetConfiguration.getTransitions().size()) {
						targetConfiguration = null;
					}
				} else {
					break;
				}
			} catch (NonPredictableParseTreeException e) {
				if (skipImpossibleSentences) {
					LOG.error("Impossible sentence, skipping", e);
					continue;
				}
				throw e;
			}
		}

		if (targetConfiguration == null) {
			LOG.debug("Event stream reading complete");
		}
		return targetConfiguration != null;
	}

	@Override
	public ClassificationEvent next() throws TalismaneException {
		ClassificationEvent event = null;
		if (this.hasNext()) {
			eventCount++;
			LOG.debug("Event " + eventCount + ": " + currentConfiguration.toString());

			List<FeatureResult<?>> parseFeatureResults = new ArrayList<FeatureResult<?>>();
			for (ParseConfigurationFeature<?> parseFeature : parseFeatures) {
				RuntimeEnvironment env = new RuntimeEnvironment();
				FeatureResult<?> featureResult = parseFeature.check(currentConfiguration, env);
				if (featureResult != null) {
					parseFeatureResults.add(featureResult);
					if (LOG.isTraceEnabled()) {
						LOG.trace(featureResult.toString());
					}
				}
			}

			Transition transition = targetConfiguration.getTransitions().get(currentIndex);
			String classification = transition.getCode();
			event = new ClassificationEvent(parseFeatureResults, classification);

			// apply the transition and up the index
			currentConfiguration = new ParseConfiguration(currentConfiguration);
			transition.apply(currentConfiguration);
			currentIndex++;

			if (currentIndex == targetConfiguration.getTransitions().size()) {
				targetConfiguration = null;
			}
		}
		return event;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = new LinkedHashMap<String, String>();
		attributes.put("eventStream", this.getClass().getSimpleName());
		attributes.put("corpusReader", corpusReader.getClass().getSimpleName());

		attributes.putAll(corpusReader.getCharacteristics());
		return attributes;
	}
}
