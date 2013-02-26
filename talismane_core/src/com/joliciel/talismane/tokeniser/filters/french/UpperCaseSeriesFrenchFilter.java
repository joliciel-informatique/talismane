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
package com.joliciel.talismane.tokeniser.filters.french;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;

/**
 * Looks for a series of 2+ words in all upper-case, and transforms them into lower-case.
 * @author Assaf Urieli
 *
 */
public class UpperCaseSeriesFrenchFilter implements TokenSequenceFilter {

	private static final String[] upperCaseEndWordArray = new String[] { "SARL", "SA", "EURL" };
	
	private Set<String> upperCaseEndWords;
	
	public UpperCaseSeriesFrenchFilter() {
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
		String lastWord = upperCaseSequence.get(upperCaseSequence.size()-1).getText();
		if (this.getUpperCaseEndWords().contains(lastWord))
			return;
		
		for (Token token : upperCaseSequence) {
			boolean foundWord = false;
			List<String> possibleWords = AllUppercaseFrenchFilter.getPossibleWords(token.getText());
			for (String possibleWord : possibleWords) {
				Set<PosTag> posTags = TalismaneSession.getLexicon().findPossiblePosTags(possibleWord);
				if (posTags.size()>0) {
					token.setText(possibleWord);
					foundWord = true;
					break;
				}
			}
			if (!foundWord) {
				if (token.getText().length()>0) {
					String text = token.getText().toLowerCase(Locale.FRENCH);
					text = token.getText().substring(0,1) + text.substring(1);
					token.setText(text);
				}
			}
		}
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
