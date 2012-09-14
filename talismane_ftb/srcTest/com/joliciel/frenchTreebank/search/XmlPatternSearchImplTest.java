/*
 * Created on 19 Jan 2010
 */
package com.joliciel.frenchTreebank.search;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jmock.Expectations;
import org.jmock.Mockery;

import com.joliciel.frenchTreebank.Category;
import com.joliciel.frenchTreebank.PhraseType;
import com.joliciel.frenchTreebank.TreebankService;
import com.joliciel.frenchTreebank.Word;

import junit.framework.TestCase;

public class XmlPatternSearchImplTest extends TestCase {
    @SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(XmlPatternSearchImplTest.class);
    private  Mockery mockContext = new Mockery();

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @SuppressWarnings("unchecked")
    public final void testPerform() {
        final TreebankService treebankService = mockContext.mock(TreebankService.class);

        String xmlPattern =
            "<phrase>"
            +"    <phrase type=\"VN,VPinf\">"
            +"         <w cat=\"V\" />"
            +"     </phrase>"
            +"     <NP>"
            +"         <w cat=\"N\" />"
            +"         <PP>"
            +"             <w cat=\"P\" lemma=\"de,à\">de,du</w>"
            +"             <NP>"
            +"                 <w cat=\"N\">"
            +"                    <w cat=\"N\"/>"
            + "                </w>"
            +"             </NP>"
            +"         </PP>"
            +"     </NP>"
            +"  </phrase>";
        
        final Category categoryV = mockContext.mock(Category.class, "categoryV");
        final Category categoryN = mockContext.mock(Category.class, "categoryN");
        final Category categoryP = mockContext.mock(Category.class, "categoryP");

        final Word wordDe = mockContext.mock(Word.class, "wordDe");
        final Word wordA = mockContext.mock(Word.class, "wordA");
        final Word wordDu = mockContext.mock(Word.class, "wordDu");
        
        final PhraseType phraseTypeVN = mockContext.mock(PhraseType.class, "phraseTypeVN");
        final PhraseType phraseTypeVPinf = mockContext.mock(PhraseType.class, "phraseTypeVPinf");
        final PhraseType phraseTypeNP = mockContext.mock(PhraseType.class, "phraseTypeNP");
        final PhraseType phraseTypePP = mockContext.mock(PhraseType.class, "phraseTypePP");
        
        XmlPatternSearchImpl xmlPatternSearch = new XmlPatternSearchImpl();
        xmlPatternSearch.setTreebankService(treebankService);
        
        mockContext.checking(new Expectations() {{
            oneOf(treebankService).loadCategory("V"); will (returnValue(categoryV));
            exactly(3).of(treebankService).loadCategory("N"); will (returnValue(categoryN));
            oneOf(treebankService).loadCategory("P"); will (returnValue(categoryP));
            exactly(2).of(treebankService).loadWord("de","de"); will (returnValue(wordDe));
            oneOf(treebankService).loadWord("du","du"); will (returnValue(wordDu));
            oneOf(treebankService).loadWord("à","à"); will (returnValue(wordA));
            oneOf(treebankService).loadPhraseType("VN"); will (returnValue(phraseTypeVN));
            oneOf(treebankService).loadPhraseType("VPinf"); will (returnValue(phraseTypeVPinf));
            exactly(2).of(treebankService).loadPhraseType("NP"); will (returnValue(phraseTypeNP));
            oneOf(treebankService).loadPhraseType("PP"); will (returnValue(phraseTypePP));
                                 
            allowing(categoryV).getId(); will (returnValue(1));
            allowing(categoryN).getId(); will (returnValue(2));
            allowing(categoryP).getId(); will (returnValue(3));
            
            allowing(wordDe).getId(); will (returnValue(1));
            allowing(wordDu).getId(); will (returnValue(2));
            allowing(wordA).getId(); will (returnValue(3));
            
            allowing(phraseTypeVN).getId(); will (returnValue(1));
            allowing(phraseTypeVPinf).getId(); will (returnValue(2));
            allowing(phraseTypeNP).getId(); will (returnValue(3));
            allowing(phraseTypePP).getId(); will (returnValue(4));
          
            oneOf(treebankService).findStuff(with(any(List.class)), with(any(List.class)), with(any(List.class)), with(any(List.class)));
       }});
        
        xmlPatternSearch.setXmlPattern(xmlPattern);
        xmlPatternSearch.perform();
    }

}
