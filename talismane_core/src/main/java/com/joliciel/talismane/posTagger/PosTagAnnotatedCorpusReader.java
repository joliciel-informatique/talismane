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

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
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

	final List<PosTagSequenceFilter> posTagSequenceFilters = new ArrayList<>();

	/**
	 * Is there another sentence to be read?
	 */
	public abstract boolean hasNextPosTagSequence();

	/**
	 * Read the list of tagged tokens from next sentence from the training
	 * corpus.
	 */
	public abstract PosTagSequence nextPosTagSequence();

	public void addPosTagSequenceFilter(PosTagSequenceFilter posTagSequenceFilter) {
		this.posTagSequenceFilters.add(posTagSequenceFilter);
	}
}
