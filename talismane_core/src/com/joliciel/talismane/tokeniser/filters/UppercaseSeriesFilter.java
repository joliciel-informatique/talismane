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
package com.joliciel.talismane.tokeniser.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;

/**
 * Looks for a series of 2+ words in all upper-case, and transforms them into lower-case.
 * Unknown words will be given an initial upper case.
 * @author Assaf Urieli
 *
 */
public class UppercaseSeriesFilter implements TokenSequenceFilter, NeedsTalismaneSession {
	/**
	 * No more than 1000 different lowercase words to check per single uppercase word.
	 */
	private static final int MAX_WORD_ATTEMPTS = 1000;
	
	private TalismaneSession talismaneSession;
	
	public UppercaseSeriesFilter() {
		super();
	}

	@Override
	public void apply(TokenSequence tokenSequence) {
		List<Token> upperCaseSequence = new ArrayList<Token>();
		for (Token token : tokenSequence) {
			String word = token.getText();
			
			if (word.length()==0)
				continue;
			
			boolean hasLowerCase = false;
			boolean hasUpperCase = false;
			for (int i=0; i<word.length();i++) {
				char c = word.charAt(i);
				if (Character.isUpperCase(c)) {
					hasUpperCase = true;
				}
				if (Character.isLowerCase(c)) {
					hasLowerCase = true;
					break;
				}
			}
			
			if (hasUpperCase&&!hasLowerCase) {
				upperCaseSequence.add(token);
			} else if (!hasLowerCase) {
				// do nothing, might be punctuation or number in middle of upper case sequence
			} else {
				if (upperCaseSequence.size()>1) {
					this.checkSequence(upperCaseSequence);
				}
				upperCaseSequence.clear();
			}
		} // next token
		if (upperCaseSequence.size()>1) {
			this.checkSequence(upperCaseSequence);
		}
	}

	void checkSequence(List<Token> upperCaseSequence) {
		for (Token token : upperCaseSequence) {
			token.setText(getKnownWord(this.talismaneSession, token.getText()));
		}
	}

	public TalismaneSession getTalismaneSession() {
		return talismaneSession;
	}

	public void setTalismaneSession(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
	}

	public static String getKnownWord(TalismaneSession talismaneSession, String word) {
		String knownWord = word;
		boolean foundWord = false;
		List<String> possibleWords = getPossibleWords(talismaneSession, word);
		for (String possibleWord : possibleWords) {
			Set<PosTag> posTags = talismaneSession.getMergedLexicon().findPossiblePosTags(possibleWord);
			if (posTags.size()>0) {
				knownWord = possibleWord;
				foundWord = true;
				break;
			}
		}
		if (!foundWord) {
			if (word.length()>0) {
				knownWord = word.substring(0,1) + word.substring(1).toLowerCase(talismaneSession.getLocale());
			}
		}
		return knownWord;
	}
	
	public static List<String> getPossibleWords(TalismaneSession talismaneSession, String word) {
		List<char[]> possibleChars = new ArrayList<char[]>();
		for (int i = 0; i<word.length();i++) {
			char c = word.charAt(i);
			char[] lowerCaseChars = talismaneSession.getLinguisticRules().getLowercaseOptionsWithDiacritics(c);
			possibleChars.add(lowerCaseChars);
		}
		
		List<String> possibleWords = new ArrayList<String>();
		possibleWords.add("");
		for (int i=0;i<word.length();i++) {
			char[] lowerCaseChars = possibleChars.get(i);
			if (possibleWords.size()>=MAX_WORD_ATTEMPTS) {
				lowerCaseChars = new char[] { possibleChars.get(i)[0] };
			}
			List<String> newPossibleWords = new ArrayList<String>();
			for (String possibleWord : possibleWords) {
				for (char c : lowerCaseChars) {
					newPossibleWords.add(possibleWord + c);
				}
			}
			possibleWords = newPossibleWords;
		}
		return possibleWords;
	}
}
