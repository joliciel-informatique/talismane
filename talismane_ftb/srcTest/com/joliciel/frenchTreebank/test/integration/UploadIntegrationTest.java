/*
 * Created on 12 Jan 2010
 */
package com.joliciel.frenchTreebank.test.integration;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.frenchTreebank.Phrase;
import com.joliciel.frenchTreebank.PhraseUnit;
import com.joliciel.frenchTreebank.TreebankService;
import com.joliciel.frenchTreebank.TreebankServiceLocator;
import com.joliciel.frenchTreebank.search.SearchResult;
import com.joliciel.frenchTreebank.search.SearchService;
import com.joliciel.frenchTreebank.search.XmlPatternSearch;
import com.joliciel.frenchTreebank.test.DaoTestUtils;
import com.joliciel.frenchTreebank.upload.TreebankSAXParser;
import com.joliciel.talismane.utils.util.SimpleObjectCache;

import junit.framework.TestCase;

public class UploadIntegrationTest extends TestCase {
    private static final Log LOG = LogFactory.getLog(UploadIntegrationTest.class);
    
    protected void setUp() throws Exception {
        super.setUp();
        DaoTestUtils.wipeDB();
        SimpleObjectCache cache = new SimpleObjectCache();
        cache.clearCache();
   }

    public final void testParseDocument() throws Exception {
         
        TreebankServiceLocator locator = TreebankServiceLocator.getInstance();
        locator.setDataSourcePropertiesFile("jdbc-test.properties");
        
        final TreebankService treebankService = locator.getTreebankService();
        final TreebankSAXParser parser = new TreebankSAXParser();
        parser.setTreebankService(treebankService);
        parser.parseDocument("data/test.cat.xml");
    }
    
    public final void testSearch() throws Exception {
        
        TreebankServiceLocator locator = TreebankServiceLocator.getInstance();
        locator.setDataSourcePropertiesFile("jdbc-test.properties");
        
        final TreebankService treebankService = locator.getTreebankService();
        final TreebankSAXParser parser = new TreebankSAXParser();
        parser.setTreebankService(treebankService);
        parser.parseDocument("data/test.cat.xml");
        
        final SearchService searchService = locator.getSearchService();
        final XmlPatternSearch search = searchService.newXmlPatternSearch();
        
        String xmlPattern =
            "<phrase>"
            +"    <phrase type=\"VN,VPinf\">"
            +"         <w cat=\"V\" />"
            +"     </phrase>"
            +"     <NP>"
            +"         <w cat=\"N\" />"
            +"         <PP exists=\"no\">"
            +"             <w cat=\"P\" />"
            + "        </PP>"
            +"         <AP>"
            +"             <w lemma=\"filiale\" exists=\"no\" />"
            +"             <w cat=\"A\" />"
            +"         </AP>"
            +"     </NP>"
            +"  </phrase>";
        search.setXmlPattern(xmlPattern);
        List<SearchResult> searchResults = search.perform();
        for (SearchResult searchResult : searchResults) {
            Phrase phrase = searchResult.getPhrase();
            List<PhraseUnit> phraseUnits = searchResult.getPhraseUnits();
            LOG.debug("Phrase: " + phrase.getId());
            for (PhraseUnit phraseUnit : phraseUnits)
                LOG.debug("PhraseUnit: " + phraseUnit.getId() + " " + phraseUnit.getLemma().getText());
        }
        assertEquals(1, searchResults.size());
    }
}
