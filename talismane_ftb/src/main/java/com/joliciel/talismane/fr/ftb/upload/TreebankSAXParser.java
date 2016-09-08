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
package com.joliciel.talismane.fr.ftb.upload;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.joliciel.talismane.fr.ftb.PhraseSubunit;
import com.joliciel.talismane.fr.ftb.PhraseUnit;
import com.joliciel.talismane.fr.ftb.Sentence;
import com.joliciel.talismane.fr.ftb.TreebankFile;
import com.joliciel.talismane.fr.ftb.TreebankService;

public class TreebankSAXParser extends DefaultHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TreebankSAXParser.class);
    
    private TreebankFile treebankFile;
    private TreebankService treebankService;
    private Sentence sentence = null;
    private PhraseUnit phraseUnit = null;
    private PhraseSubunit phraseSubunit = null;
    private String tempVal = null;
    boolean isPhraseUnitCompound = false;
    
    public TreebankSAXParser() {
        
    }
    public  void parseDocument(String filePath) {
        //get or create the treebank file
        treebankFile = this.getTreebankService().loadOrCreateTreebankFile(filePath);
        
        //get a factory
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {
        
            //get a new instance of parser
            SAXParser sp = spf.newSAXParser();
            
            //parse the file and also register this class for call backs
            sp.parse(filePath, this);
            
        }catch(SAXException se) {
            se.printStackTrace();
        }catch(ParserConfigurationException pce) {
            pce.printStackTrace();
        }catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    //Event Handlers
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //reset
        //LOG.debug("<" + qName + ">");
    	// clear out tempVal whenever a new element is started
    	tempVal = "";
        if(qName.equalsIgnoreCase("SENT")) {
            // a new sentence
            sentence = treebankService.newSentence();
            String sentenceNumber = attributes.getValue("nb");
            LOG.debug("Sentence number " + sentenceNumber);
            sentence.setSentenceNumber(sentenceNumber);
            sentence.setFile(treebankFile);
        } else if (qName.equalsIgnoreCase("w")) {
            // a new word or compound word
            if (phraseUnit==null) {
                String categoryCode = attributes.getValue("cat");
                String subCategoryCode = attributes.getValue("subcat");
                String morphologyCode = attributes.getValue("mph");
                String lemma = attributes.getValue("lemma");
                boolean isCompound = false;
                String isCompoundStr = attributes.getValue("compound");
                if (isCompoundStr!=null && isCompoundStr.equalsIgnoreCase("yes"))
                	isCompound = true;
                String compoundId = attributes.getValue("id");
                String compoundNextId = attributes.getValue("next");
                String compoundPrevId = attributes.getValue("prev");
                //String isCompound = attributes.getValue("compound");
                // ignoring compound attribute as not reliable - instead relying on embedded words to indicate a compound phrase unit
                LOG.debug("Opening w " + lemma);
                phraseUnit = sentence.newPhraseUnit(categoryCode, subCategoryCode, morphologyCode, lemma, isCompound, compoundId, compoundNextId, compoundPrevId);
            } else {
                isPhraseUnitCompound = true;
                String categoryCode = attributes.getValue("catint");
                String subCategoryCode = attributes.getValue("subcat");
                String morphologyCode = attributes.getValue("mph");
               LOG.debug("Opening subunit " + categoryCode);
                phraseSubunit = phraseUnit.newSubunit(categoryCode, subCategoryCode, morphologyCode);
            }
        } else if (qName.equalsIgnoreCase("text")) {
            // top level text tag, we don't need to do nothing
        } else {
            // must be a phrase
            if (sentence!=null) {
                String functionCode = attributes.getValue("fct");
                LOG.debug("Opening phrase " + qName + ", " + functionCode);
                sentence.openPhrase(qName, functionCode);
            }
        }
    }
    

    public void characters(char[] ch, int start, int length) throws SAXException {
    	// add the characters to tempVal
        tempVal += new String(ch,start,length);
    }
    
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //LOG.debug("</" + qName + ">");
        if(qName.equalsIgnoreCase("SENT")) {
            //add it to the list
            sentence.close();
            LOG.debug("Saving sentence");
            sentence.save();
            sentence = null;
        } else if (qName.equalsIgnoreCase("w")) {
            if (phraseSubunit!=null) {
                LOG.debug("Closing subunit " + tempVal);
                phraseSubunit.setText(tempVal.trim());
                phraseSubunit = null;
            } else if (phraseUnit!=null) {
                LOG.debug("Closing w " + tempVal);
                if (!isPhraseUnitCompound)
                    phraseUnit.setText(tempVal.trim());
                phraseUnit = null;
                isPhraseUnitCompound = false;
            }
        } else if (qName.equalsIgnoreCase("text")) {
            // top level text tag, we don't need to do nothing
        } else {
            // must be a phrase
            LOG.debug("Closing phrase " + qName);
            if (sentence!=null)
                sentence.closePhrase();
        }
    }
    public TreebankService getTreebankService() {
        return treebankService;
    }
    public void setTreebankService(TreebankService treebankService) {
        this.treebankService = treebankService;
    }
}
