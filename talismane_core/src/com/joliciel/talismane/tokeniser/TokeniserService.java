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
package com.joliciel.talismane.tokeniser;

import java.io.Reader;
import java.util.List;
import java.util.regex.Pattern;

import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternManager;

public interface TokeniserService {
	public Token getToken(String string, TokenSequence tokenSequence, int index);
	
	/**
	 * Create a token sequence for a given sentence.
	 * @param sentence
	 * @return
	 */
	public TokenSequence getTokenSequence(Sentence sentence);
	
	/**
	 * Create a token sequence from a given sentence,
	 * pre-separated into tokens matching the separatorPattern.
	 * @param sentence
	 * @param separatorPattern
	 * @return
	 */
	public TokenSequence getTokenSequence(Sentence sentence, Pattern separatorPattern);
	
	
	/**
	 * Create a token sequence from a given sentence,
	 * pre-separated into tokens matching the separatorPattern,
	 * except for the placeholders provided.
	 * @param sentence
	 * @param separatorPattern
	 * @return
	 */
	public TokenSequence getTokenSequence(Sentence sentence, Pattern separatorPattern, List<TokenPlaceholder> placeholders);

	public PretokenisedSequence getEmptyPretokenisedSequence();
	public PretokenisedSequence getEmptyPretokenisedSequence(String sentenceText);
	
	public Tokeniser getSimpleTokeniser();
	
	public TokeniserEvaluator getTokeniserEvaluator(Tokeniser tokeniser);
	public TokenComparator getTokenComparator(TokeniserAnnotatedCorpusReader referenceCorpusReader,
			TokeniserAnnotatedCorpusReader evaluationCorpusReader,
			TokeniserPatternManager tokeniserPatternManager);
	
	public TaggedToken<TokeniserOutcome> getTaggedToken(Token token, Decision decision);
	
	public <T extends TokenTag> TaggedToken<T> getTaggedToken(Token token, Decision decision, T tag);
	
	public <T extends TokenTag> TaggedTokenSequence<T> getTaggedTokenSequence(int initialCapacity);
	public <T extends TokenTag> TaggedTokenSequence<T> getTaggedTokenSequence(TaggedTokenSequence<T> history);
	
	public TokenisedAtomicTokenSequence getTokenisedAtomicTokenSequence(
			TokenisedAtomicTokenSequence history);
	
	public TokenisedAtomicTokenSequence getTokenisedAtomicTokenSequence(
			Sentence sentence,
			int initialCapacity);
	
	/**
	 * Returns a corpus reader based on the use of Regex.
	 * See class description for details.
	 * @param reader
	 * @return
	 */
	TokenRegexBasedCorpusReader getRegexBasedCorpusReader(Reader reader);
}
