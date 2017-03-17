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
 * Returns the current token's index.
 * 
 * @author Assaf Urieli
 *
 */
public final class TokenIndexFeature extends AbstractTokenFeature<Integer>implements IntegerFeature<TokenWrapper> {

	public TokenIndexFeature() {
	}

	public TokenIndexFeature(TokenAddressFunction<TokenWrapper> addressFunction) {
		this();
		this.setAddressFunction(addressFunction);
	}

	@Override
	public FeatureResult<Integer> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) throws TalismaneException {
		TokenWrapper innerWrapper = this.getToken(tokenWrapper, env);
		if (innerWrapper == null)
			return null;
		Token token = innerWrapper.getToken();
		FeatureResult<Integer> result = null;

		int index = token.getIndex();
		result = this.generateResult(index);

		return result;
	}
}
