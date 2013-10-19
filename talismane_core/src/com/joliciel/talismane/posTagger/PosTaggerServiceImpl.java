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
package com.joliciel.talismane.posTagger;

import java.io.File;
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.ExternalResource;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureService;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokeniserService;

class PosTaggerServiceImpl implements PosTaggerServiceInternal {
	PosTaggerFeatureService posTaggerFeatureService;
	PosTaggerService posTaggerService;
	TokeniserService tokeniserService;
	MachineLearningService machineLearningService;
	FeatureService featureService;

	@Override
	public PosTagger getPosTagger(
			Set<PosTaggerFeature<?>> posTaggerFeatures,
			DecisionMaker<PosTag> decisionMaker,
			int beamWidth) {
		PosTaggerImpl posTagger = new PosTaggerImpl(posTaggerFeatures, decisionMaker, beamWidth);
		posTagger.setPosTaggerFeatureService(posTaggerFeatureService);
		posTagger.setTokeniserService(tokeniserService);
		posTagger.setPosTaggerService(this);
		posTagger.setFeatureService(this.featureService);
		
		return posTagger;
	}
	
	public PosTagger getPosTagger(ClassificationModel<PosTag> posTaggerModel,
			int beamWidth) {
		Collection<ExternalResource<?>> externalResources = posTaggerModel.getExternalResources();
		if (externalResources!=null) {
			for (ExternalResource<?> externalResource : externalResources) {
				this.getPosTaggerFeatureService().getExternalResourceFinder().addExternalResource(externalResource);
			}
		}
		
		Set<PosTaggerFeature<?>> posTaggerFeatures = this.getPosTaggerFeatureService().getFeatureSet(posTaggerModel.getFeatureDescriptors());
		
		PosTagger posTagger = this.getPosTagger(posTaggerFeatures, posTaggerModel.getDecisionMaker(), beamWidth);
		return posTagger;
	}
	
	@Override
	public PosTaggerEvaluator getPosTaggerEvaluator(PosTagger posTagger) {
		PosTaggerEvaluator evaluator = new PosTaggerEvaluatorImpl(posTagger);
		return evaluator;
	}

	@Override
	public PosTagSequence getPosTagSequence(PosTagSequence history) {
		PosTagSequenceImpl posTagSequence = new PosTagSequenceImpl(history);
		posTagSequence.setPosTaggerServiceInternal(this);
		return posTagSequence;
	}

	@Override
	public PosTagSequence getPosTagSequence(TokenSequence tokenSequence,
			int initialCapacity) {
		PosTagSequenceImpl posTagSequence = new PosTagSequenceImpl(tokenSequence, initialCapacity);
		posTagSequence.setPosTaggerServiceInternal(this);
		return posTagSequence;

	}

	@Override
	public PosTaggedToken getPosTaggedToken(Token token, Decision<PosTag> decision) {
		PosTaggedTokenImpl posTaggedToken = new PosTaggedTokenImpl(token, decision);
		return posTaggedToken;
	}
	
	

	@Override
	public PosTag getPosTag(String code, String description,
			PosTagOpenClassIndicator openClassIndicator) {
		PosTagImpl posTag = new PosTagImpl(code, description, openClassIndicator);
		return posTag;
	}

	public PosTaggerFeatureService getPosTaggerFeatureService() {
		return posTaggerFeatureService;
	}

	public void setPosTaggerFeatureService(
			PosTaggerFeatureService posTaggerFeatureService) {
		this.posTaggerFeatureService = posTaggerFeatureService;
	}

	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokenizerService) {
		this.tokeniserService = tokenizerService;
	}


	@Override
	public ClassificationEventStream getPosTagEventStream(
			PosTagAnnotatedCorpusReader corpusReader,
			Set<PosTaggerFeature<?>> posTaggerFeatures) {
		PosTagEventStream eventStream = new PosTagEventStream(corpusReader, posTaggerFeatures);
		eventStream.setPosTaggerFeatureService(posTaggerFeatureService);
		eventStream.setPosTaggerService(posTaggerService);
		eventStream.setMachineLearningService(machineLearningService);
		eventStream.setFeatureService(featureService);
		return eventStream;
	}


	@Override
	public PosTagSet getPosTagSet(File file) {
		PosTagSetImpl posTagSet = new PosTagSetImpl(file);
		return posTagSet;
	}

	@Override
	public PosTagSet getPosTagSet(Scanner scanner) {
		PosTagSetImpl posTagSet = new PosTagSetImpl(scanner);
		return posTagSet;
	}

	@Override
	public PosTagSet getPosTagSet(List<String> descriptors) {
		PosTagSetImpl posTagSet = new PosTagSetImpl(descriptors);
		return posTagSet;
	}

	@Override
	public PosTagRegexBasedCorpusReader getRegexBasedCorpusReader(Reader reader) {
		PosTagRegexBasedCorpusReaderImpl corpusReader = new PosTagRegexBasedCorpusReaderImpl(reader);
		corpusReader.setPosTaggerServiceInternal(this);
		corpusReader.setTokeniserService(this.getTokeniserService());
		return corpusReader;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	@Override
	public PosTagComparator getPosTagComparator() {
		PosTagComparatorImpl comparator = new PosTagComparatorImpl();
		return comparator;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}

	@Override
	public PosTagSequenceProcessor getPosTagFeatureTester(
			Set<PosTaggerFeature<?>> posTaggerFeatures, Set<String> testWords,
			File file) {
		PosTagFeatureTester tester = new PosTagFeatureTester(posTaggerFeatures, testWords, file);
		tester.setFeatureService(this.getFeatureService());
		tester.setPosTaggerFeatureService(this.getPosTaggerFeatureService());
		tester.setPosTaggerService(this);
		return tester;
	}

	
}
