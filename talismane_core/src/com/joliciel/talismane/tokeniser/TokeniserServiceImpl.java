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
package com.joliciel.talismane.tokeniser;

import java.io.Reader;
import java.util.Set;
import java.util.regex.Pattern;

import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.tokeniser.features.TokenFeatureService;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternService;

class TokeniserServiceImpl implements TokeniserServiceInternal {
	private TokenFeatureService tokenFeatureService;
	private TokeniserPatternService tokeniserPatternService;
	private TokenFilterService tokenFilterService;
	private FilterService filterService;
	private MachineLearningService machineLearningService;
	private FeatureService featureService;

	@Override
	public Token getToken(String string, TokenSequence tokenSequence, int index) {
		return this.getTokenInternal(string, tokenSequence, index);
	}
	
	@Override
	public TokenInternal getTokenInternal(String string,
			TokenSequence tokenSequence, int index) {
		TokenImpl token = new TokenImpl(string, tokenSequence, index);
		return token;
	}

	@Override
	public TokenSequence getTokenSequence(Sentence sentence, TokenisedAtomicTokenSequence tokeniserDecisionTagSequence) {
		TokenSequenceImpl tokenSequence = new TokenSequenceImpl(sentence, tokeniserDecisionTagSequence);
		tokenSequence.setTokeniserServiceInternal(this);
		return tokenSequence;
	}
	
	@Override
	public TokenSequence getTokenSequence(Sentence sentence) {
		TokenSequenceImpl tokenSequence = new TokenSequenceImpl(sentence);
		tokenSequence.setTokeniserServiceInternal(this);
		return tokenSequence;
	}
	
	@Override
	public TokenSequence getTokenSequence(Sentence sentence, Pattern separatorPattern) {
		TokenSequenceImpl tokenSequence = new TokenSequenceImpl(sentence, separatorPattern, this);
		return tokenSequence;
	}

	@Override
	public TokenSequence getTokenSequence(Sentence sentence,
			Pattern separatorPattern, Set<TokenPlaceholder> placeholders) {
		TokenSequenceImpl tokenSequence = new TokenSequenceImpl(sentence, separatorPattern, placeholders, this);
		return tokenSequence;
	}

	@Override
	public PretokenisedSequence getEmptyPretokenisedSequence() {
		PretokenisedSequenceImpl tokenSequence = new PretokenisedSequenceImpl(filterService);
		tokenSequence.setTokeniserServiceInternal(this);
		return tokenSequence;
	}

	@Override
	public Tokeniser getSimpleTokeniser() {
		SimpleTokeniser tokeniser = new SimpleTokeniser();
		tokeniser.setTokeniserServiceInternal(this);
		tokeniser.setFilterService(this.getFilterService());
		return tokeniser;
	}
	

	@Override
	public TokeniserEvaluator getTokeniserEvaluator(Tokeniser tokeniser) {
		TokeniserEvaluatorImpl evaluator = new TokeniserEvaluatorImpl();
		evaluator.setTokeniser(tokeniser);
		return evaluator;
	}

	@Override
	public <T extends TokenTag> TaggedToken<T> getTaggedToken(Token token, Decision<T> decision) {
		TaggedToken<T> taggedToken = new TaggedTokenImpl<T>(token, decision);
		return taggedToken;
	}

	@Override
	public <T extends TokenTag> TaggedTokenSequence<T> getTaggedTokenSequence(int initialCapacity) {
		TaggedTokenSequence<T> sequence = new TaggedTokenSequenceImpl<T>(initialCapacity);
		return sequence;
	}

	@Override
	public <T extends TokenTag> TaggedTokenSequence<T> getTaggedTokenSequence(
			TaggedTokenSequence<T> history) {
		TaggedTokenSequence<T> sequence = new TaggedTokenSequenceImpl<T>(history);
		return sequence;
	}

	@Override
	public TokenisedAtomicTokenSequence getTokenisedAtomicTokenSequence(
			Sentence sentence, int initialCapacity) {
		TokenisedAtomicTokenSequenceImpl sequence = new TokenisedAtomicTokenSequenceImpl(sentence, initialCapacity);
		sequence.setTokeniserServiceInternal(this);
		return sequence;
	}

	@Override
	public TokenisedAtomicTokenSequence getTokenisedAtomicTokenSequence(
			TokenisedAtomicTokenSequence history) {
		TokenisedAtomicTokenSequenceImpl sequence = new TokenisedAtomicTokenSequenceImpl(history);
		sequence.setTokeniserServiceInternal(this);
		return sequence;
	}

	public TokenFeatureService getTokenFeatureService() {
		return tokenFeatureService;
	}

	public void setTokenFeatureService(TokenFeatureService tokenFeatureService) {
		this.tokenFeatureService = tokenFeatureService;
	}

	public TokeniserPatternService getTokeniserPatternService() {
		return tokeniserPatternService;
	}

	public void setTokeniserPatternService(
			TokeniserPatternService tokeniserPatternService) {
		this.tokeniserPatternService = tokeniserPatternService;
	}

	@Override
	public DecisionFactory<TokeniserOutcome> getDecisionFactory() {
		TokeniserDecisionFactory factory = new TokeniserDecisionFactory();
		return factory;
	}


	@Override
	public TokenRegexBasedCorpusReader getRegexBasedCorpusReader(Reader reader) {
		TokenRegexBasedCorpusReaderImpl corpusReader = new TokenRegexBasedCorpusReaderImpl(reader);
		corpusReader.setTokeniserService(this);
		corpusReader.setTokenFilterService(this.getTokenFilterService());
		return corpusReader;
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


	public TokenFilterService getTokenFilterService() {
		return tokenFilterService;
	}


	public void setTokenFilterService(TokenFilterService tokenFilterService) {
		this.tokenFilterService = tokenFilterService;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}


	
}
