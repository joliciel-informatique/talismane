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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.utils.ArrayListNoNulls;
import com.joliciel.talismane.utils.LogUtils;

class TokenFilterServiceImpl implements TokenFilterServiceInternal, TokenFilterDependencyInjector {
	private static final Logger LOG = LoggerFactory.getLogger(TokenFilterServiceImpl.class);

	private TalismaneService talismaneService;
	private MachineLearningService machineLearningService;
	private TokeniserService tokeniserService;
	private ExternalResourceFinder externalResourceFinder;
	private Map<String, Class<? extends TokenFilter>> registeredFilterTypes = new HashMap<String, Class<? extends TokenFilter>>();
	private Map<Class<? extends TokenFilter>, TokenFilterDependencyInjector> registeredDependencyInjectors = new HashMap<Class<? extends TokenFilter>, TokenFilterDependencyInjector>();
	private List<Class<? extends TokenSequenceFilter>> availableTokenSequenceFilters = new ArrayList<>();

	public TokenFilterServiceImpl() {
		registeredFilterTypes.put(AttributeRegexFilter.class.getSimpleName(), AttributeRegexFilter.class);
		registeredFilterTypes.put(TokenRegexFilter.class.getSimpleName(), TokenRegexFilterWithReplacement.class);
		registeredDependencyInjectors.put(AbstractRegexFilter.class, this);
	}

	@Override
	public TokenRegexFilter getTokenRegexFilter(String regex) {
		TokenRegexFilterWithReplacement filter = new TokenRegexFilterWithReplacement();
		filter.setExternalResourceFinder(this.getExternalResourceFinder());
		filter.setRegex(regex);
		return filter;
	}

	@Override
	public TokenRegexFilter getTokenRegexFilter(String regex, String replacement) {
		TokenRegexFilterWithReplacement filter = (TokenRegexFilterWithReplacement) this.getTokenRegexFilter(regex);
		filter.setReplacement(replacement);
		return filter;
	}

	@Override
	public TokenSequenceFilter getTokenSequenceFilter(String descriptor) {
		TokenSequenceFilter filter = null;
		List<Class<? extends TokenSequenceFilter>> classes = new ArrayListNoNulls<Class<? extends TokenSequenceFilter>>();
		classes.addAll(this.getAvailableTokenSequenceFilters());
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
			((NeedsTalismaneSession) filter).setTalismaneSession(this.talismaneService.getTalismaneSession());
		}

		if (filter == null) {
			throw new TalismaneException("Unknown TokenSequenceFilter: " + descriptor);
		}

