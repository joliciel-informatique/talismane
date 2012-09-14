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
package com.joliciel.frenchTreebank.export;

import java.util.HashMap;
import java.util.Map;
import com.joliciel.frenchTreebank.Sentence;
import com.joliciel.frenchTreebank.TreebankReader;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;

class FrenchTreebankSentenceReader implements SentenceDetectorAnnotatedCorpusReader {
	TreebankReader treebankReader;
	
	Sentence currentSentence = null;
	boolean newParagraph = false;
	int lastTextItemId = 0;

	public FrenchTreebankSentenceReader(TreebankReader treebankReader) {
		this.treebankReader = treebankReader;
	}
	
	@Override
	public boolean hasNextSentence() {
		return treebankReader.hasNextSentence();
	}

	@Override
	public String nextSentence() {
		currentSentence = treebankReader.nextSentence();
		if (currentSentence.getTextItemId()!=lastTextItemId) {
			newParagraph = true;
			lastTextItemId = currentSentence.getTextItemId();
		} else {
			newParagraph = false;
		}
		
		String text = currentSentence.getText();
		return text;
	}

	@Override
	public Map<String, String> getCharacteristics() {
		Map<String,String> characteristics = new HashMap<String, String>();

		characteristics.put("treebankReader", treebankReader.getClass().getSimpleName());
		characteristics.putAll(this.treebankReader.getCharacteristics());
	
		return characteristics;
	}

	public boolean isNewParagraph() {
		return newParagraph;
	}

}
