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
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class Lefff3LoaderImpl implements LefffLoader {
    private static final Log LOG = LogFactory.getLog(Lefff3LoaderImpl.class);

    private LefffServiceInternal lefffServiceInternal;
	private int stopLine;
	private int startLine;
	
	Pattern noUnderscore = Pattern.compile("[^_]");
	
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
            
            //affaliez	100	v	[pred="s'affaler_____1<Suj:cln|sn,Loc:(loc-sn|y)>",@pers,@pron,@être,cat=v,@I2p]	affaler_____1	Default	I2p	%actif	v-er:std

            if (!line.startsWith("#") && line.length()>0 && !line.startsWith("_")) {
	            String[] tokens = line.split("\t");
	
            	String wordText = tokens[0].trim();
            	Word word = lefffServiceInternal.loadOrCreateWord(wordText);
                	
            	String categoryCode = tokens[2].trim();
            	Category category = lefffServiceInternal.loadOrCreateCategory(categoryCode);
            	
            	Lemma lemma = null;
            	String lemmaString = tokens[4].trim();
            	String lemmaComplement = "";
            	int lemmaIndex = 1;
            	int underscorePos = lemmaString.indexOf("_");
            	if (underscorePos<0) {
            		// we have an unusual predicate
               		lemma = lefffServiceInternal.loadOrCreateLemma(wordText, 1, "");
             	} else {
            		// we have a proper lemma
            		
	            	String lemmaText = lemmaString.substring(0, underscorePos);
	            	LOG.trace(lemmaText);
	            	
//	            	int complementPos = underscorePos;
//	            	for (int j=underscorePos; j<lemmaString.length();j++) {
//	            		char c = lemmaString.charAt(j);
//	            		if (c!='_') {
//	            			complementPos = j;
//	            			break;
//	            		}
//	            	}
//	            	lemmaComplement = lemmaString.substring(complementPos);
//	            	lemmaIndex = 1;
//	            	try {
//	            		lemmaIndex = Integer.parseInt(lemmaComplement);
//	            		lemmaComplement = "";
//	            	} catch (NumberFormatException nfe) {
//	            		lemmaComplement = lemmaComplement.substring(0, lemmaComplement.indexOf('_'));
//	            		lemmaIndex = Integer.parseInt(lemmaString.substring(lemmaString.lastIndexOf('_')+1));
//	            	}
	            	

	            	lemma = lefffServiceInternal.loadOrCreateLemma(lemmaText, lemmaIndex, lemmaComplement);
            	}
            	
            	LefffEntryInternal entry = lefffServiceInternal.loadOrCreateEntry(word, lemma, category);
              	
            	String lexicalWeightText = tokens[1].trim();
            	if (lexicalWeightText.length()>0) {
            		int lexicalWeight = Integer.parseInt(lexicalWeightText);
            		entry.setLexicalWeight(lexicalWeight);
 	            }
             	
            	
            	String morphoSyntax = tokens[6];
            	Attribute attribute = lefffServiceInternal.loadOrCreateAttribute("macro", morphoSyntax, true);
            	
            	if (entry.getMorphologyId()!=0) {
            		boolean foundAttr = false;
            		for (Attribute oneAttr : entry.getAttributes()) {
            			if (oneAttr.getValue().equals(attribute.getValue())) {
            				foundAttr = true;
            				break;
                  		}
            		}
            		
            		if (!foundAttr) {
              			entry.getAttributes().add(attribute);
            			entry.save();
            		}
            		
            	} else {
            		entry.getAttributes().add(attribute);
            		entry.setMorphology(attribute);
            		entry.save();
            	}
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
