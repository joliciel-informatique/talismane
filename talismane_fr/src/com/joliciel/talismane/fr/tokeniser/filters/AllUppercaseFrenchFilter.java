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
package com.joliciel.talismane.fr.tokeniser.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;

/**
 * Looks for words in all upper-case, and transforms them into lower-case.
 * @author Assaf Urieli
 *
 */
public class AllUppercaseFrenchFilter implements TokenSequenceFilter {
	private static final String[] upperCaseEndWordArray = new String[] { "SARL", "SA", "EURL" };
	
	/**
	 * No more than 1000 different lowercase words to check per single uppercase word.
	 */
	private static final int MAX_WORD_ATTEMPTS = 1000;
	
	private Set<String> upperCaseEndWords;
	
	public AllUppercaseFrenchFilter() {
		super();
	}

	@Override
	public void apply(TokenSequence tokenSequence) {
		for (Token token : tokenSequence) {
			String word = token.getText();
			
			if (word.length()<=1)
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
				this.checkSequence(token);
			}
		} // next token
	}

	void checkSequence(Token token) {
		if (this.getUpperCaseEndWords().contains(token.getText()))
			return;
		
		List<String> possibleWords = getPossibleWords(token.getText());
		for (String possibleWord : possibleWords) {
			Set<PosTag> posTags = TalismaneSession.getLexicon().findPossiblePosTags(possibleWord);
			if (posTags.size()>0) {
				token.setText(possibleWord);
				break;
			}
		}
	}

	
	static List<String> getPossibleWords(String word) {
		List<char[]> possibleChars = new ArrayList<char[]>();
		for (int i = 0; i<word.length();i++) {
			char c = word.charAt(i);
			char[] lowerCaseChars = null;
			switch (c) {
				case 'E':
					lowerCaseChars = new char[] {'e', 'é', 'ê', 'è', 'ë'};
					break;
				case 'A':
					lowerCaseChars = new char[] {'à', 'a', 'â'};
					break;
				case 'O':
					lowerCaseChars  = new char[] {'o', 'ô'};
					break;
				case 'I':
					lowerCaseChars  = new char[] {'i', 'î', 'ï'};
					break;
				case 'U':
					lowerCaseChars = new char[] {'u', 'ù', 'û'};
					break;
				case 'C':
					lowerCaseChars = new char[] {'ç', 'c'};
					break;
				default:
					lowerCaseChars = new char[] {Character.toLowerCase(c)};
					break;
			}
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

	public Set<String> getUpperCaseEndWords() {
		if (upperCaseEndWords==null) {
			upperCaseEndWords = new TreeSet<String>();
			for (String validUpperCaseWord : upperCaseEndWordArray)
				upperCaseEndWords.add(validUpperCaseWord);
		}
		return upperCaseEndWords;
	}

	public void setUpperCaseEndWords(Set<String> upperCaseEndWords) {
		this.upperCaseEndWords = upperCaseEndWords;
	}

}
