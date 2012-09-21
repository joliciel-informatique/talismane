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


import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenWrapper;

/**
 * Tests all tokens within a certain range for a certain criterion,
 * and returns true if any one of them satisfies the criterion.<br/>
 * If (start&gt;end) will return null.<br/>
 * If start or end are outside of the token's sequence, will test all valid tokens only.<br/>
 * If no tokens are tested, will return null.<br/>
 * If any test returns null, will return null.<br/>
 * @author Assaf Urieli
 *
 */
public class OrRangeFeature extends AbstractTokenFeature<Boolean> implements BooleanFeature<TokenWrapper> {
	private BooleanFeature<TokenWrapper> criterion;
	private IntegerFeature<TokenWrapper> startFeature;
	private IntegerFeature<TokenWrapper> endFeature;

	public OrRangeFeature(BooleanFeature<TokenWrapper> criterion, IntegerFeature<TokenWrapper> startFeature, IntegerFeature<TokenWrapper> endFeature) {
		this.criterion = criterion;
		this.startFeature = startFeature;
		this.endFeature = endFeature;
		this.setName(super.getName() + "(" + criterion.getName() + "," + startFeature.getName() + "," + endFeature.getName() + ")");
	}

	@Override
	public FeatureResult<Boolean> checkInternal(TokenWrapper tokenWrapper) {
		Token token = tokenWrapper.getToken();
		FeatureResult<Boolean> featureResult = null;
		
		FeatureResult<Integer> startResult = startFeature.check(tokenWrapper);
		FeatureResult<Integer> endResult = endFeature.check(tokenWrapper);
		if (startResult!=null && endResult!=null) {
			int start = startResult.getOutcome();
			int end = endResult.getOutcome();
			if (start<0) start = 0;
			if (end>token.getTokenSequence().size()-1) end = token.getTokenSequence().size()-1;
			if (start<=end) {
				Boolean result = Boolean.FALSE;
				for (int i=start; i<=end; i++) {
					Token oneToken = token.getTokenSequence().get(i);
					FeatureResult<Boolean> criterionResult = this.criterion.check(oneToken);
					if (criterionResult==null) {
						result = null;
						break;
					}
					result = result || criterionResult.getOutcome();
					
				}
				if (result!=null) {
					featureResult = this.generateResult(result);
				}
			}
		}
		return featureResult;
	}
}
