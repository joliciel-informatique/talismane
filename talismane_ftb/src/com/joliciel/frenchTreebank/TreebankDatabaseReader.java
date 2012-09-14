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
package com.joliciel.frenchTreebank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TreebankDatabaseReader implements TreebankReader {
	TreebankSubSet treebankSubSet;
	int sentenceCount = 0;
	int startSentence = 0;
	List<Integer> sentenceIds = null;
	TreebankService treebankService;
	int currentIndex = 0;
	
	public TreebankDatabaseReader(TreebankSubSet treebankSubSet) {
		this.treebankSubSet = treebankSubSet;
	}

	@Override
	public boolean hasNextSentence() {
		if (this.sentenceIds==null)
			sentenceIds = this.treebankService.findSentenceIds(treebankSubSet, sentenceCount, startSentence);
		return currentIndex < sentenceIds.size();	
	}

	@Override
	public Sentence nextSentence() {
		int sentenceId = sentenceIds.get(currentIndex++);
		Sentence sentence = treebankService.loadSentence(sentenceId);
		return sentence;
	}

	public TreebankService getTreebankService() {
		return treebankService;
	}

	public void setTreebankService(TreebankService treebankService) {
		this.treebankService = treebankService;
	}

	public int getSentenceCount() {
		return sentenceCount;
	}

	public void setSentenceCount(int sentenceCount) {
		this.sentenceCount = sentenceCount;
	}

	public int getStartSentence() {
		return startSentence;
	}

	public void setStartSentence(int startSentence) {
		this.startSentence = startSentence;
	}

	@Override
	public Map<String, String> getCharacteristics() {
		Map<String,String> characteristics = new HashMap<String, String>();
		characteristics.put("treebankSubSet", treebankSubSet.toString());
		characteristics.put("startSentence", "" + startSentence);
		characteristics.put("sentenceCount", "" + sentenceCount);
		return characteristics;
	}

}
