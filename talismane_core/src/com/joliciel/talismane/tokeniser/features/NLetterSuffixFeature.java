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
package com.joliciel.talismane.tokeniser.features;


import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenWrapper;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.features.IntegerFeature;
import com.joliciel.talismane.utils.features.StringFeature;

/**
 * Retrieves the last N letters of the last entire word in the current token, as long as N &lt; the length of the last word.
 * @author Assaf Urieli
 *
 */
public class NLetterSuffixFeature extends AbstractTokenFeature<String> implements StringFeature<TokenWrapper>  {
	private IntegerFeature<TokenWrapper> nFeature;
	
	public NLetterSuffixFeature(IntegerFeature<TokenWrapper> nFeature) {
		this.nFeature = nFeature;
		this.setName(super.getName() + "(" + this.nFeature.getName() + ")");
	}
	
	@Override
	public FeatureResult<String> checkInternal(TokenWrapper tokenWrapper) {
		Token token = tokenWrapper.getToken();
		FeatureResult<String> result = null;
		String lastWord = token.getText().trim();
		if (lastWord.indexOf(' ')>=0) {
			int lastSpace = lastWord.lastIndexOf(' ');
			lastWord = lastWord.substring(lastSpace+1);
		}

		FeatureResult<Integer> nResult = nFeature.check(tokenWrapper);
		if (nResult!=null) {
			int n = nResult.getOutcome();
			if (lastWord.length()>n) {
				String suffix = lastWord.substring(lastWord.length()-n);
				result = this.generateResult(suffix);
			}
		}
		return result;
	}
}
