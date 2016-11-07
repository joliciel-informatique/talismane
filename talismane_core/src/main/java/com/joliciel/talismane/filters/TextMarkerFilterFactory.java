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
package com.joliciel.talismane.filters;

import java.lang.reflect.Constructor;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.tokeniser.StringAttribute;
import com.joliciel.talismane.utils.ArrayListNoNulls;

public class TextMarkerFilterFactory {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(TextMarkerFilterFactory.class);

	public TextMarkerFilter getTextMarkerFilter(String descriptor, int blockSize) {
		TextMarkerFilter filter = null;

		List<Class<? extends TextMarkerFilter>> classes = new ArrayListNoNulls<Class<? extends TextMarkerFilter>>();
		classes.add(DuplicateWhiteSpaceFilter.class);
		classes.add(NewlineEndOfSentenceMarker.class);
		classes.add(NewlineSpaceMarker.class);

		String[] parts = descriptor.split("\t");
		String filterName = parts[0];

		if (filterName.equals(RegexMarkerFilter.class.getSimpleName())) {
			String[] filterTypeStrings = parts[1].split(",");
			List<MarkerFilterType> filterTypes = new ArrayListNoNulls<MarkerFilterType>();
			for (String filterTypeString : filterTypeStrings) {
				filterTypes.add(MarkerFilterType.valueOf(filterTypeString));
			}
			boolean needsReplacement = false;
			boolean needsTag = false;
			int minParams = 3;
			if (filterTypes.contains(MarkerFilterType.REPLACE)) {
				needsReplacement = true;
				minParams = 4;
			} else if (filterTypes.contains(MarkerFilterType.TAG)) {
				needsTag = true;
				minParams = 4;
			}
			if (parts.length == minParams + 1) {
				filter = new RegexMarkerFilter(filterTypes, parts[2], Integer.parseInt(parts[3]), blockSize);
				if (needsReplacement)
					filter.setReplacement(parts[4]);
				if (needsTag) {
					if (parts[4].indexOf('=') >= 0) {
						String attribute = parts[4].substring(0, parts[4].indexOf('='));
						String value = parts[4].substring(parts[4].indexOf('=') + 1);
						filter.setTag(attribute, new StringAttribute(attribute, value));
					} else {
						filter.setTag(parts[4], new StringAttribute(parts[4], ""));
					}
				}
			} else if (parts.length == minParams) {
				filter = new RegexMarkerFilter(filterTypes, parts[2], 0, blockSize);
				if (needsReplacement)
					filter.setReplacement(parts[3]);
				if (needsTag) {
					if (parts[3].indexOf('=') >= 0) {
						String attribute = parts[3].substring(0, parts[3].indexOf('='));
						String value = parts[3].substring(parts[3].indexOf('=') + 1);
						filter.setTag(attribute, new StringAttribute(attribute, value));
					} else {
						filter.setTag(parts[3], new StringAttribute(parts[4], ""));
					}
				}
			} else {
				throw new TalismaneException("Wrong number of arguments for " + RegexMarkerFilter.class.getSimpleName() + ". Expected " + minParams + " or "
						+ (minParams + 1) + ", but was " + parts.length);
			}
		} else {
			for (Class<? extends TextMarkerFilter> clazz : classes) {
				if (filterName.equals(clazz.getSimpleName())) {
					try {
						Constructor<? extends TextMarkerFilter> constructor = clazz.getConstructor(Integer.class);
						filter = constructor.newInstance(blockSize);
					} catch (ReflectiveOperationException e) {
						throw new TalismaneException("Problem building class: " + filterName, e);
					}
				}
			}
			if (filter == null)
				throw new TalismaneException("Unknown text filter class: " + filterName);
		}

		return filter;
	}

}
