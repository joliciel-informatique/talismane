///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2011 Assaf Urieli
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
package com.joliciel.talismane.fr;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.lefff.LefffMemoryBase;
import com.joliciel.lefff.LefffMemoryLoader;
import com.joliciel.talismane.AbstractTalismane;
import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTaggerLexicon;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;

public class TalismaneFrench extends AbstractTalismane {
	private static final Log LOG = LogFactory.getLog(TalismaneFrench.class);

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
    	TalismaneFrench instance = new TalismaneFrench();
    	TalismaneConfig config = new TalismaneConfig(instance, args);
    	instance.runCommand(config);
	}

	private static ZipInputStream getZipInputStreamFromResource(String resource) {
		InputStream inputStream = getInputStreamFromResource(resource);
		ZipInputStream zis = new ZipInputStream(inputStream);
		
		return zis;
	}

	private static InputStream getInputStreamFromResource(String resource) {
		String path = "/com/joliciel/talismane/fr/resources/" + resource;
		LOG.debug("Getting " + path);
		InputStream inputStream = TalismaneFrench.class.getResourceAsStream(path); 
		
		return inputStream;
	}

	@Override
	public InputStream getDefaultPosTagSetFromStream() {
		InputStream posTagInputStream = getInputStreamFromResource("CrabbeCanditoTagset.txt");
		return posTagInputStream;
	}
	

	@Override
	public InputStream getDefaultPosTaggerRulesFromStream() {
		InputStream inputStream = getInputStreamFromResource("posTaggerConstraints_fr.txt");
		return inputStream;
	}
	

	@Override
	public PosTaggerLexicon getDefaultLexiconService() {
      	LefffMemoryLoader loader = new LefffMemoryLoader();
		String lefffPath = "/com/joliciel/talismane/fr/resources/lefff_PPAdj.zip";
		ZipInputStream lefffInputStream = new ZipInputStream(TalismaneFrench.class.getResourceAsStream(lefffPath)); 
    	LefffMemoryBase lefffMemoryBase = loader.deserializeMemoryBase(lefffInputStream);
    	return lefffMemoryBase;

	}

	@Override
	public ZipInputStream getDefaultSentenceModelStream() {
		String sentenceModelName = "ftbSentenceDetector_fr3.zip";
		return TalismaneFrench.getZipInputStreamFromResource(sentenceModelName);
	}

	@Override
	public ZipInputStream getDefaultTokeniserModelStream() {
		String tokeniserModelName = "ftbTokeniser_fr10.zip";
		return TalismaneFrench.getZipInputStreamFromResource(tokeniserModelName);
	}

	@Override
	public ZipInputStream getDefaultPosTaggerModelStream() {
		String posTaggerModelName = "ftbPosTagger_fr11.zip";
		return TalismaneFrench.getZipInputStreamFromResource(posTaggerModelName);
	}

	@Override
	public ZipInputStream getDefaultParserModelStream() {
		String parserModelName = "ftbDep_parser_arcEager_14.zip";
		return TalismaneFrench.getZipInputStreamFromResource(parserModelName);
	}

	@Override
	public List<TokenSequenceFilter> getTokenSequenceFilters() {
		List<TokenSequenceFilter> tokenFilters = new ArrayList<TokenSequenceFilter>();
		return tokenFilters;
	}

	@Override
	public List<TokenSequenceFilter> getPosTaggerPreprocessingFilters() {
		List<TokenSequenceFilter> tokenFilters = new ArrayList<TokenSequenceFilter>();
		return tokenFilters;
	}

	@Override
	public InputStream getDefaultTextMarkerFiltersFromStream() {
		InputStream inputStream = getInputStreamFromResource("text_marker_filters.txt");
		return inputStream;
	}

	@Override
	public InputStream getDefaultTokenFiltersFromStream() {
		InputStream inputStream = getInputStreamFromResource("token_filters.txt");
		return inputStream;
	}

	@Override
	public TransitionSystem getDefaultTransitionSystem() {
		return this.getParserService().getArcEagerTransitionSystem();
	}
}
