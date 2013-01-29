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

import java.io.File;
import java.io.Writer;
import java.util.List;

import com.joliciel.frenchTreebank.TreebankReader;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;

public interface TreebankExportService {
	/**
	 * Get a PosTagMapper based on a file with the following layout:<BR/>
	 * Rows starting with a # are ignored.<BR/>
	 * Other rows contained tab-delimited: category code, subcategory code, morphology code, postag code.<BR/>
	 * @param file
	 * @param posTagSet
	 * @return
	 */
	FtbPosTagMapper getFtbPosTagMapper(File file, PosTagSet posTagSet);
	
	/**
	 * Like getPosTagMapper(File) but replaces the file by a list of strings.
	 * @param descriptors
	 * @param posTagSet
	 * @return
	 */
	FtbPosTagMapper getFtbPosTagMapper(List<String> descriptors, PosTagSet posTagSet);
	
	SentenceDetectorAnnotatedCorpusReader getSentenceDetectorAnnotatedCorpusReader(TreebankReader treebankReader);

	TokeniserAnnotatedCorpusReader getTokeniserAnnotatedCorpusReader(TreebankReader treebankReader);
	TokeniserAnnotatedCorpusReader getTokeniserAnnotatedCorpusReader(TreebankReader treebankReader, Writer writer);
	TokeniserAnnotatedCorpusReader getTokeniserAnnotatedCorpusReader(TreebankReader treebankReader, boolean useCompoundPosTags);
	
	PosTagAnnotatedCorpusReader getPosTagAnnotatedCorpusReader(TreebankReader treebankReader, FtbPosTagMapper ftbPosTagMapper);
	PosTagAnnotatedCorpusReader getPosTagAnnotatedCorpusReader(TreebankReader treebankReader, FtbPosTagMapper ftbPosTagMapper, boolean useCompoundPosTags);
}
