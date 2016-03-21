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

import java.util.Set;

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;

/**
 * Transforms a word with at least an initial uppercase letter into lower-case as long as it is a known word in the lexicon.
 * @author Assaf Urieli
 *
 */
public class LowercaseKnownWordFilter implements TokenSequenceFilter, NeedsTalismaneSession {
	private TalismaneSession talismaneSession;
	
	public LowercaseKnownWordFilter() {
		super();
	}

	@Override
	public void apply(TokenSequence tokenSequence) {
		for (Token token : tokenSequence) {
			String word = token.getText();
			if (word.length()>0 && Character.isUpperCase(word.charAt(0))) {
				Set<String> possibleWords = talismaneSession.getDiacriticizer().diacriticize(word);
				if (possibleWords.size()>0)
					token.setText(possibleWords.iterator().next());
			}
		} // next token
	}

	public TalismaneSession getTalismaneSession() {
		return talismaneSession;
	}

	public void setTalismaneSession(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
	}
	
	
}
