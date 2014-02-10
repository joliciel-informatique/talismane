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
package com.joliciel.frenchTreebank.upload;

import com.joliciel.frenchTreebank.Sentence;
import com.joliciel.frenchTreebank.TreebankReader;

public class TreebankXmlLoaderImpl implements TreebankXmlLoader {
	TreebankReader reader;
	
	
    public TreebankXmlLoaderImpl(TreebankReader reader) {
		super();
		this.reader = reader;
	}


	/* (non-Javadoc)
	 * @see com.joliciel.frenchTreebank.upload.TreebankXmlLoader#loadDocument(java.io.File)
	 */
    @Override
	public void load() {
    	while (reader.hasNextSentence()) {
    		Sentence sentence = reader.nextSentence();
            sentence.save();
    	}
    }
    
    
}
