/*
 * Created on 7 Jan 2010
 */
package com.joliciel.frenchTreebank.upload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;

import com.joliciel.frenchTreebank.PhraseSubunit;
import com.joliciel.frenchTreebank.PhraseUnit;
import com.joliciel.frenchTreebank.Sentence;
import com.joliciel.frenchTreebank.TreebankFile;
import com.joliciel.frenchTreebank.TreebankService;

import junit.framework.TestCase;

public class TreebankSAXParserTest extends TestCase {
    @SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(TreebankSAXParserTest.class);
    private  Mockery mockContext = new Mockery();

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testParseDocument() {
        final TreebankService treebankService = mockContext.mock(TreebankService.class);
        
        final String filePath = "data/test.cat.xml";
        final TreebankFile treebankFile = mockContext.mock(TreebankFile.class);
        final Sentence sentence1 = mockContext.mock(Sentence.class, "sentence1");
        final Sentence sentence2 = mockContext.mock(Sentence.class, "sentence2");
        
        final PhraseUnit phraseUnit1 = mockContext.mock(PhraseUnit.class, "phraseUnit1");
        final PhraseSubunit phraseSubunit1_1 = mockContext.mock(PhraseSubunit.class, "phraseSubunit1_1");
        final PhraseSubunit phraseSubunit1_2 = mockContext.mock(PhraseSubunit.class, "phraseSubunit1_2");
        final PhraseSubunit phraseSubunit1_3 = mockContext.mock(PhraseSubunit.class, "phraseSubunit1_3");
        final PhraseUnit phraseUnit2 = mockContext.mock(PhraseUnit.class, "phraseUnit2");
        final Sequence seq = mockContext.sequence("seq");
        
        final TreebankSAXParser parser = new TreebankSAXParser();
        parser.setTreebankService(treebankService);
        
        mockContext.checking(new Expectations() {{
            oneOf (treebankService).loadOrCreateTreebankFile(filePath); will (returnValue(treebankFile));
            oneOf (treebankService).newSentence(); will (returnValue(sentence1));
            oneOf (treebankService).newSentence(); will (returnValue(sentence2));
            
            oneOf (sentence1).setSentenceNumber("1"); inSequence(seq);
            oneOf (sentence1).setFile(treebankFile);
            oneOf (sentence1).openPhrase("NP", "SUJ"); inSequence(seq);
            oneOf (sentence1).newPhraseUnit("N", "P", "fs", "Lyonnaise-Dumez"); will(returnValue(phraseUnit1)); inSequence(seq);
            oneOf (phraseUnit1).newSubunit("A", null, null); will(returnValue(phraseSubunit1_1)); inSequence(seq);
            oneOf (phraseSubunit1_1).setText("Lyonnaise"); inSequence(seq);
            oneOf (phraseUnit1).newSubunit("PONCT", null, null); will(returnValue(phraseSubunit1_2)); inSequence(seq);
            oneOf (phraseSubunit1_2).setText("-"); inSequence(seq);
            oneOf (phraseUnit1).newSubunit("N", null, null); will(returnValue(phraseSubunit1_3)); inSequence(seq);
            oneOf (phraseSubunit1_3).setText("Dumez"); inSequence(seq);
            oneOf (sentence1).closePhrase(); inSequence(seq);
            oneOf (sentence1).openPhrase("VN", null); inSequence(seq);
            oneOf (sentence1).newPhraseUnit("V", "", "P3s", "venir");will (returnValue(phraseUnit2)); inSequence(seq);
            oneOf (phraseUnit2).setText("vient");  inSequence(seq);
            oneOf (sentence1).closePhrase(); inSequence(seq);
            oneOf (sentence1).openPhrase("VPinf", "DE-OBJ"); inSequence(seq);
            oneOf (sentence1).newPhraseUnit("P", null, null, "de"); inSequence(seq);
            oneOf (sentence1).openPhrase("VN", null); inSequence(seq);
            oneOf (sentence1).newPhraseUnit("V", "", "W", "hispaniser"); inSequence(seq);
            oneOf (sentence1).closePhrase(); inSequence(seq);
            oneOf (sentence1).openPhrase("NP", "OBJ"); inSequence(seq);
            oneOf (sentence1).newPhraseUnit("D", "poss", "3fss", "son"); inSequence(seq);
            oneOf (sentence1).newPhraseUnit("N", "C", "fs", "filiale"); inSequence(seq);
            oneOf (sentence1).openPhrase("AP", null); inSequence(seq);
            oneOf (sentence1).newPhraseUnit("A", "qual", "fs", "espagnol"); inSequence(seq);
            oneOf (sentence1).closePhrase(); inSequence(seq);
            oneOf (sentence1).closePhrase(); inSequence(seq);
            oneOf (sentence1).closePhrase(); inSequence(seq);
            oneOf (sentence1).newPhraseUnit("PONCT", "S", null, "."); inSequence(seq);
            oneOf (sentence1).save(); inSequence(seq);
            
            oneOf (sentence2).setSentenceNumber("2"); inSequence(seq);
            oneOf (sentence2).setFile(treebankFile); 
            oneOf (sentence2).openPhrase("NP", "SUJ"); inSequence(seq);
            oneOf (sentence2).newPhraseUnit("D", "def", "fs", "le"); inSequence(seq);
            oneOf (sentence2).newPhraseUnit("N", "P", "fs", "Caixa"); inSequence(seq);
            oneOf (sentence2).openPhrase("PP", null); inSequence(seq);
            oneOf (sentence2).newPhraseUnit("P", null, null, "de"); inSequence(seq);
            oneOf (sentence2).openPhrase("NP", null); inSequence(seq);
            oneOf (sentence2).newPhraseUnit("D", "def", "ms", "le"); inSequence(seq);
            oneOf (sentence2).newPhraseUnit("N", "C", "ms", "capital"); inSequence(seq);
            oneOf (sentence2).closePhrase(); inSequence(seq);
            oneOf (sentence2).closePhrase(); inSequence(seq);
            oneOf (sentence2).closePhrase(); inSequence(seq);
            oneOf (sentence2).openPhrase("VN", null); inSequence(seq);
            oneOf (sentence2).newPhraseUnit("V", "", "I3s", "avoir"); inSequence(seq);
            oneOf (sentence2).newPhraseUnit("V", "", "Kms", "faire"); inSequence(seq);
            oneOf (sentence2).newPhraseUnit("V", "", "W", "manger"); inSequence(seq);
            oneOf (sentence2).closePhrase(); inSequence(seq);
            oneOf (sentence2).newPhraseUnit("PONCT", "S", null, "."); inSequence(seq);
            oneOf (sentence2).save(); inSequence(seq);
       }});
        
        parser.parseDocument(filePath);
    }

}
