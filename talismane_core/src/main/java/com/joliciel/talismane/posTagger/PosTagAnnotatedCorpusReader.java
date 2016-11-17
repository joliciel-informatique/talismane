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
package com.joliciel.talismane.posTagger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilterFactory;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.utils.ConfigUtils;
import com.typesafe.config.Config;

/**
 * An interface for reading tokenized and tagged sentences from a corpus.
 * 
 * @author Assaf Urieli
 *
 */
public abstract class PosTagAnnotatedCorpusReader extends TokeniserAnnotatedCorpusReader {
	public PosTagAnnotatedCorpusReader(Reader reader, Config config, TalismaneSession session) {
		super(reader, config, session);
	}

	private final List<String> posTagSequenceFilterDescriptors = new ArrayList<>();
	private final List<PosTagSequenceFilter> posTagSequenceFilters = new ArrayList<>();

	/**
	 * Is there another sentence to be read?
	 */
	public abstract boolean hasNextPosTagSequence();

	/**
	 * Read the list of tagged tokens from next sentence from the training
	 * corpus.
	 */
	public abstract PosTagSequence nextPosTagSequence();

	public void addPosTagSequenceFilter(PosTagSequenceFilter posTagSequenceFilter, String descriptor) {
		this.posTagSequenceFilters.add(posTagSequenceFilter);
		this.posTagSequenceFilterDescriptors.add(descriptor);
	}

	public List<PosTagSequenceFilter> getPosTagSequenceFilters() {
		return posTagSequenceFilters;
	}

	public List<String> getPosTagSequenceFilterDescriptors() {
		return posTagSequenceFilterDescriptors;
	}

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
	 * @throws ClassNotFoundException
	 *             if the corpus-reader class was not found
	 * @throws ReflectiveOperationException
	 *             if the corpus-reader class could not be instantiated
	 */
	public static PosTagAnnotatedCorpusReader getCorpusReader(Reader reader, Config config, TalismaneSession session)
			throws IOException, ClassNotFoundException, ReflectiveOperationException {
		String className = config.getString("corpus-reader");

		@SuppressWarnings("unchecked")
		Class<? extends PosTagAnnotatedCorpusReader> clazz = (Class<? extends PosTagAnnotatedCorpusReader>) Class.forName(className);
		Constructor<? extends PosTagAnnotatedCorpusReader> cons = clazz.getConstructor(Reader.class, Config.class, TalismaneSession.class);

		PosTagAnnotatedCorpusReader corpusReader = cons.newInstance(reader, config, session);
		PosTagAnnotatedCorpusReader.addAnnotators(corpusReader, config, session);

		return corpusReader;
	}

	/**
	 * Add annotators as specified in the config to the corpus reader.
	 * 
	 * @throws IOException
	 *             problem reading the files referred in the configuration
	 */
	public static void addAnnotators(PosTagAnnotatedCorpusReader corpusReader, Config config, TalismaneSession session) throws IOException {
		TokeniserAnnotatedCorpusReader.addAnnotators(corpusReader, config, session);
		PosTagSequenceFilterFactory factory = new PosTagSequenceFilterFactory();

		String configPath = "pos-tag-sequence-filters";
		List<String> posTagSequenceFilterrPaths = config.getStringList(configPath);
		for (String path : posTagSequenceFilterrPaths) {
			LOG.debug("From: " + path);
			InputStream inputStream = ConfigUtils.getFile(config, configPath, path);
			try (Scanner scanner = new Scanner(inputStream, session.getInputCharset().name())) {
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					LOG.debug(descriptor);
					if (descriptor.length() > 0 && !descriptor.startsWith("#")) {
						PosTagSequenceFilter filter = factory.getPosTagSequenceFilter(descriptor);
						corpusReader.addPosTagSequenceFilter(filter, descriptor);
					}
				}
			}
		}
	}
}
