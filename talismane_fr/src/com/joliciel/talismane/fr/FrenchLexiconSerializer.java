///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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

import com.joliciel.lefff.LefffService;
import com.joliciel.lefff.LefffServiceLocator;
import com.joliciel.talismane.lexicon.LexicalEntryMorphologyReader;
import com.joliciel.talismane.lexicon.LexiconSerializer;

public class FrenchLexiconSerializer extends LexiconSerializer {
	LexicalEntryMorphologyReader morphologyReader;
	
	@Override
	protected LexicalEntryMorphologyReader getMorphologyReader() {
		return morphologyReader;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FrenchLexiconSerializer instance = new FrenchLexiconSerializer();
		instance.serializeLexicons(args);
	}

	public FrenchLexiconSerializer() {
		LefffServiceLocator lefffServiceLocator = LefffServiceLocator.getInstance();
		LefffService lefffService = lefffServiceLocator.getLefffService();
		morphologyReader = lefffService.getLexicalEntryMorphologyReader();
	}

}
