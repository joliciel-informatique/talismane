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
package com.joliciel.talismane.fr.ftb.export;

import java.io.File;
import java.io.Writer;
import java.util.List;

import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.fr.ftb.TreebankReader;
import com.joliciel.talismane.fr.ftb.TreebankService;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;

class TreebankExportServiceImpl implements TreebankExportService {
	private TreebankService treebankService;
	private PosTaggerService posTaggerService;
	private TalismaneService talismaneService;

	public TreebankService getTreebankService() {
		return treebankService;
	}

	public void setTreebankService(TreebankService treebankService) {
		this.treebankService = treebankService;
	}

	@Override
	public FtbPosTagMapper getFtbPosTagMapper(File file, PosTagSet posTagSet) {
		FtbPosTagMapperImpl mapper = new FtbPosTagMapperImpl(file, posTagSet);
		return mapper;
	}

	@Override
	public FtbPosTagMapper getFtbPosTagMapper(List<String> descriptors, PosTagSet posTagSet) {
		FtbPosTagMapperImpl mapper = new FtbPosTagMapperImpl(descriptors, posTagSet);
		return mapper;
	}

	@Override
	public SentenceDetectorAnnotatedCorpusReader getSentenceDetectorAnnotatedCorpusReader(TreebankReader treebankReader) {
		FrenchTreebankSentenceReader reader = new FrenchTreebankSentenceReader(treebankReader);
		return reader;
	}

	@Override
	public TokeniserAnnotatedCorpusReader getTokeniserAnnotatedCorpusReader(TreebankReader treebankReader) {
		FrenchTreebankTokenReader reader = new FrenchTreebankTokenReader(treebankReader);
		reader.setTreebankService(this.getTreebankService());
		reader.setPosTaggerService(posTaggerService);
		reader.setTalismaneService(talismaneService);
		reader.setIgnoreCase(false);
		return reader;
	}

	@Override
	public PosTagAnnotatedCorpusReader getPosTagAnnotatedCorpusReader(TreebankReader treebankReader, FtbPosTagMapper ftbPosTagMapper) {
		FrenchTreebankTokenReader reader = (FrenchTreebankTokenReader) this.getTokeniserAnnotatedCorpusReader(treebankReader);
		reader.setFtbPosTagMapper(ftbPosTagMapper);
		return reader;
	}

	@Override
	public PosTagAnnotatedCorpusReader getPosTagAnnotatedCorpusReader(TreebankReader treebankReader, FtbPosTagMapper ftbPosTagMapper,
			boolean useCompoundPosTags) {
		FrenchTreebankTokenReader reader = (FrenchTreebankTokenReader) this.getTokeniserAnnotatedCorpusReader(treebankReader);
		reader.setFtbPosTagMapper(ftbPosTagMapper);
		reader.setUseCompoundPosTags(useCompoundPosTags);
		return reader;
	}

	@Override
	public TokeniserAnnotatedCorpusReader getTokeniserAnnotatedCorpusReader(TreebankReader treebankReader, Writer writer) {
		FrenchTreebankTokenReader reader = (FrenchTreebankTokenReader) this.getTokeniserAnnotatedCorpusReader(treebankReader);
		reader.setCsvFileErrorWriter(writer);
		return reader;
	}

	@Override
	public TokeniserAnnotatedCorpusReader getTokeniserAnnotatedCorpusReader(TreebankReader treebankReader, boolean useCompoundPosTags) {
		return this.getTokeniserAnnotatedCorpusReader(treebankReader, null, useCompoundPosTags);
	}

	@Override
	public TokeniserAnnotatedCorpusReader getTokeniserAnnotatedCorpusReader(TreebankReader treebankReader, FtbPosTagMapper ftbPosTagMapper,
			boolean useCompoundPosTags) {
		FrenchTreebankTokenReader reader = (FrenchTreebankTokenReader) this.getTokeniserAnnotatedCorpusReader(treebankReader);
		reader.setUseCompoundPosTags(useCompoundPosTags);
		reader.setFtbPosTagMapper(ftbPosTagMapper);
		return reader;
	}

	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public TalismaneService getTalismaneService() {
		return talismaneService;
	}

	public void setTalismaneService(TalismaneService talismaneService) {
		this.talismaneService = talismaneService;
	}
}
