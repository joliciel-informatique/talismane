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

import java.util.Iterator;
import java.util.List;

class TreebankFileImpl extends EntityImpl implements TreebankFileInternal {
    /**
	 * 
	 */
	private static final long serialVersionUID = -5918812007515159506L;
	String fileName;
    TreebankServiceInternal treebankServiceInternal;
 
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TreebankFile) {
            return ((TreebankFile) obj).getId()==this.getId();
        }
        return false;
    }
    @Override
    public int hashCode() {
        if (this.getId()!=0)
            return this.getId();
        else
            return super.hashCode();
    }
    public TreebankServiceInternal getTreebankServiceInternal() {
        return treebankServiceInternal;
    }
    public void setTreebankServiceInternal(
            TreebankServiceInternal treebankServiceInternal) {
        this.treebankServiceInternal = treebankServiceInternal;
    }
    @Override
    public void saveInternal() {
        this.treebankServiceInternal.saveTreebankFileInternal(this);
    }
    
	@Override
	public Iterator<Sentence> getSentences() {
		List<Integer> sentenceIds = treebankServiceInternal.findSentenceIds(this);
		final Iterator<Integer> iSentenceIds = sentenceIds.iterator();
		
		Iterator<Sentence> iterator = new Iterator<Sentence>() {
			@Override
			public void remove() {
				throw new RuntimeException("Unsupported operation: remove");
			}
			
			@Override
			public Sentence next() {
				int sentenceId = iSentenceIds.next();
				Sentence sentence = treebankServiceInternal.loadFullSentence(sentenceId);
				return sentence;
			}
			
			@Override
			public boolean hasNext() {
				return iSentenceIds.hasNext();
			}
		};
		return iterator;
	}
    
    
}
