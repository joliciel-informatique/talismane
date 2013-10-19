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
package com.joliciel.talismane.sentenceDetector;

import java.io.Reader;
import java.util.Collection;
import java.util.Set;

import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.ExternalResource;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureService;
import com.joliciel.talismane.tokeniser.TokeniserService;

public class SentenceDetectorServiceImpl implements SentenceDetectorService {
	SentenceDetectorFeatureService sentenceDetectorFeatureService;
	TokeniserService tokeniserService;
	MachineLearningService machineLearningService;
	FilterService filterService;
	FeatureService featureService;
	
	@Override
	public PossibleSentenceBoundary getPossibleSentenceBoundary(String text,
			int index) {
		PossibleSentenceBoundaryImpl boundary = new PossibleSentenceBoundaryImpl(text, index);
		boundary.setTokeniserService(tokeniserService);
		boundary.setFilterService(filterService);
		return boundary;
	}

	@Override
	public SentenceDetector getSentenceDetector(
			DecisionMaker<SentenceDetectorOutcome> decisionMaker,
			Set<SentenceDetectorFeature<?>> features) {
		SentenceDetectorImpl sentenceDetector = new SentenceDetectorImpl(decisionMaker, features);
		sentenceDetector.setSentenceDetectorService(this);
		sentenceDetector.setSentenceDetectorFeatureService(sentenceDetectorFeatureService);
		sentenceDetector.setFeatureService(featureService);
		return sentenceDetector;
	}

	@Override
	public SentenceDetector getSentenceDetector(
			ClassificationModel<SentenceDetectorOutcome> sentenceModel) {
		Collection<ExternalResource<?>> externalResources = sentenceModel.getExternalResources();
		if (externalResources!=null) {
			for (ExternalResource<?> externalResource : externalResources) {
				this.getSentenceDetectorFeatureService().getExternalResourceFinder().addExternalResource(externalResource);
			}
		}

		Set<SentenceDetectorFeature<?>> sentenceDetectorFeatures =
			this.getSentenceDetectorFeatureService().getFeatureSet(sentenceModel.getFeatureDescriptors());
		SentenceDetector sentenceDetector = this.getSentenceDetector(sentenceModel.getDecisionMaker(), sentenceDetectorFeatures);
		return sentenceDetector;
	}

	@Override
	public SentenceDetectorEvaluator getEvaluator(
			SentenceDetector sentenceDetector) {
		SentenceDetectorEvaluatorImpl evaluator = new SentenceDetectorEvaluatorImpl(sentenceDetector);
		return evaluator;
	}

	public SentenceDetectorFeatureService getSentenceDetectorFeatureService() {
		return sentenceDetectorFeatureService;
	}

	public void setSentenceDetectorFeatureService(
			SentenceDetectorFeatureService sentenceDetectorFeatureService) {
		this.sentenceDetectorFeatureService = sentenceDetectorFeatureService;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}


	@Override
	public ClassificationEventStream getSentenceDetectorEventStream(
			SentenceDetectorAnnotatedCorpusReader corpusReader,
			Set<SentenceDetectorFeature<?>> features) {
		SentenceDetectorEventStream eventStream = new SentenceDetectorEventStream(corpusReader, features);
		eventStream.setSentenceDetectorService(this);
		eventStream.setSentenceDetectorFeatureService(sentenceDetectorFeatureService);
		eventStream.setMachineLearningService(machineLearningService);
		eventStream.setFeatureService(featureService);
		return eventStream;
	}

	@Override
	public DecisionFactory<SentenceDetectorOutcome> getDecisionFactory() {
		SentenceDetectorDecisionFactory factory = new SentenceDetectorDecisionFactory();
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
	public SentenceDetectorAnnotatedCorpusReader getDefaultReader(Reader reader) {
		SentencePerLineCorpusReader corpusReader = new SentencePerLineCorpusReader(reader);
		return corpusReader;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}



}
