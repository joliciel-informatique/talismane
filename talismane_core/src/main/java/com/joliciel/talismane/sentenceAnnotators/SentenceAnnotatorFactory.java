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
package com.joliciel.talismane.sentenceAnnotators;

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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.utils.LogUtils;

public class SentenceAnnotatorFactory {
	public static final String TOKEN_FILTER_DESCRIPTOR_KEY = "token_filter";

	private static final Logger LOG = LoggerFactory.getLogger(SentenceAnnotatorFactory.class);

	private final TalismaneSession talismaneSession;
	private final Map<String, Class<? extends SentenceAnnotator>> registeredFilterTypes = new HashMap<>();
	private final Map<Class<? extends SentenceAnnotator>, AnnotatorDependencyInjector> registeredDependencyInjectors = new HashMap<>();
	private final Map<String, Class<? extends TextReplacer>> registeredTextReplacers = new HashMap<>();

	private static final Map<String, SentenceAnnotatorFactory> instances = new HashMap<>();

	public static SentenceAnnotatorFactory getInstance(TalismaneSession talismaneSession) {
		SentenceAnnotatorFactory factory = instances.get(talismaneSession.getSessionId());
		if (factory == null) {
			factory = new SentenceAnnotatorFactory(talismaneSession);
			instances.put(talismaneSession.getSessionId(), factory);
		}
		return factory;
	}

	private SentenceAnnotatorFactory(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
		registeredFilterTypes.put(AttributeRegexFilter.class.getSimpleName(), AttributeRegexFilter.class);
		registeredFilterTypes.put(TokenRegexFilter.class.getSimpleName(), TokenRegexFilterImpl.class);
		registeredFilterTypes.put(TextReplaceFilter.class.getSimpleName(), TextReplaceFilter.class);

		registeredTextReplacers.put(DiacriticRemover.class.getSimpleName(), DiacriticRemover.class);
		registeredTextReplacers.put(LowercaseFilter.class.getSimpleName(), LowercaseFilter.class);
		registeredTextReplacers.put(LowercaseKnownFirstWordFilter.class.getSimpleName(), LowercaseKnownFirstWordFilter.class);
		registeredTextReplacers.put(LowercaseKnownWordFilter.class.getSimpleName(), LowercaseKnownWordFilter.class);
		registeredTextReplacers.put(QuoteNormaliser.class.getSimpleName(), QuoteNormaliser.class);
		registeredTextReplacers.put(UppercaseSeriesFilter.class.getSimpleName(), UppercaseSeriesFilter.class);

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
	public List<Pair<SentenceAnnotator, String>> readTokenFilters(Scanner scanner) {
		return this.readTokenFilters(scanner, null);
	}

	/**
	 * Similar to {@link #readTokenFilters(Scanner)}, but keeps a reference to
	 * the file, useful for finding the location of any descriptor errors.
	 * 
	 * @param file
	 *            the file to be read
	 * @param charset
	 *            the charset used to read the file
	 */
	public List<Pair<SentenceAnnotator, String>> readTokenFilters(File file, Charset charset) {
		try {
			try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(file), charset)))) {
				return this.readTokenFilters(scanner, file.getCanonicalPath());
			}
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reads a sequence of token filters from a scanner, with a path providing
	 * clean error reporting.
	 */
	public List<Pair<SentenceAnnotator, String>> readTokenFilters(Scanner scanner, String path) {
		List<Pair<SentenceAnnotator, String>> tokenFilters = new ArrayList<>();
		Map<String, String> defaultParams = new HashMap<String, String>();
		int lineNumber = 0;
		while (scanner.hasNextLine()) {
			String descriptor = scanner.nextLine();
			lineNumber++;
			LOG.debug(descriptor);

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
					Pair<SentenceAnnotator, String> tokenFilterPair = this.getTokenFilter(descriptor, defaultParams);
					if (tokenFilterPair != null)
						tokenFilters.add(tokenFilterPair);
				} catch (SentenceAnnotatorLoadException e) {
					if (path != null)
						throw new TalismaneException("Unable to parse file " + path + ", line " + lineNumber + ": " + descriptor, e);
					throw new TalismaneException("Unable to parse line " + lineNumber + ": " + descriptor, e);
				}
			}
		}

		return tokenFilters;
	}

	/**
	 * Gets a TokenFilter corresponding to a given descriptor. The descriptor
	 * should contain the class name, followed by any arguments, separated by
	 * tabs.
	 */
	public Pair<SentenceAnnotator, String> getTokenFilter(String descriptor) throws SentenceAnnotatorLoadException {
		Map<String, String> parameterMap = new HashMap<String, String>();
		return this.getTokenFilter(descriptor, parameterMap);
	}

	public Pair<SentenceAnnotator, String> getTokenFilter(String descriptor, Map<String, String> defaultParams) throws SentenceAnnotatorLoadException {
		try {
			Map<String, String> myParams = new HashMap<>(defaultParams);

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

			Class<? extends SentenceAnnotator> clazz = this.registeredFilterTypes.get(className);
			if (clazz == null) {
				throw new SentenceAnnotatorLoadException("Unknown TokenFilter: " + className);
			}

			SentenceAnnotator filter = null;
			if (clazz.equals(TextReplaceFilter.class)) {
				filter = new TextReplaceFilter(registeredTextReplacers, talismaneSession);
			} else {
				filter = clazz.newInstance();
			}

			if (filter instanceof NeedsTalismaneSession) {
				((NeedsTalismaneSession) filter).setTalismaneSession(talismaneSession);
			}

			for (Entry<Class<? extends SentenceAnnotator>, AnnotatorDependencyInjector> entry : this.registeredDependencyInjectors.entrySet()) {
				Class<? extends SentenceAnnotator> tokenFilterClass = entry.getKey();
				AnnotatorDependencyInjector dependencyInjector = entry.getValue();
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

			// construct descriptor including default parameters
			StringBuilder sb = new StringBuilder();
			sb.append(className);
			sb.append('(');
			boolean first = true;
			for (String key : myParams.keySet()) {
				if (!first)
					sb.append(',');
				sb.append(key);
				sb.append('=');
				sb.append(myParams.get(key));
				first = false;
			}
			sb.append(')');
			for (String tab : tabs) {
				sb.append('\t');
				sb.append(tab);
			}

			return new ImmutablePair<>(filter, sb.toString());
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
	public void registerTokenFilterType(String name, Class<? extends SentenceAnnotator> type) {
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
	public void registerTokenFilterType(String name, Class<? extends SentenceAnnotator> type, AnnotatorDependencyInjector dependencyInjector) {
		this.registeredFilterTypes.put(name, type);
		this.registeredDependencyInjectors.put(type, dependencyInjector);
	}

}
