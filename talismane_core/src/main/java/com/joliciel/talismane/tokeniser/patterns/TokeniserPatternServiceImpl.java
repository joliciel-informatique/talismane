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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.ExternalResource;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.features.TokenPatternMatchFeature;
import com.joliciel.talismane.tokeniser.features.TokenPatternMatchFeatureParser;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeatureParser;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;

class TokeniserPatternServiceImpl implements TokeniserPatternServiceInternal {
	private TokeniserService tokeniserService;
	private TokenFilterService tokenFilterService;
	private TalismaneService talismaneService;

	@Override
	public TokeniserPatternManager getPatternManager(List<String> patternDescriptors) {
		TokeniserPatternManagerImpl patternManager = new TokeniserPatternManagerImpl(patternDescriptors);
		patternManager.setTokeniserPatternService(this);
		patternManager.setTokeniserService(this.getTokeniserService());
		return patternManager;
	}

	@Override
	public TokenPattern getTokeniserPattern(String regexp, Pattern separatorPattern) {
		TokenPatternImpl pattern = new TokenPatternImpl(regexp, separatorPattern);
		pattern.setTokeniserPatternServiceInternal(this);
		return pattern;
	}

	@Override
	public Tokeniser getIntervalPatternTokeniser(TokeniserPatternManager patternManager, Set<TokeniserContextFeature<?>> tokeniserContextFeatures,
			DecisionMaker decisionMaker, int beamWidth) {
		IntervalPatternTokeniser tokeniser = new IntervalPatternTokeniser(patternManager, tokeniserContextFeatures, beamWidth,
				talismaneService.getTalismaneSession());
		tokeniser.setTokeniserPatternService(this);
		tokeniser.setTokeniserService(this.getTokeniserService());
		tokeniser.setDecisionMaker(decisionMaker);
		return tokeniser;
	}

	@Override
	public Tokeniser getCompoundPatternTokeniser(TokeniserPatternManager patternManager, Set<TokenPatternMatchFeature<?>> features, DecisionMaker decisionMaker,
			int beamWidth) {
		CompoundPatternTokeniser tokeniser = new CompoundPatternTokeniser(patternManager, features, beamWidth, talismaneService.getTalismaneSession());
		tokeniser.setTokeniserPatternService(this);
		tokeniser.setTokeniserService(this.getTokeniserService());
		tokeniser.setDecisionMaker(decisionMaker);
		return tokeniser;

	}

	@Override
	public Tokeniser getPatternTokeniser(ClassificationModel tokeniserModel, int beamWidth) {
		TokeniserPatternManager patternManager = this.getPatternManager(tokeniserModel.getDescriptors().get(TokeniserPatternService.PATTERN_DESCRIPTOR_KEY));

		PatternTokeniserType patternTokeniserType = PatternTokeniserType
				.valueOf((String) tokeniserModel.getModelAttributes().get(PatternTokeniserType.class.getSimpleName()));

		Tokeniser tokeniser = null;
		if (patternTokeniserType == PatternTokeniserType.Interval) {
			TokeniserContextFeatureParser featureParser = new TokeniserContextFeatureParser(this.talismaneService.getTalismaneSession(),
					patternManager.getParsedTestPatterns());
			Collection<ExternalResource<?>> externalResources = tokeniserModel.getExternalResources();
			if (externalResources != null) {
				for (ExternalResource<?> externalResource : externalResources) {
					featureParser.getExternalResourceFinder().addExternalResource(externalResource);
				}
			}
			Set<TokeniserContextFeature<?>> tokeniserContextFeatures = featureParser.getTokeniserContextFeatureSet(tokeniserModel.getFeatureDescriptors());

			tokeniser = this.getIntervalPatternTokeniser(patternManager, tokeniserContextFeatures, tokeniserModel.getDecisionMaker(), beamWidth);
		} else {
			TokenPatternMatchFeatureParser featureParser = new TokenPatternMatchFeatureParser(this.talismaneService.getTalismaneSession());
			Collection<ExternalResource<?>> externalResources = tokeniserModel.getExternalResources();
			if (externalResources != null) {
				for (ExternalResource<?> externalResource : externalResources) {
					featureParser.getExternalResourceFinder().addExternalResource(externalResource);
				}
			}
			Set<TokenPatternMatchFeature<?>> features = featureParser.getTokenPatternMatchFeatureSet(tokeniserModel.getFeatureDescriptors());

			tokeniser = this.getCompoundPatternTokeniser(patternManager, features, tokeniserModel.getDecisionMaker(), beamWidth);
		}
		return tokeniser;
	}

	@Override
	public ClassificationEventStream getIntervalPatternEventStream(TokeniserAnnotatedCorpusReader corpusReader,
			Set<TokeniserContextFeature<?>> tokeniserContextFeatures, TokeniserPatternManager patternManager) {
		IntervalPatternEventStream eventStream = new IntervalPatternEventStream(corpusReader, tokeniserContextFeatures, talismaneService.getTalismaneSession());

		eventStream.setTokeniserPatternService(this);
		eventStream.setTokeniserService(this.getTokeniserService());
		eventStream.setTokeniserPatternManager(patternManager);
		eventStream.setTokenFilterService(this.getTokenFilterService());
		return eventStream;
	}

	@Override
	public ClassificationEventStream getCompoundPatternEventStream(TokeniserAnnotatedCorpusReader corpusReader, Set<TokenPatternMatchFeature<?>> features,
			TokeniserPatternManager patternManager) {
		CompoundPatternEventStream eventStream = new CompoundPatternEventStream(corpusReader, features, talismaneService.getTalismaneSession());

		eventStream.setTokeniserPatternService(this);
		eventStream.setTokeniserService(this.getTokeniserService());
		eventStream.setTokeniserPatternManager(patternManager);
		eventStream.setTokenFilterService(this.getTokenFilterService());
		return eventStream;

	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public TokenFilterService getTokenFilterService() {
		return tokenFilterService;
	}

	public void setTokenFilterService(TokenFilterService tokenFilterService) {
		this.tokenFilterService = tokenFilterService;
	}

	@Override
	public TokenPatternMatch getTokenPatternMatch(TokenPatternMatchSequence sequence, Token token, TokenPattern pattern, int index) {
		TokenPatternMatchImpl match = new TokenPatternMatchImpl(sequence, token, pattern, index);
		return match;
	}

	@Override
	public TokenPatternMatchSequence getTokenPatternMatchSequence(TokenPattern tokenPattern, List<Token> tokenSequence) {
		TokenPatternMatchSequenceImpl sequence = new TokenPatternMatchSequenceImpl(tokenPattern, tokenSequence);
		return sequence;
	}

	public TalismaneService getTalismaneService() {
		return talismaneService;
	}

	public void setTalismaneService(TalismaneService talismaneService) {
		this.talismaneService = talismaneService;
	}

}
