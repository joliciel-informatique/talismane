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
package com.joliciel.lefff;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class LefffLoaderImpl implements LefffLoader {
    private static final Log LOG = LogFactory.getLog(LefffLoaderImpl.class);

    private LefffServiceInternal lefffServiceInternal;
	private int stopLine;
	private int startLine;
	
	@Override
	public void LoadFile(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("ISO-8859-1")) );
		String line = null;
        int i = 0;
        
        LOG.debug("Max lines: " + stopLine);
        while ((line = reader.readLine()) != null) {
        	i++;
        	if (startLine>0 && i < startLine)
        		continue;
        	if (LOG.isDebugEnabled())
        		LOG.debug(i + ": " + line);
            if (stopLine>0 && i > stopLine)
            	break;
            
            //passepoilés	50	v	[pred='passepoiler_____1<obj:(sn),suj:(par-sn)>',cat=v,@passive,@être,@Kmp]
            //!		poncts	[]
            //+		adv	[pred='plus_____1',cat=adv,adv_kind=intens]
            //+		advneg	[neg=+]
            if (!line.startsWith("#")) {
	            LefffEntry entry = lefffServiceInternal.newEntry();
	            String[] tokens = line.split("\t");
	
            	String wordText = tokens[0].trim();
            	Word word = lefffServiceInternal.loadOrCreateWord(wordText);
            	entry.setWordId(word.getId());
            	
            	String lexicalWeightText = tokens[1].trim();
            	if (lexicalWeightText.length()>0) {
            		int lexicalWeight = Integer.parseInt(lexicalWeightText);
            		entry.setLexicalWeight(lexicalWeight);
	            }
            	
            	String categoryCode = tokens[2].trim();
            	Category category = lefffServiceInternal.loadOrCreateCategory(categoryCode);
            	entry.setCategoryId(category.getId());
            	
            	String descriptionString = tokens[3].trim().substring(1, tokens[3].length()-1);
            	String attributeString = "";
            	int predPos = descriptionString.indexOf("pred=");
            	if (predPos<0) {
            		Lemma lemma = lefffServiceInternal.loadOrCreateLemma(wordText, 1, "");
            		entry.setLemmaId(lemma.getId());
            		attributeString = descriptionString;
            	} else {
	            	int firstSingleQuote = descriptionString.indexOf('\'');
	            	int lastSingleQuote = descriptionString.lastIndexOf('\'');
	            	
	            	// pred='passepoiler_____1<obj:(sn),suj:(par-sn)>'
	            	String predicateString = descriptionString.substring(firstSingleQuote+1, lastSingleQuote);
	            	LOG.debug(predicateString);
	            	
	            	int longUnderscorePos = predicateString.indexOf("_____");
	            	if (longUnderscorePos<0) {
	            		// we have an unusual predicate
	               		Lemma lemma = lefffServiceInternal.loadOrCreateLemma(wordText, 1, "");
	            		entry.setLemmaId(lemma.getId());

	            		Attribute predicateAttribute = lefffServiceInternal.loadOrCreateAttribute("pred", predicateString);
	            		entry.getAttributes().add(predicateAttribute);
	            	} else {
	            		// we have a proper lemma
		            	String lemmaText = predicateString.substring(0, longUnderscorePos);
		            	LOG.debug(lemmaText);
		            	int lastUnderscore = predicateString.lastIndexOf('_');
		            	int lemmaIndex = Integer.parseInt(predicateString.substring(lastUnderscore+1, lastUnderscore+2));
		            	LOG.debug(lemmaIndex);
		            	int firstLessThan = predicateString.indexOf('<');
		            	LOG.debug(lastUnderscore + "-" + firstLessThan);
		            	String lemmaComplement = "";
		            	if (firstLessThan >= 0)
		            		lemmaComplement = predicateString.substring(lastUnderscore+2, firstLessThan);
		            	else
		            		lemmaComplement = predicateString.substring(lastUnderscore+2);
		            	LOG.debug(lemmaComplement);
		            	Lemma lemma = lefffServiceInternal.loadOrCreateLemma(lemmaText, lemmaIndex, lemmaComplement);
		            	entry.setLemmaId(lemma.getId());
		            	
		            	String predicateText = "";
		            	if (firstLessThan >= 0) {
			            	predicateText = predicateString.substring(firstLessThan+1, predicateString.length()-1);
		            	}
		            	Predicate predicate = lefffServiceInternal.loadOrCreatePredicate(predicateText);
		            	entry.setPredicateId(predicate.getId());
	            	}

	            	if (lastSingleQuote+2<descriptionString.length())
	            		attributeString = descriptionString.substring(lastSingleQuote+2);

            	} // have predicate
            	// cat=v,@passive,@être,@Kmp
            	String[] attributes = attributeString.split(",");
            	for (String oneAttribute : attributes) {
            		Attribute attribute = null;
            		String attributeText = oneAttribute.trim();
            		if (attributeText.length()==0) {
            			// do nothing
            		} else if (attributeText.startsWith("@")) {
            			attribute = lefffServiceInternal.loadOrCreateAttribute("macro", attributeText.substring(1).trim());
            		} else if (attributeText.startsWith("cat=")) {
            			// do nothing, we already have the category
            		} else {
            			int equalsPos = attributeText.indexOf('=');
            			attribute = lefffServiceInternal.loadOrCreateAttribute(attributeText.substring(0, equalsPos).trim(), attributeText.substring(equalsPos+1).trim());
            		}
            		if (attribute!=null)
            			entry.getAttributes().add(attribute);
            	} // next attribute
            	entry.save();
            }
        }
        reader.close();
	}

	public LefffServiceInternal getLefffServiceInternal() {
		return lefffServiceInternal;
	}

	public void setLefffServiceInternal(LefffServiceInternal lefffServiceInternal) {
		this.lefffServiceInternal = lefffServiceInternal;
	}

	public int getStopLine() {
		return stopLine;
	}

	public void setStopLine(int stopLine) {
		this.stopLine = stopLine;
	}

	public int getStartLine() {
		return startLine;
	}

	public void setStartLine(int startLine) {
		this.startLine = startLine;
	}


}
