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

import java.util.List;
import java.util.ArrayList;

import com.joliciel.talismane.fr.ftb.PhraseUnit;
import com.joliciel.talismane.fr.ftb.TreebankService;
import com.joliciel.talismane.posTagger.PosTag;

class SimplePhraseUnitReader implements PhraseUnitReader {
	private List<PhraseUnit> phraseUnits;
	private int phraseUnitIndex = -1;
	private PhraseUnit currentPhraseUnit;
	private FtbPosTagMapper ftbPosTagMapper;
	TreebankService treebankService;
	
	public SimplePhraseUnitReader(List<PhraseUnit> phraseUnits) {
		this.phraseUnits = phraseUnits;
	}
	
	/* (non-Javadoc)
	 * @see com.joliciel.frenchTreebank.export.PhraseUnitReader#nextString()
	 */
	@Override
	public String nextString() {
		String phraseUnitText = "";
		phraseUnitIndex++;
		while (phraseUnitIndex<phraseUnits.size()) {
			currentPhraseUnit = phraseUnits.get(phraseUnitIndex);
			phraseUnitText = currentPhraseUnit.getWord().getOriginalText();
			if (phraseUnitText.length()==0)
				phraseUnitIndex++;
			else
				break;
		}
		return phraseUnitText;
	}


	@Override
	public List<String> getCurrentInfo() {
		List<String> info = new ArrayList<String>();
		info.add(currentPhraseUnit==null? "null" : ""+currentPhraseUnit.getId());
		info.add(currentPhraseUnit==null? "null" : currentPhraseUnit.getWord().getOriginalText());
		return info;
	}

	@Override
	public PosTag getPosTag() {
		return ftbPosTagMapper.getPosTag(currentPhraseUnit.getCategory(), currentPhraseUnit.getSubCategory(), currentPhraseUnit.getMorphology());
	}


	public FtbPosTagMapper getFtbPosTagMapper() {
		return ftbPosTagMapper;
	}

	public void setFtbPosTagMapper(FtbPosTagMapper ftbPosTagMapper) {
		this.ftbPosTagMapper = ftbPosTagMapper;
	}

	public TreebankService getTreebankService() {
		return treebankService;
	}

	public void setTreebankService(TreebankService treebankService) {
		this.treebankService = treebankService;
	}
	
	
	
}