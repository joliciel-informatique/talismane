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
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.lefff.LefffMemoryBase;
import com.joliciel.lefff.LefffMemoryLoader;
import com.joliciel.talismane.AbstractTalismane;
import com.joliciel.talismane.posTagger.PosTaggerLexiconService;

public class TalismaneFrench extends AbstractTalismane {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(TalismaneFrench.class);
	

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
    	TalismaneFrench instance = new TalismaneFrench();
    	instance.runCommand(args);
    	
	}

	private static ZipInputStream getZipInputStreamFromResource(String resource) {
		InputStream inputStream = getInputStreamFromResource(resource);
		ZipInputStream zis = new ZipInputStream(inputStream);
		
		return zis;
	}
	

	private static InputStream getInputStreamFromResource(String resource) {
		String path = "/com/joliciel/talismane/fr/resources/" + resource;
		InputStream inputStream = TalismaneFrench.class.getResourceAsStream(path); 
		
		return inputStream;
	}

	@Override
	protected InputStream getDefaultPosTagSetFromStream() {
		InputStream posTagInputStream = getInputStreamFromResource("CrabbeCanditoTagset.txt");
		return posTagInputStream;
	}
	

	@Override
	protected InputStream getDefaultPosTaggerRulesFromStream() {
		InputStream inputStream = getInputStreamFromResource("posTaggerConstraints_fr.txt");
		return inputStream;
	}
	

	@Override
	protected PosTaggerLexiconService getDefaultLexiconService() {
      	LefffMemoryLoader loader = new LefffMemoryLoader();
		String lefffPath = "/com/joliciel/talismane/fr/resources/lefff.zip";
		ZipInputStream lefffInputStream = new ZipInputStream(TalismaneFrench.class.getResourceAsStream(lefffPath)); 
    	LefffMemoryBase lefffMemoryBase = loader.deserializeMemoryBase(lefffInputStream);
    	return lefffMemoryBase;

	}

	@Override
	protected ZipInputStream getDefaultSentenceModelStream() {
		String sentenceModelName = "ftbSentenceDetector_fr3.zip";
		return TalismaneFrench.getZipInputStreamFromResource(sentenceModelName);
	}

	@Override
	protected ZipInputStream getDefaultTokeniserModelStream() {
		String tokeniserModelName = "ftbTokeniser_fr7.zip";
		return TalismaneFrench.getZipInputStreamFromResource(tokeniserModelName);
	}

	@Override
	protected ZipInputStream getDefaultPosTaggerModelStream() {
		String posTaggerModelName = "ftbPosTagger_fr7.zip";
		return TalismaneFrench.getZipInputStreamFromResource(posTaggerModelName);
	}

	@Override
	protected ZipInputStream getDefaultParserModelStream() {
		String parserModelName = "ftbDepParser_fr12_ArcEager_cutoff5.zip";
		return TalismaneFrench.getZipInputStreamFromResource(parserModelName);
	}

	
}
