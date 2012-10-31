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
import com.joliciel.frenchTreebank.TreebankService;
import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;

class TreebankExportServiceImpl implements TreebankExportService {
	private TreebankService treebankService;
	private TokeniserService tokeniserService;
	private PosTaggerService posTaggerService;
	private FilterService filterService;
	private TokenFilterService tokenFilterService;

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
	public FtbPosTagMapper getFtbPosTagMapper(List<String> descriptors,
			PosTagSet posTagSet) {
		FtbPosTagMapperImpl mapper = new FtbPosTagMapperImpl(descriptors, posTagSet);
		return mapper;
	}

	@Override
	public SentenceDetectorAnnotatedCorpusReader getSentenceDetectorAnnotatedCorpusReader(
			TreebankReader treebankReader) {
		FrenchTreebankSentenceReader reader = new FrenchTreebankSentenceReader(treebankReader);
		return reader;
	}

	@Override
	public TokeniserAnnotatedCorpusReader getTokeniserAnnotatedCorpusReader(
			TreebankReader treebankReader) {
		FrenchTreebankTokenReader reader = new FrenchTreebankTokenReader(treebankReader);
		reader.setTreebankService(this.getTreebankService());
		reader.setTokeniserService(tokeniserService);
		reader.setPosTaggerService(posTaggerService);
		reader.setFilterService(filterService);
		reader.setTokenFilterService(tokenFilterService);
    	reader.setIgnoreCase(false);
		return reader;
	}

	@Override
	public PosTagAnnotatedCorpusReader getPosTagAnnotatedCorpusReader(
			TreebankReader treebankReader,
			FtbPosTagMapper ftbPosTagMapper) {
		FrenchTreebankTokenReader reader = (FrenchTreebankTokenReader) this.getTokeniserAnnotatedCorpusReader(treebankReader);
		reader.setFtbPosTagMapper(ftbPosTagMapper);
		return reader;
	}

	@Override
	public TokeniserAnnotatedCorpusReader getTokeniserAnnotatedCorpusReader(
			TreebankReader treebankReader, Writer writer) {
		FrenchTreebankTokenReader reader = (FrenchTreebankTokenReader) this.getTokeniserAnnotatedCorpusReader(treebankReader);
		reader.setCsvFileErrorWriter(writer);
		return reader;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public FilterService getFilterService() {
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}

	public TokenFilterService getTokenFilterService() {
		return tokenFilterService;
	}

	public void setTokenFilterService(TokenFilterService tokenFilterService) {
		this.tokenFilterService = tokenFilterService;
	}


}
