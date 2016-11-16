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

import com.joliciel.talismane.AnnotatedCorpusReader;
import com.joliciel.talismane.TalismaneSession;
import com.typesafe.config.Config;

/**
 * An interface for reading sentence splits from a training corpus.
 * 
 * @author Assaf Urieli
 *
 */
public abstract class SentenceDetectorAnnotatedCorpusReader implements AnnotatedCorpusReader {
	public SentenceDetectorAnnotatedCorpusReader(Reader reader, Config config, TalismaneSession session) {
	}

	/**
	 * Is there another sentence to be read?
	 */
	public abstract boolean hasNextSentence();

	/**
	 * Reads the next sentence from the corpus.
	 */
	public abstract String nextSentence();

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
	 * @throws ClassNotFoundException
	 *             if the corpus-reader class was not found
	 * @throws ReflectiveOperationException
	 *             if the corpus-reader class could not be instantiated
	 */
	public static SentenceDetectorAnnotatedCorpusReader getCorpusReader(Reader reader, Config config, TalismaneSession session)
			throws IOException, ClassNotFoundException, ReflectiveOperationException {
		String className = config.getString("corpus-reader");

		@SuppressWarnings("unchecked")
		Class<? extends SentenceDetectorAnnotatedCorpusReader> clazz = (Class<? extends SentenceDetectorAnnotatedCorpusReader>) Class.forName(className);
		Constructor<? extends SentenceDetectorAnnotatedCorpusReader> cons = clazz.getConstructor(Reader.class, Config.class, TalismaneSession.class);

		SentenceDetectorAnnotatedCorpusReader corpusReader = cons.newInstance(reader, config, session);

		corpusReader.setMaxSentenceCount(config.getInt("sentence-count"));
		corpusReader.setStartSentence(config.getInt("start-sentence"));
		int crossValidationSize = config.getInt("cross-validation.fold-count");
		if (crossValidationSize > 0)
			corpusReader.setCrossValidationSize(crossValidationSize);
		int includeIndex = -1;
		if (config.hasPath("cross-validation.include-index"))
			includeIndex = config.getInt("cross-validation.include-index");
		if (includeIndex >= 0)
			corpusReader.setIncludeIndex(includeIndex);
		int excludeIndex = -1;
		if (config.hasPath("cross-validation.exclude-index"))
			excludeIndex = config.getInt("cross-validation.exclude-index");
		if (excludeIndex >= 0)
			corpusReader.setExcludeIndex(excludeIndex);

		return corpusReader;
	}
}
