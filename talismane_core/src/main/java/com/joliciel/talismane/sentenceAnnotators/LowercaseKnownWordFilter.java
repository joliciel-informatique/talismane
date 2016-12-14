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
package com.joliciel.talismane.sentenceAnnotators;

import java.util.List;
import java.util.Set;

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneSession;

/**
 * Transforms a word with at least an initial uppercase letter into lower-case
 * as long as it is a known word in the lexicon.
 * 
 * @author Assaf Urieli
 *
 */
public class LowercaseKnownWordFilter implements TextReplacer, NeedsTalismaneSession {
	private TalismaneSession session;

	public LowercaseKnownWordFilter() {
		super();
	}

	@Override
	public void replace(List<String> tokens) {
		for (int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);
			if (token.length() > 0 && Character.isUpperCase(token.charAt(0))) {
				Set<String> possibleWords = session.getDiacriticizer().diacriticize(token);
				if (possibleWords.size() > 0) {
					tokens.set(i, possibleWords.iterator().next());
				}
			}
		}
	}

	@Override
	public TalismaneSession getTalismaneSession() {
		return session;
	}

	@Override
	public void setTalismaneSession(TalismaneSession talismaneSession) {
		this.session = talismaneSession;
	}

}
