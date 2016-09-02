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
package com.joliciel.talismane.sentenceDetector;

import java.io.Reader;
import java.util.Collection;
import java.util.Set;

import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.ExternalResource;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureService;
import com.joliciel.talismane.tokeniser.TokeniserService;

public class SentenceDetectorServiceImpl implements SentenceDetectorService {
	TokeniserService tokeniserService;
	TalismaneService talismaneService;
	SentenceDetectorFeatureService sentenceDetectorFeatureService;

	@Override
	public PossibleSentenceBoundary getPossibleSentenceBoundary(String text, int index) {
		PossibleSentenceBoundaryImpl boundary = new PossibleSentenceBoundaryImpl(text, index, talismaneService.getTalismaneSession());
		return boundary;
	}

	@Override
	public SentenceDetector getSentenceDetector(DecisionMaker decisionMaker, Set<SentenceDetectorFeature<?>> features) {
		SentenceDetectorImpl sentenceDetector = new SentenceDetectorImpl(decisionMaker, features);
		sentenceDetector.setSentenceDetectorService(this);
		return sentenceDetector;
	}

	@Override
	public SentenceDetector getSentenceDetector(ClassificationModel sentenceModel) {
		Collection<ExternalResource<?>> externalResources = sentenceModel.getExternalResources();
		if (externalResources != null) {
			for (ExternalResource<?> externalResource : externalResources) {
				this.getSentenceDetectorFeatureService().getExternalResourceFinder().addExternalResource(externalResource);
			}
		}

		Set<SentenceDetectorFeature<?>> sentenceDetectorFeatures = this.getSentenceDetectorFeatureService()
				.getFeatureSet(sentenceModel.getFeatureDescriptors());
		SentenceDetector sentenceDetector = this.getSentenceDetector(sentenceModel.getDecisionMaker(), sentenceDetectorFeatures);
		return sentenceDetector;
	}

	@Override
	public SentenceDetectorEvaluator getEvaluator(SentenceDetector sentenceDetector) {
		SentenceDetectorEvaluatorImpl evaluator = new SentenceDetectorEvaluatorImpl(sentenceDetector);
		return evaluator;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	@Override
	public ClassificationEventStream getSentenceDetectorEventStream(SentenceDetectorAnnotatedCorpusReader corpusReader,
			Set<SentenceDetectorFeature<?>> features) {
		SentenceDetectorEventStream eventStream = new SentenceDetectorEventStream(corpusReader, features);
		eventStream.setSentenceDetectorService(this);
		return eventStream;
	}

	@Override
	public SentenceDetectorAnnotatedCorpusReader getDefaultReader(Reader reader) {
		SentencePerLineCorpusReader corpusReader = new SentencePerLineCorpusReader(reader);
		return corpusReader;
	}

	public TalismaneService getTalismaneService() {
		return talismaneService;
	}

	public void setTalismaneService(TalismaneService talismaneService) {
		this.talismaneService = talismaneService;
	}

	public SentenceDetectorFeatureService getSentenceDetectorFeatureService() {
		return sentenceDetectorFeatureService;
	}

	public void setSentenceDetectorFeatureService(SentenceDetectorFeatureService sentenceDetectorFeatureService) {
		this.sentenceDetectorFeatureService = sentenceDetectorFeatureService;
	}

}
