///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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

import java.util.ArrayList;
import java.util.List;

class TokenComparatorImpl implements TokenComparator {
	private List<TokenEvaluationObserver> observers = new ArrayList<TokenEvaluationObserver>();
	private int sentenceCount;
	
	@Override
	public void evaluate(TokeniserAnnotatedCorpusReader referenceCorpusReader,
			TokeniserAnnotatedCorpusReader evaluationCorpusReader) {
		// TODO: problem - we don't have atomic sequences here
		// this can only compare a generated tokenSequence with another generated token sequence
		// hence, it cannot use TokenEvaluationObserver as it's currently interfaced
		// also, we have no access to the original patterns that surrounded each decision
	}

	@Override
	public void addObserver(TokenEvaluationObserver observer) {
		this.observers.add(observer);
	}

	public int getSentenceCount() {
		return sentenceCount;
	}

	public void setSentenceCount(int sentenceCount) {
		this.sentenceCount = sentenceCount;
	}
	

}