		return filter;
	}

	@Override
	public List<TokenFilter> readTokenFilters(Scanner scanner) {
		return this.readTokenFilters(scanner, null);
	}

	@Override
	public List<TokenFilter> readTokenFilters(File file, Charset charset, List<String> descriptors) {
		try {
			try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(file), charset)))) {
				return this.readTokenFilters(scanner, file.getCanonicalPath(), descriptors);
			}
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<TokenFilter> readTokenFilters(Scanner scanner, List<String> descriptors) {
		return this.readTokenFilters(scanner, null, descriptors);
	}

	@Override
	public List<TokenFilter> readTokenFilters(Scanner scanner, String path, List<String> descriptors) {
		List<TokenFilter> tokenFilters = new ArrayListNoNulls<TokenFilter>();
		Map<String, String> defaultParams = new HashMap<String, String>();
		int lineNumber = 0;
		while (scanner.hasNextLine()) {
			String descriptor = scanner.nextLine();
			lineNumber++;
			LOG.debug(descriptor);
			if (descriptors != null) {
				descriptors.add(descriptor);
			}
			if (descriptor.trim().length() == 0 || descriptor.startsWith("#"))
				continue;
			if (descriptor.startsWith("DefaultParameters")) {
				defaultParams = new HashMap<String, String>();
				if (descriptor.indexOf('(') > 0) {
					String parameters = descriptor.substring(descriptor.indexOf('(') + 1, descriptor.indexOf(')'));
					if (parameters.length() > 0) {
						String[] paramArray = parameters.split(",");
						for (String paramEntry : paramArray) {
							String paramName = paramEntry.substring(0, paramEntry.indexOf('='));
							String paramValue = paramEntry.substring(paramEntry.indexOf('=') + 1);
							defaultParams.put(paramName, paramValue);
						}
					}
				}
			} else {
				try {
					TokenFilter tokenFilter = this.getTokenFilter(descriptor, defaultParams);
					if (tokenFilter != null)
						tokenFilters.add(tokenFilter);
				} catch (TokenFilterLoadException e) {
					if (path != null)
						throw new TalismaneException("Unable to parse file " + path + ", line " + lineNumber + ": " + descriptor, e);
					throw new TalismaneException("Unable to parse line " + lineNumber + ": " + descriptor, e);
				}
			}
		}
		// cancel the default parameters if required
		if (descriptors != null)
			descriptors.add("DefaultParameters()");

		return tokenFilters;
	}

	@Override
	public TokenFilter getTokenFilter(String descriptor) throws TokenFilterLoadException {
		Map<String, String> parameterMap = new HashMap<String, String>();
		return this.getTokenFilter(descriptor, parameterMap);
	}

	public TokenFilter getTokenFilter(String descriptor, Map<String, String> defaultParams) throws TokenFilterLoadException {
		try {
			Map<String, String> myParams = new HashMap<String, String>(defaultParams);

			String[] parts = descriptor.split("\t");
			String className = parts[0];

			if (className.indexOf('(') > 0) {
				String parameters = className.substring(className.indexOf('(') + 1, className.indexOf(')'));
				className = className.substring(0, className.indexOf('('));
				if (parameters.length() > 0) {
					String[] paramArray = parameters.split(",");
					for (String paramEntry : paramArray) {
						String paramName = paramEntry.substring(0, paramEntry.indexOf('='));
						String paramValue = paramEntry.substring(paramEntry.indexOf('=') + 1);
						myParams.put(paramName, paramValue);
					}
				}
			}

			Class<? extends TokenFilter> clazz = this.registeredFilterTypes.get(className);
			if (clazz == null) {
				throw new TokenFilterLoadException("Unknown TokenFilter: " + className);
			}
			TokenFilter filter = clazz.newInstance();

			for (Entry<Class<? extends TokenFilter>, TokenFilterDependencyInjector> entry : this.registeredDependencyInjectors.entrySet()) {
				Class<? extends TokenFilter> tokenFilterClass = entry.getKey();
				TokenFilterDependencyInjector dependencyInjector = entry.getValue();
				if (tokenFilterClass.isAssignableFrom(clazz)) {
					dependencyInjector.injectDependencies(filter);
				}
			}

			List<String> tabs = new ArrayList<String>(parts.length - 1);
			for (int i = 1; i < parts.length; i++)
				tabs.add(parts[i]);
			filter.load(myParams, tabs);

			if (filter.isExcluded())
				return null;
			return filter;
		} catch (InstantiationException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void injectDependencies(TokenFilter tokenFilter) {
		if (AbstractRegexFilter.class.isAssignableFrom(tokenFilter.getClass())) {
			AbstractRegexFilter abstractRegexFilter = (AbstractRegexFilter) tokenFilter;
			abstractRegexFilter.setExternalResourceFinder(this.getExternalResourceFinder());
		}
	}

	@Override
	public TokenSequenceFilter getTokenSequenceFilter(List<TokenFilter> tokenFilters) {
		TokenFilterWrapper wrapper = new TokenFilterWrapper(tokenFilters);
		return wrapper;
	}

	public TalismaneService getTalismaneService() {
		return talismaneService;
	}

	public void setTalismaneService(TalismaneService talismaneService) {
		this.talismaneService = talismaneService;
	}

	@Override
	public ExternalResourceFinder getExternalResourceFinder() {
		if (this.externalResourceFinder == null) {
			this.externalResourceFinder = this.machineLearningService.getExternalResourceFinder();
		}
		return externalResourceFinder;
	}

	@Override
	public void setExternalResourceFinder(ExternalResourceFinder externalResourceFinder) {
		this.externalResourceFinder = externalResourceFinder;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	@Override
	public void registerTokenFilterType(String name, Class<? extends TokenFilter> type) {
		this.registeredFilterTypes.put(name, type);
	}

	@Override
	public void registerTokenFilterType(String name, Class<? extends TokenFilter> type, TokenFilterDependencyInjector dependencyInjector) {
		this.registeredFilterTypes.put(name, type);
		this.registeredDependencyInjectors.put(type, dependencyInjector);
	}

	@Override
	public List<Class<? extends TokenSequenceFilter>> getAvailableTokenSequenceFilters() {
		return availableTokenSequenceFilters;
	}

}
