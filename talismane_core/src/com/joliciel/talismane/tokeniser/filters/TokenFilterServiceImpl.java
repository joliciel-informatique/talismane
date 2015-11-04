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
package com.joliciel.talismane.tokeniser.filters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.LanguageImplementation;
import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.utils.ArrayListNoNulls;
import com.joliciel.talismane.utils.LogUtils;

class TokenFilterServiceImpl implements TokenFilterServiceInternal {
	private static final Log LOG = LogFactory.getLog(TokenFilterServiceImpl.class);
	
	private TalismaneService talismaneService;
	private MachineLearningService machineLearningService;
	private ExternalResourceFinder externalResourceFinder;
	
	@Override
	public TokenPlaceholder getTokenPlaceholder(int startIndex, int endIndex,
			String replacement, String regex) {
		TokenPlaceholderImpl placeholder = new TokenPlaceholderImpl();
		placeholder.setStartIndex(startIndex);
		placeholder.setEndIndex(endIndex);
		placeholder.setReplacement(replacement);
		placeholder.setRegex(regex);
		return placeholder;
	}


	@Override
	public TokenRegexFilter getTokenRegexFilter(String regex) {
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl(regex);
		filter.setTokeniserFilterService(this);
		filter.setExternalResourceFinder(this.getExternalResourceFinder());
		return filter;
	}
	
	@Override
	public AttributeRegexFilter getAttributeRegexFilter(String regex) {
		AttributeRegexFilterImpl filter = new AttributeRegexFilterImpl(regex);
		filter.setTokeniserFilterService(this);
		filter.setExternalResourceFinder(this.getExternalResourceFinder());
		return filter;
	}

	@Override
	public TokenSequenceFilter getTokenSequenceFilter(String descriptor) {
		TokenSequenceFilter filter = null;
		List<Class<? extends TokenSequenceFilter>> classes = new ArrayListNoNulls<Class<? extends TokenSequenceFilter>>();
		LanguageImplementation implementation = talismaneService.getTalismaneSession().getImplementation();
		classes.addAll(implementation.getAvailableTokenSequenceFilters());
		classes.add(DiacriticRemover.class);
		classes.add(LowercaseFilter.class);
		classes.add(LowercaseKnownWordFilter.class);
		classes.add(LowercaseKnownFirstWordFilter.class);
		classes.add(QuoteNormaliser.class);
		classes.add(UppercaseSeriesFilter.class);
		
		for (Class<? extends TokenSequenceFilter> clazz : classes) {
			if (descriptor.equals(clazz.getSimpleName())) {
				try {
					filter = clazz.newInstance();
				} catch (InstantiationException e) {
					LogUtils.logError(LOG, e);
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					LogUtils.logError(LOG, e);
					throw new RuntimeException(e);
				}
			}
		}
		
		if (filter instanceof NeedsTalismaneSession) {
			((NeedsTalismaneSession)filter).setTalismaneSession(this.talismaneService.getTalismaneSession());
		}
		
		if (filter==null) {
			throw new TalismaneException("Unknown TokenSequenceFilter: " + descriptor);
		}
		
		return filter;
	}
	
	@Override
	public List<TokenFilter> readTokenFilters(Scanner scanner) {
		return this.readTokenFilters(scanner, null);
	}
	
	@Override
	public List<TokenFilter> readTokenFilters(Scanner scanner,
			List<String> descriptors) {
		List<TokenFilter> tokenFilters = new ArrayListNoNulls<TokenFilter>();
		Map<String,String> defaultParams = new HashMap<String, String>();
		while (scanner.hasNextLine()) {
			String descriptor = scanner.nextLine();
			LOG.debug(descriptor);
			if (descriptors!=null) {
				descriptors.add(descriptor);
			}
			if (descriptor.trim().length()==0 || descriptor.startsWith("#"))
				continue;
			if (descriptor.startsWith("DefaultParameters")) {
				defaultParams = new HashMap<String, String>();
				if (descriptor.indexOf('(')>0) {
					String parameters = descriptor.substring(descriptor.indexOf('(')+1, descriptor.indexOf(')'));
					if (parameters.length()>0) {
						String[] paramArray = parameters.split(",");
						for (String paramEntry : paramArray) {
							String paramName = paramEntry.substring(0, paramEntry.indexOf('='));
							String paramValue = paramEntry.substring(paramEntry.indexOf('=')+1);
							defaultParams.put(paramName, paramValue);
						}
					}
				}
			} else {
				Map<String,String> parameterMap = new HashMap<String, String>(defaultParams);
				TokenFilter tokenFilter = this.getTokenFilter(descriptor, parameterMap);
				tokenFilters.add(tokenFilter);
			}
		}
		// cancel the default parameters if required
		if (descriptors!=null)
			descriptors.add("DefaultParameters()");

		return tokenFilters;
	}

