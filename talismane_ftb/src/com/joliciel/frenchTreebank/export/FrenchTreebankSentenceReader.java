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

import java.util.LinkedHashMap;
import java.util.Map;
import com.joliciel.frenchTreebank.Sentence;
import com.joliciel.frenchTreebank.TreebankReader;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;

class FrenchTreebankSentenceReader implements SentenceDetectorAnnotatedCorpusReader {
	TreebankReader treebankReader;
	
	Sentence sentence = null;
	boolean newParagraph = false;
	int lastTextItemId = 0;
	
	private int maxSentenceCount = 0;
	private int sentenceCount = 0;
	private int includeIndex = -1;
	private int excludeIndex = -1;
	private int crossValidationSize = 0;

	public FrenchTreebankSentenceReader(TreebankReader treebankReader) {
		this.treebankReader = treebankReader;
	}
	
	@Override
	public boolean hasNextSentence() {
		if (maxSentenceCount>0 && sentenceCount>=maxSentenceCount) {
			// we've reached the end, do nothing
		} else {

			while (sentence==null) {
				if (!treebankReader.hasNextSentence()) {
					break;
				}
				
				sentence = treebankReader.nextSentence();
				if (sentence.getText().trim().length()==0) {
					sentence = null;
					continue;
				}
				
				sentenceCount++;
				
				// check cross-validation
				if (crossValidationSize>0) {
					boolean includeMe = true;
					if (includeIndex>=0) {
						if (sentenceCount % crossValidationSize != includeIndex) {
							includeMe = false;
						}
					} else if (excludeIndex>=0) {
						if (sentenceCount % crossValidationSize == excludeIndex) {
							includeMe = false;
						}
					}
					if (!includeMe) {
						sentence = null;
						continue;
					}
				}
				
				if (sentence.getTextItemId()!=lastTextItemId) {
					newParagraph = true;
					lastTextItemId = sentence.getTextItemId();
				} else {
					newParagraph = false;
				}
			}
		}
		return sentence !=null;
	}

	@Override
	public String nextSentence() {
		String text = sentence.getText();
		sentence = null;
		return text;
	}

	@Override
	public Map<String, String> getCharacteristics() {
		Map<String,String> characteristics = new LinkedHashMap<String, String>();

		characteristics.put("treebankReader", treebankReader.getClass().getSimpleName());
		characteristics.putAll(this.treebankReader.getCharacteristics());
	
		characteristics.put("maxSentenceCount", "" + this.maxSentenceCount);
		characteristics.put("crossValidationSize", "" + this.crossValidationSize);
		characteristics.put("includeIndex", "" + this.includeIndex);
		characteristics.put("excludeIndex", "" + this.excludeIndex);

		return characteristics;
	}

	public boolean isNewParagraph() {
		return newParagraph;
	}

	public int getMaxSentenceCount() {
		return maxSentenceCount;
	}

	public void setMaxSentenceCount(int maxSentenceCount) {
		this.maxSentenceCount = maxSentenceCount;
	}

	public int getIncludeIndex() {
		return includeIndex;
	}

	public void setIncludeIndex(int includeIndex) {
		this.includeIndex = includeIndex;
	}

	public int getExcludeIndex() {
		return excludeIndex;
	}

	public void setExcludeIndex(int excludeIndex) {
		this.excludeIndex = excludeIndex;
	}

	public int getCrossValidationSize() {
		return crossValidationSize;
	}

	public void setCrossValidationSize(int crossValidationSize) {
		this.crossValidationSize = crossValidationSize;
	}

	
}
