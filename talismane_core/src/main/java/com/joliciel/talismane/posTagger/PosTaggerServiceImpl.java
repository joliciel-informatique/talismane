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

import java.io.File;
import java.io.Reader;
import java.util.Collection;
import java.util.Set;

import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.ExternalResource;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureParser;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokeniserService;

class PosTaggerServiceImpl implements PosTaggerServiceInternal {
	PosTaggerService posTaggerService;
	TokeniserService tokeniserService;
	TalismaneService talismaneService;

	@Override
	public PosTagger getPosTagger(Set<PosTaggerFeature<?>> posTaggerFeatures, DecisionMaker decisionMaker, int beamWidth) {
		PosTaggerImpl posTagger = new PosTaggerImpl(posTaggerFeatures, decisionMaker, beamWidth);
		posTagger.setTokeniserService(tokeniserService);
		posTagger.setPosTaggerService(this);
		posTagger.setTalismaneService(this.getTalismaneService());

		return posTagger;
	}

	@Override
	public PosTagger getPosTagger(ClassificationModel posTaggerModel, int beamWidth) {
		PosTaggerFeatureParser featureParser = new PosTaggerFeatureParser(this.talismaneService.getTalismaneSession());
		Collection<ExternalResource<?>> externalResources = posTaggerModel.getExternalResources();
		if (externalResources != null) {
			for (ExternalResource<?> externalResource : externalResources) {
				featureParser.getExternalResourceFinder().addExternalResource(externalResource);
			}
		}

		Set<PosTaggerFeature<?>> posTaggerFeatures = featureParser.getFeatureSet(posTaggerModel.getFeatureDescriptors());

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
		posTagSequence.setTalismaneService(this.getTalismaneService());
		return posTagSequence;
	}

	@Override
	public PosTagSequence getPosTagSequence(TokenSequence tokenSequence) {
		PosTagSequenceImpl posTagSequence = new PosTagSequenceImpl(tokenSequence);
		posTagSequence.setPosTaggerServiceInternal(this);
		posTagSequence.setTalismaneService(this.getTalismaneService());
		return posTagSequence;

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
	public ClassificationEventStream getPosTagEventStream(PosTagAnnotatedCorpusReader corpusReader, Set<PosTaggerFeature<?>> posTaggerFeatures) {
		PosTagEventStream eventStream = new PosTagEventStream(corpusReader, posTaggerFeatures);
		eventStream.setPosTaggerService(posTaggerService);
		return eventStream;
	}

	@Override
	public PosTagRegexBasedCorpusReader getRegexBasedCorpusReader(Reader reader) {
		PosTagRegexBasedCorpusReaderImpl corpusReader = new PosTagRegexBasedCorpusReaderImpl(reader);
		corpusReader.setPosTaggerServiceInternal(this);
		corpusReader.setTalismaneService(this.getTalismaneService());
		corpusReader.setTokeniserService(this.getTokeniserService());
		return corpusReader;
	}

	@Override
	public PosTagComparator getPosTagComparator() {
		PosTagComparatorImpl comparator = new PosTagComparatorImpl();
		return comparator;
	}

	@Override
	public PosTagSequenceProcessor getPosTagFeatureTester(Set<PosTaggerFeature<?>> posTaggerFeatures, Set<String> testWords, File file) {
		PosTagFeatureTester tester = new PosTagFeatureTester(posTaggerFeatures, testWords, file);
		tester.setPosTaggerService(this);
		return tester;
	}

	public TalismaneService getTalismaneService() {
		return talismaneService;
	}

	public void setTalismaneService(TalismaneService talismaneService) {
		this.talismaneService = talismaneService;
	}

}
