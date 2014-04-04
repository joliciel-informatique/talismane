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
package com.joliciel.talismane.en;

import java.util.Map;

import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.lexicon.DefaultMorphologyReader;
import com.joliciel.talismane.lexicon.LexicalEntryMorphologyReader;
import com.joliciel.talismane.lexicon.LexiconSerializer;

public class EnglishLexiconSerializer extends LexiconSerializer {
	LexicalEntryMorphologyReader morphologyReader = new DefaultMorphologyReader();
	
	@Override
	protected LexicalEntryMorphologyReader getMorphologyReader() {
		return morphologyReader;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		EnglishLexiconSerializer instance = new EnglishLexiconSerializer();
		Map<String,String> argMap = TalismaneConfig.convertArgs(args);
		if (!argMap.containsKey("lexiconPattern") && !argMap.containsKey("regex"))
			argMap.put("regex", "%TOKEN%\\t%LEMMA%\\t%POSTAG%\\t%MORPH%");
		instance.serializeLexicons(argMap);
	}

	public EnglishLexiconSerializer() {
	}

}
