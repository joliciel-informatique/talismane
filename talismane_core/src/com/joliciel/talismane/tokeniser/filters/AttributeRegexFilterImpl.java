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
package com.joliciel.talismane.tokeniser.filters;

import java.util.List;

class AttributeRegexFilterImpl extends TokenRegexFilterImpl implements AttributeRegexFilter {

	public AttributeRegexFilterImpl(String regex) {
		super(regex);
	}

	@Override
	public List<TokenPlaceholder> apply(String text) {
		List<TokenPlaceholder> placeholders = super.apply(text);
		for (TokenPlaceholder placeholder : placeholders) {
			placeholder.setSingleToken(false);
		}
		return placeholders;
	}

	
}
