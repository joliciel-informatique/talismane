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

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import com.joliciel.talismane.TalismaneSession;
import com.typesafe.config.Config;

/**
 * A default corpus reader which assumes one sentence per line.
 * 
 * @author Assaf Urieli
 *
 */
public class SentencePerLineCorpusReader extends SentenceDetectorAnnotatedCorpusReader {
	private Scanner scanner;
	private int maxSentenceCount = 0;
	private int startSentence = 0;
	private int sentenceCount = 0;
	private int includeIndex = -1;
	private int excludeIndex = -1;
	private int crossValidationSize = 0;
	String sentence = null;

	public SentencePerLineCorpusReader(Reader reader, Config config, TalismaneSession session) {
		super(reader, config, session);
		scanner = new Scanner(reader);
	}

	@Override
	public boolean hasNextSentence() {
		if (maxSentenceCount > 0 && sentenceCount >= maxSentenceCount) {
			// we've reached the end, do nothing
		} else {

			while (sentence == null) {
				if (!scanner.hasNextLine()) {
					break;
				}

				sentence = scanner.nextLine().trim();
				if (sentence.length() == 0) {
					sentence = null;
					continue;
				}

				boolean includeMe = true;

				// check cross-validation
				if (crossValidationSize > 0) {
					if (includeIndex >= 0) {
						if (sentenceCount % crossValidationSize != includeIndex) {
							includeMe = false;
						}
					} else if (excludeIndex >= 0) {
						if (sentenceCount % crossValidationSize == excludeIndex) {
							includeMe = false;
						}
					}
				}

				if (startSentence > sentenceCount) {
					includeMe = false;
				}

				sentenceCount++;

				if (!includeMe) {
					sentence = null;
					continue;
				}

			}
		}
		return sentence != null;
	}

	@Override
	public String nextSentence() {
		String currentSentence = sentence;
		sentence = null;
		return currentSentence;
	}

	@Override
	public Map<String, String> getCharacteristics() {
		Map<String, String> attributes = new LinkedHashMap<String, String>();

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

	@Override
	public int getMaxSentenceCount() {
		return maxSentenceCount;
	}

	@Override
	public void setMaxSentenceCount(int maxSentenceCount) {
		this.maxSentenceCount = maxSentenceCount;
	}

	@Override
	public int getIncludeIndex() {
		return includeIndex;
	}

	@Override
	public void setIncludeIndex(int includeIndex) {
		this.includeIndex = includeIndex;
	}

	@Override
	public int getExcludeIndex() {
		return excludeIndex;
	}

	@Override
	public void setExcludeIndex(int excludeIndex) {
		this.excludeIndex = excludeIndex;
	}

	@Override
	public int getCrossValidationSize() {
		return crossValidationSize;
	}

	@Override
	public void setCrossValidationSize(int crossValidationSize) {
		this.crossValidationSize = crossValidationSize;
	}

	@Override
	public int getStartSentence() {
		return startSentence;
	}

	@Override
	public void setStartSentence(int startSentence) {
		this.startSentence = startSentence;
	}

}
