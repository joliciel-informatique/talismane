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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;

/**
 * A token sequence that has been pre-tokenised by another source (manual
 * annotation, external module, etc.).
 * 
 * @author Assaf Urieli
 *
 */
public class PretokenisedSequence extends TokenSequence {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(PretokenisedSequence.class);
	private static final long serialVersionUID = 1L;

	PretokenisedSequence(PretokenisedSequence sequenceToClone) {
		super(sequenceToClone);
	}

	public PretokenisedSequence(Sentence sentence, TalismaneSession talismaneSession) {
		super(sentence, talismaneSession);
	}

	@Override
	public Token addToken(int start, int end) {
		throw new TalismaneException("Cannot add tokens by index");
	}

	/**
	 * Called when reconstructing a sentence from a previously annotated corpus,
	 * adding the next string.
	 */
	public Token addToken(String string) {
		String text = this.getSentence().getText();

		int start = 0;
		if (this.size() > 0)
			start = this.get(this.size() - 1).getEndIndex();

		for (; start < text.length(); start++) {
			char c = text.charAt(start);
			if (!Character.isWhitespace(c)) {
				break;
			}
		}
		int end = start + string.length();

		if (!text.substring(start, end).equals(string)) {
			throw new TalismaneException("Add token failed: Expected '" + string + "' but was '" + text.substring(start, end) + "' in sentence: " + text);
		}

		return this.addToken(start, end);
	}

	@Override
	public TokenSequence cloneTokenSequence() {
		PretokenisedSequence tokenSequence = new PretokenisedSequence(this);
		return tokenSequence;
	}
}
