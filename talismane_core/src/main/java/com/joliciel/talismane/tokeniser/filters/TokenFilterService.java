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

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Scanner;

import com.joliciel.talismane.machineLearning.ExternalResourceFinder;

public interface TokenFilterService {
	public static final String TOKEN_FILTER_DESCRIPTOR_KEY = "token_filter";
	public static final String TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY = "token_sequence_filter";

	/**
	 * Return a filter for the regex provided.
	 */
	public TokenRegexFilter getTokenRegexFilter(String regex);

	/**
	 * Return a filter for the regex and replacement provided.
	 */
	public TokenRegexFilter getTokenRegexFilter(String regex, String replacement);

	/**
	 * Gets a TokenSequenceFilter corresponding to a given descriptor. The
	 * descriptor should contain the class name, followed by any arguments,
	 * separated by tabs.
	 */
	public TokenSequenceFilter getTokenSequenceFilter(String descriptor);

	/**
	 * Gets a TokenFilter corresponding to a given descriptor. The descriptor
	 * should contain the class name, followed by any arguments, separated by
	 * tabs.
	 */
	public TokenFilter getTokenFilter(String descriptor) throws TokenFilterLoadException;

	/**
	 * Reads a sequence of token filters from a scanner.
	 */
	public List<TokenFilter> readTokenFilters(Scanner scanner);

	/**
	 * Reads a sequence of token filters from a file, and stores their
	 * descriptors in the provided paramater.
	 */
	public List<TokenFilter> readTokenFilters(Scanner scanner, List<String> descriptors);

	/**
	 * Reads a sequence of token filters from a scanner, with a path providing
	 * clean error reporting.
	 */
	public List<TokenFilter> readTokenFilters(Scanner scanner, String path, List<String> descriptors);

	/**
	 * Similar to {@link TokenFilterService#readTokenFilters(Scanner, List)},
	 * but keeps a reference to the file, useful for finding the location of any
	 * descriptor errors.
	 * 
	 * @param file
	 *            the file to be read
	 * @param charset
	 *            the charset used to read the file
	 * @param descriptors
	 *            a list of descriptors in which we store the descriptors added
	 *            from this file
	 */
	public List<TokenFilter> readTokenFilters(File file, Charset charset, List<String> descriptors);

	/**
	 * Get a TokenSequenceFilter that wraps a list of token filters. While it
	 * won't re-assign any token boundaries, it will check each TokenFilter
	 * against each individual token, and if a match is found, will replace the
	 * text.
	 */
	public TokenSequenceFilter getTokenSequenceFilter(List<TokenFilter> tokenFilters);

	/**
	 * Set the external resource finder to be use by the token filters.
	 */
	public void setExternalResourceFinder(ExternalResourceFinder externalResourceFinder);

	public ExternalResourceFinder getExternalResourceFinder();

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
	public void registerTokenFilterType(String name, Class<? extends TokenFilter> type);

	/**
	 * Like {@link #registerTokenFilterType(String, Class)}, but with an
	 * additional dependency injector argument to inject dependencies prior to
	 * loading.
	 * 
	 * @param dependencyInjector
	 *            the dependency injector for this class - any TokenFilter
	 *            assignable to this class will receive calls for this injector.
	 */
	public void registerTokenFilterType(String name, Class<? extends TokenFilter> type, TokenFilterDependencyInjector dependencyInjector);

	/**
	 * A list of token sequence filters to which any new filters can be added.
	 * 
	 * @return
	 */
	List<Class<? extends TokenSequenceFilter>> getAvailableTokenSequenceFilters();
}
