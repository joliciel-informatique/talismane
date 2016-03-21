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

import java.util.List;

import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

class TokenFilterWrapper implements TokenSequenceFilter {
	List<TokenFilter> tokenFilters;
	
	public TokenFilterWrapper(List<TokenFilter> tokenFilters) {
		this.tokenFilters = tokenFilters;
	}
	
	@Override
	public void apply(TokenSequence tokenSequence) {
		for (Token token : tokenSequence) {
			for (TokenFilter tokenFilter : this.tokenFilters) {
				List<TokenPlaceholder> placeholders = tokenFilter.apply(token.getOriginalText());
				if (placeholders.size()>0) {
					TokenPlaceholder placeholder = placeholders.iterator().next();
					if (placeholder.getReplacement()!=null && placeholder.getStartIndex()==0 && placeholder.getEndIndex()==token.getText().length()) {
						token.setText(placeholder.getReplacement());
					}
					break;
				}
			}
		}
	}

}
