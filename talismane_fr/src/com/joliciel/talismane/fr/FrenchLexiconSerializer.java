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
		LefffServiceLocator lefffServiceLocator = new LefffServiceLocator();
		LefffService lefffService = lefffServiceLocator.getLefffService();
		morphologyReader = lefffService.getLexicalEntryMorphologyReader();
	}

}
