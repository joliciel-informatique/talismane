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
package com.joliciel.talismane.languageDetector;

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

/**
 * A ClassificationEventStream for language detection training.
 * 
 * @author Assaf Urieli
 *
 */
public class LanguageDetectorEventStream implements ClassificationEventStream {
	private static final Logger LOG = LoggerFactory.getLogger(LanguageDetectorEventStream.class);

	private LanguageDetectorAnnotatedCorpusReader corpusReader;
	private Set<LanguageDetectorFeature<?>> features;

	public LanguageDetectorEventStream(LanguageDetectorAnnotatedCorpusReader corpusReader, Set<LanguageDetectorFeature<?>> features) {
		super();
		this.corpusReader = corpusReader;
		this.features = features;
	}

	@Override
	public boolean hasNext() {
		return this.corpusReader.hasNextText();
	}

	@Override
	public ClassificationEvent next() throws TalismaneException {
		LanguageTaggedText languageTaggedText = this.corpusReader.nextText();
		List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();
		for (LanguageDetectorFeature<?> feature : features) {
			RuntimeEnvironment env = new RuntimeEnvironment();
			FeatureResult<?> featureResult = feature.check(languageTaggedText.getText(), env);
			if (featureResult != null)
				featureResults.add(featureResult);
		}
		String classification = languageTaggedText.getLanguage().toLanguageTag();

		if (LOG.isTraceEnabled()) {
			for (FeatureResult<?> result : featureResults) {
				LOG.trace(result.toString());
			}
			LOG.trace("classification: " + classification);
		}

		ClassificationEvent event = new ClassificationEvent(featureResults, classification);
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
