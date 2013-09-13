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
package com.joliciel.talismane.tokeniser.filters;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.tokeniser.filters.french.AllUppercaseFrenchFilter;
import com.joliciel.talismane.tokeniser.filters.french.EmptyTokenAfterDuFilter;
import com.joliciel.talismane.tokeniser.filters.french.EmptyTokenBeforeDuquelFilter;
import com.joliciel.talismane.tokeniser.filters.french.LowercaseFirstWordFrenchFilter;
import com.joliciel.talismane.tokeniser.filters.french.UpperCaseSeriesFrenchFilter;
import com.joliciel.talismane.utils.LogUtils;

class TokenFilterServiceImpl implements TokenFilterServiceInternal {
	private static final Log LOG = LogFactory.getLog(TokenFilterServiceImpl.class);

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
	public TokenFilter getTokenRegexFilter(String regex,
			String replacement) {
		return this.getTokenRegexFilter(regex, 0, replacement);
	}

	@Override
	public TokenFilter getTokenRegexFilter(String regex,
			int groupIndex, String replacement) {
		TokenRegexFilter filter = new TokenRegexFilter(regex, groupIndex, replacement);
		filter.setTokeniserFilterService(this);
		return filter;
	}

	@Override
	public TokenSequenceFilter getTokenSequenceFilter(String descriptor) {
		TokenSequenceFilter filter = null;
		List<Class<? extends TokenSequenceFilter>> classes = new ArrayList<Class<? extends TokenSequenceFilter>>();
		classes.add(EmptyTokenAfterDuFilter.class);
		classes.add(EmptyTokenBeforeDuquelFilter.class);
		classes.add(LowercaseFirstWordFrenchFilter.class);
		classes.add(UpperCaseSeriesFrenchFilter.class);
		classes.add(AllUppercaseFrenchFilter.class);
		
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
		
		if (filter==null) {
			throw new TalismaneException("Unknown TokenSequenceFilter: " + descriptor);
		}
		
		return filter;
	}

	@Override
	public TokenFilter getTokenFilter(String descriptor) {
		TokenFilter filter = null;
		String[] parts = descriptor.split("\t");
		if (parts[0].equals(TokenRegexFilter.class.getSimpleName())) {
			if (parts.length==4) {
				filter = this.getTokenRegexFilter(parts[1], Integer.parseInt(parts[2]), parts[3]);
			} else if (parts.length==3) {
				filter = this.getTokenRegexFilter(parts[1], parts[2]);
			} else {
				throw new TalismaneException("Wrong number of arguments for " + TokenRegexFilter.class.getSimpleName() + ". Expected 3 or 4, but was " + parts.length);
			}
		} else {
			throw new TalismaneException("Unknown TokenFilter: " + parts[0]);
		}
		return filter;
	}

	@Override
	public TokenSequenceFilter getTokenSequenceFilter(
			List<TokenFilter> tokenFilters) {
		TokenFilterWrapper wrapper = new TokenFilterWrapper(tokenFilters);
		return wrapper;
	}
	

}
