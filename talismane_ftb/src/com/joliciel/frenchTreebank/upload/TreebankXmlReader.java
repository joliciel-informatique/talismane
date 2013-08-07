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
package com.joliciel.frenchTreebank.upload;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.frenchTreebank.PhraseSubunit;
import com.joliciel.frenchTreebank.PhraseUnit;
import com.joliciel.frenchTreebank.Sentence;
import com.joliciel.frenchTreebank.TreebankFile;
import com.joliciel.frenchTreebank.TreebankReader;
import com.joliciel.frenchTreebank.TreebankService;
import com.joliciel.frenchTreebank.util.LogUtils;
import com.joliciel.talismane.utils.PerformanceMonitor;

class TreebankXmlReader implements TreebankReader {
	private static final Log LOG = LogFactory.getLog(TreebankXmlReader.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(TreebankXmlReader.class);
	    
    private TreebankFile treebankFile;
    private TreebankService treebankService;
    private Sentence sentence = null;
    private PhraseUnit phraseUnit = null;
    private PhraseSubunit phraseSubunit = null;
    private String tempVal = null;
    boolean isPhraseUnitCompound = false;
    XMLEventReader eventReader = null;
    private int sentenceCount;
    private String sentenceNumber = "";
    private int currentSentenceCount = 0;
    private File file = null;
    private List<File> files = new ArrayList<File>();
    private int currentFileIndex = 0;
	
    public TreebankXmlReader(File file) {
    	this.file = file;
    	this.addFiles(file, this.files);
    }
    
    void addFiles(File file, List<File> files) {
    	if (file.isDirectory()) {
    		File[] fileArray = file.listFiles();
    		for (File oneFile : fileArray) {
    			this.addFiles(oneFile, files);
    		}
    	} else if (file.getName().endsWith(".xml")) {
    		files.add(file);
    	}
    }
    
    /* (non-Javadoc)
	 * @see com.joliciel.frenchTreebank.upload.TreebankReader#hasNextSentence()
	 */
    @Override
	public boolean hasNextSentence() {
    	MONITOR.startTask("hasNextSentence");
		try {
	    	if (sentenceCount>0 && currentSentenceCount==sentenceCount)
	    		return false;
	    	
	    	if (eventReader==null)
	    		this.getNextEventReader();
	    	
	    	boolean sentenceClosed = false;
	    	while (eventReader!=null && !sentenceClosed) {
				while(eventReader.hasNext() && !sentenceClosed) {
				    XMLEvent xmlEvent;
					try {
						xmlEvent = eventReader.nextEvent();
					} catch (XMLStreamException e) {
						LogUtils.logError(LOG, e);
						throw new RuntimeException(e);
					}
				    switch (xmlEvent.getEventType()) {
				        case XMLEvent.START_ELEMENT:
				        	StartElement startElementEvent = xmlEvent.asStartElement();
				        	this.startElement(startElementEvent);
				        	break;
				        case XMLEvent.END_ELEMENT:
				        	EndElement endElementEvent = xmlEvent.asEndElement();
				        	sentenceClosed = this.endElement(endElementEvent);
				        	break;
				        case XMLEvent.PROCESSING_INSTRUCTION:
				        	break;
				        case XMLEvent.CHARACTERS:
				        	Characters charactersEvent = xmlEvent.asCharacters();
				        	this.characters(charactersEvent);
				        	break;
				        case XMLEvent.COMMENT:
				        	break;
				        case XMLEvent.START_DOCUMENT:
				        	break;
				        case XMLEvent.END_DOCUMENT:
				        	break;
				        case XMLEvent.ENTITY_REFERENCE:
				        	break;
				        case XMLEvent.ATTRIBUTE:
				        	break;
				        case XMLEvent.DTD:
				        	break;
				        case XMLEvent.CDATA:
				        	break;
				        case XMLEvent.SPACE:
				        	break;
				    }
				}
				if (!eventReader.hasNext()) {
					eventReader = null;
			    	this.getNextEventReader();
				}
				if (sentenceNumber!=null && sentenceNumber.length()>0 && sentenceClosed) {
					if (!sentenceNumber.equals(sentence.getSentenceNumber())) {
						sentenceClosed = false;
						sentence = null;
					}
				}
	    	}
			return sentenceClosed;
		} finally {
			MONITOR.endTask("hasNextSentence");
		}
	}
    
    void getNextEventReader() {
    	if (eventReader==null && currentFileIndex<files.size()) {
    		File file = files.get(currentFileIndex++);
			try {
				LOG.info("Reading file: " + file.getName());
				InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
	   			XMLInputFactory factory = XMLInputFactory.newInstance();
    			eventReader = factory.createXMLEventReader(inputStream);
			} catch (FileNotFoundException e) {
	   			LogUtils.logError(LOG, e);
    			throw new RuntimeException(e);
     		} catch (XMLStreamException e) {
    			LogUtils.logError(LOG, e);
    			throw new RuntimeException(e);
    		}
     		treebankFile = this.treebankService.newTreebankFile(file.getName());
    	}
    }
    
    /* (non-Javadoc)
	 * @see com.joliciel.frenchTreebank.upload.TreebankReader#nextSentence()
	 */
    @Override
	public Sentence nextSentence() {
    	return sentence;
    }

    //Event Handlers
    public void startElement(StartElement startElementEvent) {
    	String qName = startElementEvent.getName().getLocalPart();
    	@SuppressWarnings("rawtypes")
		Iterator iAttributes = startElementEvent.getAttributes();
    	Map<String,String> attributes = new HashMap<String, String>();
    	while (iAttributes.hasNext()) {
	    	Attribute attribute = (Attribute) iAttributes.next();
	        String name = attribute.getName().getLocalPart();
	        String value = attribute.getValue();
	        attributes.put(name, value);
    	}
    	
    	// clear out tempVal whenever a new element is started
    	tempVal = "";
        if(qName.equalsIgnoreCase("SENT")) {
            // a new sentence
            sentence = treebankService.newSentence();
            String sentenceNumber = attributes.get("nb");
            if (LOG.isDebugEnabled())
            	LOG.debug("Sentence number " + sentenceNumber);
            sentence.setSentenceNumber(sentenceNumber);
            sentence.setFile(treebankFile);
        } else if (qName.equalsIgnoreCase("w")) {
            // a new word or compound word
            if (phraseUnit==null) {
                String categoryCode = attributes.get("cat");
                String subCategoryCode = attributes.get("subcat");
                String morphologyCode = attributes.get("mph");
                String lemma = attributes.get("lemma");
                boolean isCompound = false;
                String isCompoundStr = attributes.get("compound");
                if (isCompoundStr!=null && isCompoundStr.equalsIgnoreCase("yes"))
                	isCompound = true;
                String compoundId = attributes.get("id");
                String compoundNextId = attributes.get("next");
                String compoundPrevId = attributes.get("prev");
                //String isCompound = attributes.getValue("compound");
                // ignoring compound attribute as not reliable - instead relying on embedded words to indicate a compound phrase unit
                if (LOG.isTraceEnabled())
                	LOG.trace("Opening w " + lemma);
                phraseUnit = sentence.newPhraseUnit(categoryCode, subCategoryCode, morphologyCode, lemma, isCompound, compoundId, compoundNextId, compoundPrevId);
            
                String word = attributes.get("word");
                if (word!=null) {
                	phraseUnit.setText(word);
                }
            } else {
                isPhraseUnitCompound = true;
                String categoryCode = attributes.get("catint");
                String subCategoryCode = attributes.get("subcat");
                String morphologyCode = attributes.get("mph");
                if (LOG.isTraceEnabled())
                	LOG.trace("Opening subunit " + categoryCode);
                phraseSubunit = phraseUnit.newSubunit(categoryCode, subCategoryCode, morphologyCode);
            }
        } else if (qName.equalsIgnoreCase("sentence")) {
            // ignore for now, will only be treated in end element
        } else if (qName.equalsIgnoreCase("text")) {
            // top level text tag, we don't need to do nothing
        } else {
            // must be a phrase
            if (sentence!=null) {
                String functionCode = attributes.get("fct");
            	if (LOG.isTraceEnabled())
            		LOG.trace("Opening phrase " + qName + ", " + functionCode);
                sentence.openPhrase(qName, functionCode);
            }
        }
    }
    

    public void characters(Characters charactersEvent) {
    	// add the characters to tempVal
        tempVal += charactersEvent.getData();;
    }
    
    public boolean endElement(EndElement endElementEvent) {
    	boolean sentenceClosed = false;
    	String qName = endElementEvent.getName().getLocalPart();
        if(qName.equalsIgnoreCase("SENT")) {
            //add it to the list
            sentence.close();
            sentenceClosed = true;
            currentSentenceCount++;
        } else if (qName.equalsIgnoreCase("w")) {
            if (phraseSubunit!=null) {
            	if (LOG.isTraceEnabled())
            		LOG.trace("Closing subunit " + tempVal);
                phraseSubunit.setText(tempVal.trim());
                phraseSubunit = null;
            } else if (phraseUnit!=null) {
                if (LOG.isTraceEnabled())
                	LOG.trace("Closing w " + tempVal);
                if (!isPhraseUnitCompound)
                    phraseUnit.setText(tempVal.trim());
                phraseUnit = null;
                isPhraseUnitCompound = false;
            }
        } else if (qName.equalsIgnoreCase("sentence")) {
            sentence.setText(tempVal);
        } else if (qName.equalsIgnoreCase("text")) {
            // top level text tag, we don't need to do nothing
        } else {
            // must be a phrase
        	if (LOG.isTraceEnabled())
        		LOG.trace("Closing phrase " + qName);
            if (sentence!=null)
                sentence.closePhrase();
        }
        return sentenceClosed;
    }
    
    public TreebankService getTreebankService() {
        return treebankService;
    }
    public void setTreebankService(TreebankService treebankService) {
        this.treebankService = treebankService;
    }
    
	@Override
	public Map<String, String> getCharacteristics() {
		Map<String,String> characteristics = new HashMap<String, String>();
		if (this.file!=null)
			characteristics.put("file", file.getPath());
		return characteristics;
	}

	/**
	 * Maximum number of sentences to read.
	 */
	public int getSentenceCount() {
		return sentenceCount;
	}

	public void setSentenceCount(int sentenceCount) {
		this.sentenceCount = sentenceCount;
	}

	/**
	 * The single sentence to read.
	 * @return
	 */
	public String getSentenceNumber() {
		return sentenceNumber;
	}

	public void setSentenceNumber(String sentenceNumber) {
		this.sentenceNumber = sentenceNumber;
	}
	
	
}
