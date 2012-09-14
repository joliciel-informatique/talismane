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
import java.io.Writer;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureService;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.utils.CorpusEventStream;
import com.joliciel.talismane.utils.DecisionMaker;

class PosTaggerServiceImpl implements PosTaggerServiceInternal {
	PosTaggerFeatureService posTaggerFeatureService;
	PosTaggerService posTaggerService;
	TokeniserService tokeniserService;

	@Override
	public PosTagger getPosTagger(
			Set<PosTaggerFeature<?>> posTaggerFeatures,
			PosTagSet posTagSet,
			DecisionMaker decisionMaker,
			int beamWidth) {
		PosTaggerImpl posTagger = new PosTaggerImpl(posTaggerFeatures, posTagSet, decisionMaker, beamWidth);
		posTagger.setPosTaggerFeatureService(posTaggerFeatureService);
		posTagger.setTokeniserService(tokeniserService);
		posTagger.setPosTaggerService(this);
		posTagger.setLexiconService(TalismaneSession.getLexiconService());
		
		return posTagger;
	}
	
	@Override
	public PosTaggerEvaluator getPosTaggerEvaluator(PosTagger posTagger, Writer csvFileWriter) {
		PosTaggerEvaluatorImpl evaluator = new PosTaggerEvaluatorImpl(posTagger, csvFileWriter);
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
	public PosTaggedToken getPosTaggedToken(Token token, PosTag posTag,
			double probability) {
		PosTaggedTokenImpl posTaggedToken = new PosTaggedTokenImpl(token, posTag, probability);
		posTaggedToken.setLexiconService(TalismaneSession.getLexiconService());
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
	public CorpusEventStream getPosTagEventStream(
			PosTagAnnotatedCorpusReader corpusReader,
			Set<PosTaggerFeature<?>> posTaggerFeatures) {
		PosTagEventStream eventStream = new PosTagEventStream(corpusReader, posTaggerFeatures);
		eventStream.setPosTaggerFeatureService(posTaggerFeatureService);
		eventStream.setPosTaggerService(posTaggerService);
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
	public PosTagRegexBasedCorpusReader getRegexBasedCorpusReader(String regex,
			Reader reader) {
		PosTagRegexBasedCorpusReaderImpl corpusReader = new PosTagRegexBasedCorpusReaderImpl(reader, regex);
		corpusReader.setPosTaggerServiceInternal(this);
		corpusReader.setTokeniserService(this.getTokeniserService());
		return corpusReader;
	}
	
}
