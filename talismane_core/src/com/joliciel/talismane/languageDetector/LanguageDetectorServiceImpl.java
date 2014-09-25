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

import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.tokeniser.TokeniserService;

public class LanguageDetectorServiceImpl implements LanguageDetectorService {
	TokeniserService tokeniserService;
	MachineLearningService machineLearningService;
	FilterService filterService;
	FeatureService featureService;
	
	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	@Override
	public ClassificationEventStream getLanguageDetectorEventStream(
			LanguageDetectorAnnotatedCorpusReader corpusReader,
			Set<LanguageDetectorFeature<?>> features) {
		LanguageDetectorEventStream eventStream = new LanguageDetectorEventStream(corpusReader, features);
		eventStream.setMachineLearningService(machineLearningService);
		eventStream.setFeatureService(featureService);
		return eventStream;
	}

	@Override
	public DecisionFactory<LanguageOutcome> getDecisionFactory() {
		LanguageDetectorDecisionFactory factory = new LanguageDetectorDecisionFactory();
		return factory;
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
	
	@Override
	public LanguageDetectorAnnotatedCorpusReader getDefaultReader(Map<Locale,Reader> readerMap) {
		TextPerLineCorpusReader corpusReader = new TextPerLineCorpusReader(readerMap);
		return corpusReader;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}

	@Override
	public Set<LanguageDetectorFeature<?>> getFeatureSet(
			List<String> featureDescriptors) {
		Set<LanguageDetectorFeature<?>> features = new HashSet<LanguageDetectorFeature<?>>();
		for (String descriptor : featureDescriptors) {
			if (descriptor.startsWith("CharNgram")) {
				int n = Integer.parseInt(descriptor.substring(descriptor.indexOf('(')+1, descriptor.lastIndexOf(')')));
				CharacterNgramFeature charNgramFeature = new CharacterNgramFeature(n);
				features.add(charNgramFeature);
			} else {
				throw new TalismaneException("Unknown language feature descriptor: " + descriptor);
			}
		}
		return features;
	}


	@Override
	public LanguageDetector getLanguageDetector(
			DecisionMaker<LanguageOutcome> decisionMaker,
			Set<LanguageDetectorFeature<?>> features) {
		LanguageDetectorImpl languageDetector = new LanguageDetectorImpl(decisionMaker, features);
		languageDetector.setLanguageDetectorService(this);
		languageDetector.setFeatureService(featureService);
		return languageDetector;
	}

	@Override
	public LanguageDetector getLanguageDetector(
			ClassificationModel<LanguageOutcome> languageModel) {
		Set<LanguageDetectorFeature<?>> languageDetectorFeatures =
			this.getFeatureSet(languageModel.getFeatureDescriptors());
		LanguageDetector languageDetector = this.getLanguageDetector(languageModel.getDecisionMaker(), languageDetectorFeatures);
		return languageDetector;
	}

	@Override
	public LanguageDetectorProcessor getDefaultLanguageDetectorProcessor(
			Writer out) {
		DefaultLanguageDetectorProcessor processor = new DefaultLanguageDetectorProcessor(out);
		return processor;
	}

}