	@Override
	public TokenFilter getTokenFilter(String descriptor) {
		Map<String,String> parameterMap = new HashMap<String, String>();
		return this.getTokenFilter(descriptor, parameterMap);
	}
	
	public TokenFilter getTokenFilter(String descriptor, Map<String,String> parameterMap) {
		TokenRegexFilter filter = null;
		String[] parts = descriptor.split("\t");
		String className = parts[0];
		
		if (className.indexOf('(')>0) {
			String parameters = className.substring(className.indexOf('(')+1, className.indexOf(')'));
			className = className.substring(0, className.indexOf('('));
			if (parameters.length()>0) {
				String[] paramArray = parameters.split(",");
				for (String paramEntry : paramArray) {
					String paramName = paramEntry.substring(0, paramEntry.indexOf('='));
					String paramValue = paramEntry.substring(paramEntry.indexOf('=')+1);
					parameterMap.put(paramName, paramValue);
				}
			}
		}
		
		if (className.equals(TokenRegexFilter.class.getSimpleName())) {
			if (parts.length<2 || parts.length>3)
				throw new TalismaneException("Wrong number of tabs for " + TokenRegexFilter.class.getSimpleName() + ". Expected 2 or 3, but was " + parts.length);
			filter = this.getTokenRegexFilter(parts[1]);
			if (parts.length==3) {
				filter.setReplacement(parts[2]);
			}
		} else if (className.equals(AttributeRegexFilter.class.getSimpleName())) {
			if (parts.length!=2)
				throw new TalismaneException("Wrong number of tabs for " + AttributeRegexFilter.class.getSimpleName() + ". Expected 2, but was " + parts.length);
			filter = this.getAttributeRegexFilter(parts[1]);
		} else {
			throw new TalismaneException("Unknown TokenFilter: " + className);
		}
		
		for (String paramName : parameterMap.keySet()) {
			String paramValue = parameterMap.get(paramName);
			if (paramName.equals("possibleSentenceBoundary")) {
				filter.setPossibleSentenceBoundary(Boolean.valueOf(paramValue));
			} else if (paramName.equals("group")) {
				filter.setGroupIndex(Integer.parseInt(paramValue));
			} else if (paramName.equals("caseSensitive")) {
				filter.setCaseSensitive(Boolean.valueOf(paramValue));
			} else if (paramName.equals("diacriticSensitive")) {
				filter.setDiacriticSensitive(Boolean.valueOf(paramValue));
			} else if (paramName.equals("autoWordBoundaries")) {
				filter.setAutoWordBoundaries(Boolean.valueOf(paramValue));
			} else {
				filter.addAttribute(paramName, paramValue);
			}
		}
			
		// verify that the filter can work correctly
		filter.verify();

		return filter;
	}

	@Override
	public TokenSequenceFilter getTokenSequenceFilter(
			List<TokenFilter> tokenFilters) {
		TokenFilterWrapper wrapper = new TokenFilterWrapper(tokenFilters);
		return wrapper;
	}

	public TalismaneService getTalismaneService() {
		return talismaneService;
	}

	public void setTalismaneService(TalismaneService talismaneService) {
		this.talismaneService = talismaneService;
	}
	
	public ExternalResourceFinder getExternalResourceFinder() {
		if (this.externalResourceFinder==null) {
			this.externalResourceFinder = this.machineLearningService.getExternalResourceFinder();
		}
		return externalResourceFinder;
	}

	public void setExternalResourceFinder(
			ExternalResourceFinder externalResourceFinder) {
		this.externalResourceFinder = externalResourceFinder;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}
}
