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

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;

import com.joliciel.talismane.AnnotatedCorpusReader;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.typesafe.config.Config;

/**
 * An interface for reading sentence splits from a training corpus.
 * 
 * @author Assaf Urieli
 *
 */
public abstract class SentenceDetectorAnnotatedCorpusReader implements AnnotatedCorpusReader {

	private final int maxSentenceCount;
	private final int startSentence;
	private final int crossValidationSize;
	private final int includeIndex;
	private final int excludeIndex;

	/**
	 * Add attributes as specified in the config to the corpus reader.
	 * Recognises the attributes:
	 * <ul>
	 * <li>sentence-count</li>
	 * <li>start-sentence</li>
	 * <li>cross-validation.fold-count</li>
	 * <li>cross-validation.include-index</li>
	 * <li>cross-validation.exclude-index</li>
	 * </ul>
	 * 
	 * @param config
	 *            the local config for this corpus reader (local namespace)
	 */
	public SentenceDetectorAnnotatedCorpusReader(Reader reader, Config config, TalismaneSession session) {
		this.maxSentenceCount = config.getInt("sentence-count");
		this.startSentence = config.getInt("start-sentence");
		this.crossValidationSize = config.getInt("cross-validation.fold-count");
		this.includeIndex = config.getInt("cross-validation.include-index");
		this.excludeIndex = config.getInt("cross-validation.exclude-index");
	}

	@Override
	public int getMaxSentenceCount() {
		return maxSentenceCount;
	}

	@Override
	public int getStartSentence() {
		return startSentence;
	}

	@Override
	public int getCrossValidationSize() {
		return crossValidationSize;
	}

	@Override
	public int getIncludeIndex() {
		return includeIndex;
	}

	@Override
	public int getExcludeIndex() {
		return excludeIndex;
	}

	@Override
	public Map<String, String> getCharacteristics() {
		Map<String, String> attributes = new LinkedHashMap<String, String>();

		attributes.put("maxSentenceCount", "" + this.getMaxSentenceCount());
		attributes.put("startSentence", "" + this.getStartSentence());
		attributes.put("crossValidationSize", "" + this.getCrossValidationSize());
		attributes.put("includeIndex", "" + this.getIncludeIndex());
		attributes.put("excludeIndex", "" + this.getExcludeIndex());

		return attributes;
	}

	/**
	 * Is there another sentence to be read?
	 */
	public abstract boolean hasNextSentence();

	/**
	 * Reads the next sentence from the corpus.
	 */
	public abstract Sentence nextSentence();

	/**
	 * Is the last sentence read the start of a new paragraph?
	 */
	public abstract boolean isNewParagraph();

	/**
	 * Builds an annotated corpus reader for a particular Reader and Config,
	 * where the config is the local namespace. For configuration example, see
	 * talismane.core.sentence-detector.input in reference.conf.
	 * 
	 * @param config
	 *            the local configuration section from which we're building a
	 *            reader
	 * @throws IOException
	 *             problem reading the files referred in the configuration
	 * @throws ReflectiveOperationException
	 *             if the corpus-reader class could not be instantiated
	 */
	public static SentenceDetectorAnnotatedCorpusReader getCorpusReader(Reader reader, Config config, TalismaneSession session)
			throws IOException, ReflectiveOperationException {
		String className = config.getString("corpus-reader");

		@SuppressWarnings("unchecked")
		Class<? extends SentenceDetectorAnnotatedCorpusReader> clazz = (Class<? extends SentenceDetectorAnnotatedCorpusReader>) Class.forName(className);
		Constructor<? extends SentenceDetectorAnnotatedCorpusReader> cons = clazz.getConstructor(Reader.class, Config.class, TalismaneSession.class);

		SentenceDetectorAnnotatedCorpusReader corpusReader = cons.newInstance(reader, config, session);

		return corpusReader;
	}
}
