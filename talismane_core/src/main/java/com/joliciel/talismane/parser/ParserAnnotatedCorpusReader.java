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

import java.io.Reader;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalEntryReader;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
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

	@Override
	public abstract void addPosTagSequenceFilter(PosTagSequenceFilter posTagSequenceFilter);

	/**
	 * If provided, will read a lexical entry for each pos-tagged token.
	 */
	public abstract LexicalEntryReader getLexicalEntryReader();

	public abstract void setLexicalEntryReader(LexicalEntryReader lexicalEntryReader);

	/**
	 * Take this reader back to its initial position.
	 */
	public abstract void rewind();
}
