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

import java.util.regex.Pattern;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.tokeniser.Token;

/**
 * Returns true if the token matches a given regular expression.
 * 
 * @author Assaf Urieli
 *
 */
public final class RegexFeature extends AbstractTokenFeature<Boolean>implements BooleanFeature<TokenWrapper> {
	StringFeature<TokenWrapper> regexFeature = null;
	Pattern pattern = null;

	public RegexFeature(StringFeature<TokenWrapper> regexFeature) {
		this.regexFeature = regexFeature;
		this.setName(super.getName() + "(" + regexFeature.getName() + ")");
	}

	public RegexFeature(TokenAddressFunction<TokenWrapper> addressFunction, StringFeature<TokenWrapper> regexFeature) {
		this(regexFeature);
		this.setAddressFunction(addressFunction);
	}

	@Override
	public FeatureResult<Boolean> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) throws TalismaneException {
		TokenWrapper innerWrapper = this.getToken(tokenWrapper, env);
		if (innerWrapper == null)
			return null;
		Token token = innerWrapper.getToken();
		FeatureResult<Boolean> result = null;

		FeatureResult<String> regexResult = regexFeature.check(innerWrapper, env);
		if (regexResult != null) {
			String regex = regexResult.getOutcome();
			this.pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);

			boolean matches = this.pattern.matcher(token.getAnalyisText()).matches();
			result = this.generateResult(matches);
		}

		return result;
	}
}
