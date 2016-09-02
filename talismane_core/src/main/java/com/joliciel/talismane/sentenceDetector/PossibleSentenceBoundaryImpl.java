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
package com.joliciel.talismane.sentenceDetector;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.utils.StringUtils;

class PossibleSentenceBoundaryImpl implements PossibleSentenceBoundary {
	private static final int NUM_CHARS = 30;
	private final String text;
	private final int index;
	private TokenSequence tokenSequence;
	private String string;
	private int tokenIndex = -1;

	private final TalismaneSession talismaneSession;

	public PossibleSentenceBoundaryImpl(String text, int index, TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
		this.text = text;
		this.index = index;
	}

	@Override
	public String getText() {
		return text;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public TokenSequence getTokenSequence() {
		if (tokenSequence == null) {
			Sentence sentence = new Sentence(text, talismaneSession);
			tokenSequence = new TokenSequence(sentence, Tokeniser.SEPARATORS, talismaneSession);
		}
		return tokenSequence;
	}

	@Override
	public String getBoundaryString() {
		return "" + this.text.charAt(index);
	}

	@Override
	public int getTokenIndexWithWhitespace() {
		if (tokenIndex < 0) {
			for (Token token : this.getTokenSequence().listWithWhiteSpace()) {
				if (token.getStartIndex() >= index) {
					tokenIndex = token.getIndexWithWhiteSpace();
					break;
				}
			}
		}
		return tokenIndex;
	}

	@Override
	public String toString() {
		if (string == null) {
			int start1 = index - NUM_CHARS;
			int end1 = index + NUM_CHARS;

			if (start1 < 0)
				start1 = 0;
			String startString = text.substring(start1, index);
			startString = StringUtils.padLeft(startString, NUM_CHARS);

			String middleString = "" + text.charAt(index);
			if (end1 >= text.length())
				end1 = text.length() - 1;
			String endString = "";
			if (end1 >= 0 && index + 1 < text.length())
				endString = text.substring(index + 1, end1);

			string = startString + "[" + middleString + "]" + endString;
			string = string.replace('\n', '¶');
		}
		return string;
	}
}
