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
package com.joliciel.talismane.posTagger.filters;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.utils.ArrayListNoNulls;
import com.joliciel.talismane.utils.LogUtils;

public class PosTagSequenceFilterFactory {
	public static final String TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY = "postag_preprocessing_filter";
	public static final String POSTAG_SEQUENCE_FILTER_DESCRIPTOR_KEY = "postag_postprocessing_filter";

	private static final Logger LOG = LoggerFactory.getLogger(PosTagSequenceFilterFactory.class);

	/**
	 * Gets a PosTagSequenceFilter corresponding to a given descriptor. The
	 * descriptor should contain the class name, followed by any arguments,
	 * separated by tabs.
	 */
	public PosTagSequenceFilter getPosTagSequenceFilter(String descriptor) {
		PosTagSequenceFilter filter = null;
		List<Class<? extends PosTagSequenceFilter>> classes = new ArrayListNoNulls<Class<? extends PosTagSequenceFilter>>();
		classes.add(RemoveNullEmptyTokensFilter.class);

		for (Class<? extends PosTagSequenceFilter> clazz : classes) {
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

		if (filter == null) {
			throw new TalismaneException("Unknown PosTagSequenceFilter: " + descriptor);
		}

		return filter;
	}

}
