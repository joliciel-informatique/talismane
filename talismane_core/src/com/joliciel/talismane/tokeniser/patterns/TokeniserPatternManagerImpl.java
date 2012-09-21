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
package com.joliciel.talismane.tokeniser.patterns;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.ArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.tokeniser.SeparatorDecision;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserService;

class TokeniserPatternManagerImpl implements TokeniserPatternManager {
	private static final Log LOG = LogFactory.getLog(TokeniserPatternManagerImpl.class);
	
	private TokeniserService tokeniserService;
	private TokeniserPatternService tokeniserPatternService;
	
	private Map<SeparatorDecision, String> separatorDefaults;
	private List<String> testPatterns;
	
	private List<TokenPattern> parsedTestPatterns;
	
	/**
	 * Reads separator defaults and test patterns from the default file for this locale.
	 * @param locale
	 */
	public TokeniserPatternManagerImpl(Locale locale) {
        InputStream patternFileStream = this.getClass().getResourceAsStream("/com/joliciel/talismane/tokeniser/patterns/tokeniserTestPatterns_" + locale.getLanguage() + ".txt"); 
        Scanner scanner = new Scanner(patternFileStream);
        
        List<String> patternDescriptors = new ArrayList<String>();
        while (scanner.hasNextLine()) {
        	String line = scanner.nextLine();
        	patternDescriptors.add(line);
        }
		this.configure(patternDescriptors);
	}
	
	/**
	 * Reads separator defaults and test patterns from a list of strings.
	 * @param locale
	 */
	public TokeniserPatternManagerImpl (List<String> patternDescriptors) {
		this.configure(patternDescriptors);
	}

	private void configure(List<String> patternDescriptors) {
    	String[] separatorDecisions = new String[] {
    			SeparatorDecision.IS_NOT_SEPARATOR.toString(),
    			SeparatorDecision.IS_SEPARATOR_AFTER.toString(),
    			SeparatorDecision.IS_SEPARATOR_BEFORE.toString() };
    	List<String> testPatterns = new ArrayList<String>();
        Map<SeparatorDecision, String> separatorDefaults = new HashMap<SeparatorDecision, String>();
        for (String line : patternDescriptors) {
        	if (line.startsWith("#"))
        		continue;
        	boolean lineProcessed = false;
        	for (String separatorDecision : separatorDecisions) {
	        	if (line.startsWith(separatorDecision)) {
	        		if (line.length() > separatorDecision.length()+1) {
		        		String separatorsForDefault = line.substring(separatorDecision.length()+1);
		        		if (LOG.isTraceEnabled())
		        			LOG.trace(separatorDecision + ": '" + separatorsForDefault + "'");
		        		if (separatorsForDefault.length()>0)
		        			separatorDefaults.put(Enum.valueOf(SeparatorDecision.class, separatorDecision), separatorsForDefault);
	        		}
	        		lineProcessed = true;
	        		break;
	        	}
        	}
        	if (lineProcessed)
        		continue;
        	if (line.trim().length()>0)
        		testPatterns.add(line.trim());
        }
        this.separatorDefaults = separatorDefaults;
        this.testPatterns = testPatterns;
	}
	
	/* (non-Javadoc)
	 * @see com.joliciel.talismane.tokeniser.TokeniserPatternFileProcessor#getSeparatorDefaults()
	 */
	@Override
	public Map<SeparatorDecision, String> getSeparatorDefaults() {
		return separatorDefaults;
	}

	/* (non-Javadoc)
	 * @see com.joliciel.talismane.tokeniser.TokeniserPatternFileProcessor#getTestPatterns()
	 */
	@Override
	public List<String> getTestPatterns() {
		return testPatterns;
	}

	public List<TokenPattern> getParsedTestPatterns() {
		if (this.parsedTestPatterns==null&&this.testPatterns!=null) {
			this.parsedTestPatterns = new ArrayList<TokenPattern>();
			for (String testPattern : this.testPatterns) {
				String pattern = testPattern;
				String name = null;
				int tabPos = testPattern.indexOf('\t');
				if (tabPos>0) {
					name = testPattern.substring(0, tabPos);
					pattern = testPattern.substring(tabPos+1);
				}
				TokenPattern parsedPattern = this.getTokeniserPatternService().getTokeniserPattern(pattern, Tokeniser.SEPARATORS);
				if (name!=null)
					parsedPattern.setName(name);
				this.parsedTestPatterns.add(parsedPattern);
			}
		}
		return parsedTestPatterns;
	}
	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}
	public void setTokeniserService(
			TokeniserService tokeniserServiceInternal) {
		this.tokeniserService = tokeniserServiceInternal;
	}

	public TokeniserPatternService getTokeniserPatternService() {
		return tokeniserPatternService;
	}

	public void setTokeniserPatternService(
			TokeniserPatternService tokeniserPatternService) {
		this.tokeniserPatternService = tokeniserPatternService;
	}
	
	
}
