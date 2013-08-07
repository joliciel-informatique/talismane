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
package com.joliciel.talismane.sentenceDetector;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * A default corpus reader which assumes one sentence per line.
 * @author Assaf Urieli
 *
 */
class SentencePerLineCorpusReader implements SentenceDetectorAnnotatedCorpusReader {
	private Scanner scanner;
	private int maxSentenceCount = 0;
	private int sentenceCount = 0;
	private int includeIndex = -1;
	private int excludeIndex = -1;
	private int crossValidationSize = 0;
	String sentence = null;
	
	public SentencePerLineCorpusReader(Reader reader) {
		scanner = new Scanner(reader);
	}
	
	@Override
	public boolean hasNextSentence() {
		if (maxSentenceCount>0 && sentenceCount>=maxSentenceCount) {
			// we've reached the end, do nothing
		} else {

			while (sentence==null) {
				if (!scanner.hasNextLine()) {
					break;
				}
				
				sentence = scanner.nextLine().trim();
				if (sentence.length()==0) {
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
			}
		}
		return sentence !=null;
	}

	@Override
	public String nextSentence() {
		String currentSentence = sentence;
		sentence = null;
		return currentSentence;
	}

	@Override
	public Map<String, String> getCharacteristics() {
		Map<String,String> attributes = new LinkedHashMap<String, String>();

		attributes.put("maxSentenceCount", "" + this.maxSentenceCount);
		attributes.put("crossValidationSize", "" + this.crossValidationSize);
		attributes.put("includeIndex", "" + this.includeIndex);
		attributes.put("excludeIndex", "" + this.excludeIndex);
		
		return attributes;
	}

	@Override
	public boolean isNewParagraph() {
		return false;
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
