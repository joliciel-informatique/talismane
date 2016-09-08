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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.utils.ArrayListNoNulls;
import com.joliciel.talismane.utils.LogUtils;

public class TokenSequenceFilterFactory {
	public static final String TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY = "token_sequence_filter";

	private static final Logger LOG = LoggerFactory.getLogger(TokenSequenceFilterFactory.class);
	private final TalismaneSession talismaneSession;
	private final List<Class<? extends TokenSequenceFilter>> availableTokenSequenceFilters = new ArrayList<>();

	private static final Map<String, TokenSequenceFilterFactory> instances = new HashMap<>();

	public static TokenSequenceFilterFactory getInstance(TalismaneSession talismaneSession) {
		TokenSequenceFilterFactory factory = instances.get(talismaneSession.getSessionId());
		if (factory == null) {
			factory = new TokenSequenceFilterFactory(talismaneSession);
			instances.put(talismaneSession.getSessionId(), factory);
		}
		return factory;
	}

	TokenSequenceFilterFactory(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
	}

	/**
	 * Gets a TokenSequenceFilter corresponding to a given descriptor. The
	 * descriptor should contain the class name, followed by any arguments,
	 * separated by tabs.
	 */
	public TokenSequenceFilter getTokenSequenceFilter(String descriptor) {
		TokenSequenceFilter filter = null;
		List<Class<? extends TokenSequenceFilter>> classes = new ArrayListNoNulls<Class<? extends TokenSequenceFilter>>();
		classes.addAll(this.availableTokenSequenceFilters);
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
			((NeedsTalismaneSession) filter).setTalismaneSession(talismaneSession);
		}

		if (filter == null) {
			throw new TalismaneException("Unknown TokenSequenceFilter: " + descriptor);
		}

		return filter;
	}

	/**
	 * A list of token sequence filters to which any new filters can be added.
	 * 
	 * @return
	 */
	public List<Class<? extends TokenSequenceFilter>> getAvailableTokenSequenceFilters() {
		return availableTokenSequenceFilters;
	}
}
