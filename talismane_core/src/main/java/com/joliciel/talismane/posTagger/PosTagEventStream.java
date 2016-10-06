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
package com.joliciel.talismane.posTagger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.ClassificationEvent;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;

/**
 * A corpus event stream for postagging.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTagEventStream implements ClassificationEventStream {
	private static final Logger LOG = LoggerFactory.getLogger(PosTagEventStream.class);

	private final PosTagAnnotatedCorpusReader corpusReader;
	private final Set<PosTaggerFeature<?>> posTaggerFeatures;

	PosTagSequence currentSentence;
	PosTagSequence currentHistory;
	int currentIndex;

	/**
	 * Create a classification event stream for a given corpus reader and set of
	 * features.
	 * 
	 * @param corpusReader
	 *            the corpus reader used to read the training corpus
	 * @param posTaggerFeatures
	 *            the features used to describe events in the training corpus
	 */
	public PosTagEventStream(PosTagAnnotatedCorpusReader corpusReader, Set<PosTaggerFeature<?>> posTaggerFeatures) {
		this.corpusReader = corpusReader;
		this.posTaggerFeatures = posTaggerFeatures;
	}

	@Override
	public boolean hasNext() {
		while (currentSentence == null) {
			if (this.corpusReader.hasNextPosTagSequence()) {
				currentSentence = this.corpusReader.nextPosTagSequence();
				if (LOG.isDebugEnabled())
					LOG.debug("### next sentence: " + currentSentence.getTokenSequence().getSentence());
				currentIndex = 0;
				currentHistory = new PosTagSequence(currentSentence.getTokenSequence());
				if (currentIndex == currentSentence.size()) {
					currentSentence = null;
				}
			} else {
				break;
			}
		}
		return currentSentence != null;
	}

	@Override
	public ClassificationEvent next() {
		ClassificationEvent event = null;
		if (this.hasNext()) {
			PosTaggedToken taggedToken = currentSentence.get(currentIndex++);
			String classification = taggedToken.getTag().getCode();

			if (LOG.isDebugEnabled())
				LOG.debug("next event, token: " + taggedToken.getToken().getText() + " : " + classification);
			PosTaggerContext context = new PosTaggerContextImpl(taggedToken.getToken(), currentHistory);

			List<FeatureResult<?>> posTagFeatureResults = new ArrayList<FeatureResult<?>>();
			for (PosTaggerFeature<?> posTaggerFeature : posTaggerFeatures) {
				RuntimeEnvironment env = new RuntimeEnvironment();
				FeatureResult<?> featureResult = posTaggerFeature.check(context, env);
				if (featureResult != null)
					posTagFeatureResults.add(featureResult);
			}

			if (LOG.isTraceEnabled()) {
				LOG.trace("Token: " + taggedToken.getToken().getText());
				for (FeatureResult<?> result : posTagFeatureResults) {
					LOG.trace(result.toString());
				}
			}
			event = new ClassificationEvent(posTagFeatureResults, classification);

			currentHistory.addPosTaggedToken(taggedToken);
			if (currentIndex == currentSentence.size()) {
				currentSentence = null;
			}
		}
		return event;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = new LinkedHashMap<String, String>();
		attributes.put("eventStream", this.getClass().getSimpleName());
		attributes.put("corpusReader", corpusReader.getClass().getSimpleName());

		return attributes;
	}
}
