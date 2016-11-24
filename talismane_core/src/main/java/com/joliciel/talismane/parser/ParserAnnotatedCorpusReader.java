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
package com.joliciel.talismane.parser;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalEntryReader;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.typesafe.config.Config;

/**
 * An interface for reading ParseConfigurations from sentences in a corpus.
 * 
 * @author Assaf Urieli
 *
 */
public abstract class ParserAnnotatedCorpusReader extends PosTagAnnotatedCorpusReader {
	public ParserAnnotatedCorpusReader(Reader reader, Config config, TalismaneSession session) {
		super(reader, config, session);
	}

	/**
	 * Is there another sentence to be read?
	 */
	public abstract boolean hasNextConfiguration();

	/**
	 * Read the ParseConfiguration from the next sentence in the training
	 * corpus.
	 */
	public abstract ParseConfiguration nextConfiguration();

	/**
	 * If provided, will read a lexical entry for each pos-tagged token.
	 */
	public abstract LexicalEntryReader getLexicalEntryReader();

	public abstract void setLexicalEntryReader(LexicalEntryReader lexicalEntryReader);

	/**
	 * Take this reader back to its initial position.
	 */
	public abstract void rewind();

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
	public static ParserAnnotatedCorpusReader getCorpusReader(Reader reader, Config config, TalismaneSession session)
			throws IOException, ReflectiveOperationException {
		String className = config.getString("corpus-reader");

		@SuppressWarnings("unchecked")
		Class<? extends ParserAnnotatedCorpusReader> clazz = (Class<? extends ParserAnnotatedCorpusReader>) Class.forName(className);
		Constructor<? extends ParserAnnotatedCorpusReader> cons = clazz.getConstructor(Reader.class, Config.class, TalismaneSession.class);

		ParserAnnotatedCorpusReader corpusReader = cons.newInstance(reader, config, session);
		return corpusReader;
	}
}
