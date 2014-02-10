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
package com.joliciel.talismane.tokeniser.patterns;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.tokeniser.SeparatorDecision;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.TokeniserService;

class TokeniserPatternManagerImpl implements TokeniserPatternManager {
	private static final Log LOG = LogFactory.getLog(TokeniserPatternManagerImpl.class);
	
	private TokeniserService tokeniserService;
	private TokeniserPatternService tokeniserPatternService;
	
	private Map<SeparatorDecision, String> separatorDefaults;
	private Map<SeparatorDecision, Pattern> separatorDefaultPatterns;
	private List<String> testPatterns;
	
	private List<TokenPattern> parsedTestPatterns;
	
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
				String groupName = null;
				String[] parts = testPattern.split("\t");
				if (parts.length==2) {
					name = parts[0];
					pattern = parts[1];
				} else if (parts.length==3) {
					name = parts[0];
					groupName = parts[1];
					pattern = parts[2];
				}
				
				TokenPattern parsedPattern = this.getTokeniserPatternService().getTokeniserPattern(pattern, Tokeniser.SEPARATORS);
				if (name!=null)
					parsedPattern.setName(name);
				if (groupName!=null)
					parsedPattern.setGroupName(groupName);
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
	
	public List<TokeniserOutcome> getDefaultOutcomes(TokenSequence tokenSequence) {
		List<TokeniserOutcome> defaultOutcomes = new ArrayList<TokeniserOutcome>();
		
		// Assign each separator its default value
		TokeniserOutcome nextOutcome = TokeniserOutcome.SEPARATE;
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			
			TokeniserOutcome outcome = null;
			if (Tokeniser.SEPARATORS.matcher(token.getText()).matches()) {
				boolean defaultValueFound = false;
				for (Entry<SeparatorDecision, Pattern> entry : this.getSeparatorDefaultPatterns().entrySet()) {
					if (entry.getValue().matcher(token.getText()).matches()) {
						defaultValueFound = true;
						SeparatorDecision defaultSeparatorDecision = entry.getKey();
						switch (defaultSeparatorDecision) {
						case IS_SEPARATOR:
							outcome = TokeniserOutcome.SEPARATE;
							nextOutcome = TokeniserOutcome.SEPARATE;
							break;
						case IS_NOT_SEPARATOR:
							outcome = TokeniserOutcome.JOIN;
							nextOutcome = TokeniserOutcome.JOIN;
							break;
						case IS_SEPARATOR_BEFORE:
							outcome = TokeniserOutcome.SEPARATE;
							nextOutcome = TokeniserOutcome.JOIN;
						case IS_SEPARATOR_AFTER:
							outcome = TokeniserOutcome.JOIN;
							nextOutcome = TokeniserOutcome.SEPARATE;
						}
						break;
					}
				}
				if (!defaultValueFound) {
					outcome = TokeniserOutcome.SEPARATE;
					nextOutcome = TokeniserOutcome.SEPARATE;
				}
				defaultOutcomes.add(outcome);
			} else {
				defaultOutcomes.add(nextOutcome);
			}

		}
		return defaultOutcomes;
	}
	

	public void setSeparatorDefaults(
			Map<SeparatorDecision, String> separatorDefaults) {
		this.separatorDefaults = separatorDefaults;
	}

	protected Map<SeparatorDecision, Pattern> getSeparatorDefaultPatterns() {
		if (this.separatorDefaultPatterns==null) {
			this.separatorDefaultPatterns = new HashMap<SeparatorDecision, Pattern>();
			for (Entry<SeparatorDecision, String> entry : this.getSeparatorDefaults().entrySet()) {
				String separators = entry.getValue();
				StringBuilder sb = new StringBuilder();
				for (int i=0; i<separators.length(); i++) {
					char c = separators.charAt(i);
					sb.append('\\');
					sb.append(c);
				}
				Pattern pattern = Pattern.compile("[" + sb.toString() + "]");
				this.separatorDefaultPatterns.put(entry.getKey(), pattern);
			}
			
		}
		return separatorDefaultPatterns;
	}
}
