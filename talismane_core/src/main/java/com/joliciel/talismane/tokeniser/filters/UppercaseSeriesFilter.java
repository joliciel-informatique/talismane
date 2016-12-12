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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.Diacriticizer;
import com.joliciel.talismane.tokeniser.Tokeniser;

/**
 * Looks for a series of 2+ words in all upper-case, and transforms them into
 * lower-case. Unknown words will be given an initial upper case.
 * 
 * @author Assaf Urieli
 *
 */
public class UppercaseSeriesFilter implements TextReplacer, NeedsTalismaneSession {
	private static final Logger LOG = LoggerFactory.getLogger(UppercaseSeriesFilter.class);
	private TalismaneSession session;

	/**
	 * Maximum number of spaces in a single word.
	 */
	private static final int maxSpaces = 2;

	public UppercaseSeriesFilter() {
		super();
	}

	@Override
	public void replace(List<String> tokens) {
		List<String> upperCaseSequence = new ArrayList<>();
		int startIndex = 0;
		int numWords = 0;
		// add a lowercase word at the end
		tokens.add("a");
		for (int j = 0; j < tokens.size(); j++) {

			String token = tokens.get(j);

			if (token.length() == 0)
				continue;

			boolean hasLowerCase = false;
			boolean hasUpperCase = false;
			for (int i = 0; i < token.length(); i++) {
				char c = token.charAt(i);
				if (Character.isUpperCase(c)) {
					hasUpperCase = true;
				}
				if (Character.isLowerCase(c)) {
					hasLowerCase = true;
					break;
				}
			}

			if (hasUpperCase && !hasLowerCase) {
				if (upperCaseSequence.size() == 0)
					startIndex = j;
				upperCaseSequence.add(token);
				numWords++;
			} else if (!hasLowerCase) {
				// might be punctuation or space or number in the middle of
				// uppercase sequence
				if (upperCaseSequence.size() > 0)
					upperCaseSequence.add(token);
			} else {
				if (numWords > 1) {
					for (int k = 0; k < upperCaseSequence.size(); k++) {
						String startWord = upperCaseSequence.get(k);
						if (startWord.length() == 1 && Character.isWhitespace(startWord.charAt(0))) {
							continue;
						}
						StringBuilder wordBuilder = new StringBuilder();
						boolean foundWord = false;
						int numSpaces = 0;
						for (int l = k; l < upperCaseSequence.size(); l++) {
							String part = upperCaseSequence.get(l);
							// don't go beyond white space
							if (part.length() == 1 && Character.isWhitespace(part.charAt(0)))
								numSpaces++;
							if (numSpaces > maxSpaces)
								break;

							wordBuilder.append(part);
							String word = wordBuilder.toString();
							String knownWord = null;
							Diacriticizer diacriticizer = session.getDiacriticizer();
							Set<String> lowercaseForms = diacriticizer.diacriticize(word);
							if (lowercaseForms.size() > 0) {
								knownWord = lowercaseForms.iterator().next();
								List<String> knownTokens = Tokeniser.bruteForceTokenise(knownWord, session);
								if (knownTokens.size() == l - k + 1) {
									// same number of tokens, we can update the
									// existing word
									for (int m = 0; m < knownTokens.size(); m++) {
										tokens.set(startIndex + k + m, knownTokens.get(m));
									}
									foundWord = true;
									// skip the words that were lowercased
									k += knownTokens.size() - 1;
									break;
								} else {
									LOG.debug("Different number of tokens: |" + knownTokens.toString() + "| and |" + word + "|");
								}
							}
						}

						if (!foundWord) {
							if (startWord.length() > 0) {
								String newWord = startWord.substring(0, 1) + startWord.substring(1).toLowerCase(session.getLocale());
								tokens.set(startIndex + k, newWord);
							}
						}
					} // next word in series
				} // have series
				upperCaseSequence.clear();
				numWords = 0;
			}
		} // next token
			// remove the lowercase word
		tokens.remove(tokens.size() - 1);
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
