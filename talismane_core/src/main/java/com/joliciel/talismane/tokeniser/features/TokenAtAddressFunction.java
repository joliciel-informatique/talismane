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
package com.joliciel.talismane.tokeniser.features;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.tokeniser.Token;

/**
 * Returns a token at a given absolute index, or null if the index is out of
 * sentence range.
 * 
 * @author Assaf Urieli
 *
 */
public final class TokenAtAddressFunction extends AbstractTokenAddressFunction {
	IntegerFeature<TokenWrapper> indexFeature;

	public TokenAtAddressFunction(IntegerFeature<TokenWrapper> index) {
		this.indexFeature = index;
		this.setName("TokenAt(" + this.indexFeature.getName() + ")");
	}

	public TokenAtAddressFunction(TokenAddressFunction<TokenWrapper> addressFunction, IntegerFeature<TokenWrapper> index) {
		this(index);
		this.setAddressFunction(addressFunction);
	}

	@Override
	public FeatureResult<TokenWrapper> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) throws TalismaneException {
		TokenWrapper innerWrapper = this.getToken(tokenWrapper, env);
		if (innerWrapper == null)
			return null;
		Token token = innerWrapper.getToken();

		FeatureResult<TokenWrapper> result = null;

		FeatureResult<Integer> indexResult = indexFeature.check(innerWrapper, env);

		if (indexResult != null) {
			int i = indexResult.getOutcome();
			if (i >= 0 && i < token.getTokenSequence().size()) {
				Token indexedToken = token.getTokenSequence().get(i);
				result = this.generateResult(indexedToken);
			}
		}

		return result;
	}
}
