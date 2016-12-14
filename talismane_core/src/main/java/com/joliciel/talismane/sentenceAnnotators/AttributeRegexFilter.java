///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Joliciel Informatique
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

import java.util.List;
import java.util.Map;

import com.joliciel.talismane.TalismaneSession;

/**
 * A TokenRegexFilter which only adds attributes to contained tokens, without
 * delimiting a single token. Note that replacements will be ignored in this
 * type of filter.
 * 
 * @author Assaf Urieli
 *
 */
public class AttributeRegexFilter extends AbstractRegexFilter {
	public AttributeRegexFilter(String regex, TalismaneSession talismaneSession) {
		super(regex, talismaneSession, false);
	}

	AttributeRegexFilter() {
		super(false);
	}

	@Override
	protected void loadInternal(Map<String, String> parameters, List<String> tabs) {
		// nothing to do
	}
}
