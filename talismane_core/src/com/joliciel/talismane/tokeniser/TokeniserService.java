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

import java.util.Set;
import java.util.regex.Pattern;

import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternManager;

public interface TokeniserService {
	public Token getToken(String string, TokenSequence tokenSequence, int index);
	
	/**
	 * Create a token sequence for a given sentence.
	 * @param sentence
	 * @return
	 */
	public TokenSequence getTokenSequence(String sentence);
	
	/**
	 * Create a token sequence from a given sentence,
	 * pre-separated into tokens matching the separatorPattern.
	 * @param sentence
	 * @param separatorPattern
	 * @return
	 */
	public TokenSequence getTokenSequence(String sentence, Pattern separatorPattern);
	
	public PretokenisedSequence getEmptyPretokenisedSequence();
	
	public Tokeniser getSimpleTokeniser();
	
	public TokeniserEvaluator getTokeniserEvaluator(Tokeniser tokeniser, String separators);
	public TokeniserEvaluator getTokeniserEvaluator(Tokeniser tokeniser, Pattern separatorPattern);
	
	public <T extends TokenTag> TaggedToken<T> getTaggedToken(Token token, T tag, double probability);
	
	public <T extends TokenTag> TaggedTokenSequence<T> getTaggedTokenSequence(int initialCapacity);
	public <T extends TokenTag> TaggedTokenSequence<T> getTaggedTokenSequence(TaggedTokenSequence<T> history);
	
	public TokeniserDecisionTagSequence getTokeniserDecisionTagSequence(
			TokeniserDecisionTagSequence history);

	public TokeniserDecisionTagSequence getTokeniserDecisionTagSequence(
			String sentence,
			int initialCapacity);


	public TokeniserEventStream getTokeniserEventStream(TokeniserAnnotatedCorpusReader corpusReader,
			Set<TokeniserContextFeature<?>> tokeniserContextFeatures, TokeniserPatternManager patternManager);

	
}
