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

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.utils.io.CurrentFileObserver;
import com.joliciel.talismane.utils.io.CurrentFileProvider;
import com.typesafe.config.Config;

/**
 * An interface for reading tokenized sentences from a corpus.
 * 
 * @author Assaf Urieli
 *
 */
public interface TokeniserAnnotatedCorpusReader extends SentenceDetectorAnnotatedCorpusReader {
	public static final Logger LOG = LoggerFactory.getLogger(TokeniserAnnotatedCorpusReader.class);

	/***
	 * Reads the next token sequence from the corpus.
	 * 
	 * @throws TalismaneException
	 *             if impossible to read next sequence for logical reasons
	 */
	public abstract TokenSequence nextTokenSequence() throws TalismaneException;

	/**
	 * Builds an annotated corpus reader for a particular Reader and Config,
	 * where the config is the local namespace. For configuration example, see
	 * talismane.core.tokeniser.input in reference.conf.
	 * 
	 * @param config
	 *            the local configuration section from which we're building a
	 *            reader
	 * @throws IOException
	 *             problem reading the files referred in the configuration
	 * @throws ReflectiveOperationException
	 *             if the corpus-reader class could not be instantiated
	 */
	public static TokeniserAnnotatedCorpusReader getCorpusReader(Reader reader, Config config, TalismaneSession session)
			throws IOException, ReflectiveOperationException {
		String className = config.getString("corpus-reader");

		@SuppressWarnings("unchecked")
		Class<? extends TokeniserAnnotatedCorpusReader> clazz = (Class<? extends TokeniserAnnotatedCorpusReader>) Class.forName(className);
		Constructor<? extends TokeniserAnnotatedCorpusReader> cons = clazz.getConstructor(Reader.class, Config.class, TalismaneSession.class);

		TokeniserAnnotatedCorpusReader corpusReader = cons.newInstance(reader, config, session);
		if (reader instanceof CurrentFileProvider && corpusReader instanceof CurrentFileObserver) {
			((CurrentFileProvider) reader).addCurrentFileObserver((CurrentFileObserver) corpusReader);
		}
		return corpusReader;
	}
}
