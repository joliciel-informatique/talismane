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
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.utils.ArrayListNoNulls;
import com.joliciel.talismane.utils.LogUtils;

public class TokenFilterFactory {
	public static final String TOKEN_FILTER_DESCRIPTOR_KEY = "token_filter";

	private static final Logger LOG = LoggerFactory.getLogger(TokenFilterFactory.class);

	private final TalismaneSession talismaneSession;
	private final Map<String, Class<? extends TokenFilter>> registeredFilterTypes = new HashMap<String, Class<? extends TokenFilter>>();
	private final Map<Class<? extends TokenFilter>, TokenFilterDependencyInjector> registeredDependencyInjectors = new HashMap<Class<? extends TokenFilter>, TokenFilterDependencyInjector>();

	private static final Map<String, TokenFilterFactory> instances = new HashMap<>();

	public static TokenFilterFactory getInstance(TalismaneSession talismaneSession) {
		TokenFilterFactory factory = instances.get(talismaneSession.getSessionId());
		if (factory == null) {
			factory = new TokenFilterFactory(talismaneSession);
			instances.put(talismaneSession.getSessionId(), factory);
		}
		return factory;
	}

	private TokenFilterFactory(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
		registeredFilterTypes.put(AttributeRegexFilter.class.getSimpleName(), AttributeRegexFilter.class);
		registeredFilterTypes.put(TokenRegexFilter.class.getSimpleName(), TokenRegexFilterImpl.class);
	}

	public TokenRegexFilter getTokenRegexFilter(String regex) {
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl(regex, talismaneSession);
		return filter;
	}

	public TokenRegexFilter getTokenRegexFilter(String regex, String replacement) {
		TokenRegexFilterImpl filter = (TokenRegexFilterImpl) this.getTokenRegexFilter(regex);
		filter.setReplacement(replacement);
		return filter;
	}

	/**
	 * Reads a sequence of token filters from a scanner.
	 */
	public List<TokenFilter> readTokenFilters(Scanner scanner) {
		return this.readTokenFilters(scanner, null);
	}

	/**
	 * Similar to {@link #readTokenFilters(Scanner, List)}, but keeps a
	 * reference to the file, useful for finding the location of any descriptor
	 * errors.
	 * 
	 * @param file
	 *            the file to be read
	 * @param charset
	 *            the charset used to read the file
	 * @param descriptors
	 *            a list of descriptors in which we store the descriptors added
	 *            from this file
	 */
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

	/**
	 * Reads a sequence of token filters from a file, and stores their
	 * descriptors in the provided paramater.
	 */
	public List<TokenFilter> readTokenFilters(Scanner scanner, List<String> descriptors) {
		return this.readTokenFilters(scanner, null, descriptors);
	}

	/**
	 * Reads a sequence of token filters from a scanner, with a path providing
	 * clean error reporting.
	 */
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

	/**
	 * Gets a TokenFilter corresponding to a given descriptor. The descriptor
	 * should contain the class name, followed by any arguments, separated by
	 * tabs.
	 */
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

			if (NeedsTalismaneSession.class.isAssignableFrom(filter.getClass())) {
				((NeedsTalismaneSession) filter).setTalismaneSession(talismaneSession);
			}

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

	/**
	 * Register a token filter class that can be loaded using
	 * {@link #readTokenFilters(Scanner)}. The type must include a default
	 * constructor.
	 * 
	 * @param name
	 *            the name used to recognise this class
	 * @param type
	 *            the class to be instantiated
	 */
	public void registerTokenFilterType(String name, Class<? extends TokenFilter> type) {
		this.registeredFilterTypes.put(name, type);
	}

	/**
	 * Like {@link #registerTokenFilterType(String, Class)}, but with an
	 * additional dependency injector argument to inject dependencies prior to
	 * loading.
	 * 
	 * @param dependencyInjector
	 *            the dependency injector for this class - any TokenFilter
	 *            assignable to this class will receive calls for this injector.
	 */
	public void registerTokenFilterType(String name, Class<? extends TokenFilter> type, TokenFilterDependencyInjector dependencyInjector) {
		this.registeredFilterTypes.put(name, type);
		this.registeredDependencyInjectors.put(type, dependencyInjector);
	}

}
