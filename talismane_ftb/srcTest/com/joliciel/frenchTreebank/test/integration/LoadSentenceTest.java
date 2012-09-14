package com.joliciel.frenchTreebank.test.integration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.frenchTreebank.Phrase;
import com.joliciel.frenchTreebank.PhraseElement;
import com.joliciel.frenchTreebank.PhraseUnit;
import com.joliciel.frenchTreebank.Sentence;
import com.joliciel.frenchTreebank.TreebankService;
import com.joliciel.frenchTreebank.TreebankServiceLocator;

import junit.framework.TestCase;

public class LoadSentenceTest extends TestCase {
	private static final Log LOG = LogFactory.getLog(LoadSentenceTest.class);

	public void testLoadFullSentence() {
		TreebankServiceLocator locator = TreebankServiceLocator.getInstance();
		locator.setDataSourcePropertiesFile("jdbc-live.properties");
		
		TreebankService treebankService = locator.getTreebankService();
		Sentence sentence = treebankService.loadFullSentence(123909);
		LOG.debug(sentence.getText());
		
		this.logPhrase(sentence, 0);
	}
	
	public void logPhrase(Phrase phrase, int depth) {
		String prefix = "";
		for (int i=0; i<depth; i++) prefix += "-";
		prefix += " ";
		for (PhraseElement phraseElement : phrase.getElements()) {
			if (phraseElement.isPhrase()) {
				Phrase subPhrase = (Phrase)phraseElement;
				LOG.debug(prefix + subPhrase.getPhraseType().getCode());
				this.logPhrase(subPhrase, depth+1);
			} else {
				PhraseUnit phraseUnit = (PhraseUnit) phraseElement;
				LOG.debug(prefix + phraseUnit.getWord().getOriginalText());
			}
		}
	}
}
