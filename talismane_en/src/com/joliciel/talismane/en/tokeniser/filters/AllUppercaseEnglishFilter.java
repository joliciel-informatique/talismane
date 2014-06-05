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
package com.joliciel.talismane.en.tokeniser.filters;

import java.util.Set;
import java.util.TreeSet;

import com.joliciel.talismane.NeedsTalismaneSession;
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
public class AllUppercaseEnglishFilter implements TokenSequenceFilter, NeedsTalismaneSession {
	private static final String[] upperCaseEndWordArray = new String[] { "USA", "UK" };
	
	private Set<String> upperCaseEndWords;
	
	TalismaneSession talismaneSession;
	
	public AllUppercaseEnglishFilter() {
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
		
		String word = token.getText().toLowerCase();
		Set<PosTag> posTags = talismaneSession.getLexicon().findPossiblePosTags(word);
		if (posTags.size()>0) {
			token.setText(word);
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

	public TalismaneSession getTalismaneSession() {
		return talismaneSession;
	}

	public void setTalismaneSession(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
	}
}
