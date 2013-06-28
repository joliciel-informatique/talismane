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


import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.IntegerLiteralFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.tokeniser.Token;

/**
 * Returns the offset of the first token to the left of this one
 * which matches a certain criterion, or null if no such token is found.<br/>
 * If an initial offset is provided as a second argument (must be &lt;=0), will start looking at this initial offset.<br/>
 * The offset returned is expressed as a negative integer.<br/>
 * @author Assaf Urieli
 *
 */
public final class BackwardLookupFeature extends AbstractTokenFeature<Integer> implements IntegerFeature<TokenWrapper> {
	private BooleanFeature<TokenWrapper> criterion;
	private IntegerFeature<TokenWrapper> offsetFeature = new IntegerLiteralFeature<TokenWrapper>(-1);
	
	public BackwardLookupFeature(BooleanFeature<TokenWrapper> criterion) {
		this.criterion = criterion;
		this.setName(super.getName() + "(" + criterion.getName() + ")");
	}
	
	public BackwardLookupFeature(TokenAddressFunction<TokenWrapper> addressFunction, BooleanFeature<TokenWrapper> criterion) {
		this(criterion);
		this.setAddressFunction(addressFunction);
	}


	public BackwardLookupFeature(BooleanFeature<TokenWrapper> criterion, IntegerFeature<TokenWrapper> offsetFeature) {
		this.criterion = criterion;
		this.offsetFeature = offsetFeature;
		this.setName(super.getName() + "(" + criterion.getName() + "," + offsetFeature.getName() + ")");
	}
	
	public BackwardLookupFeature(TokenAddressFunction<TokenWrapper> addressFunction, BooleanFeature<TokenWrapper> criterion, IntegerFeature<TokenWrapper> offsetFeature) {
		this(criterion, offsetFeature);
		this.setAddressFunction(addressFunction);
	}


	@Override
	public FeatureResult<Integer> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) {
		TokenWrapper innerWrapper = this.getToken(tokenWrapper, env);
		if (innerWrapper==null)
			return null;
		Token token = innerWrapper.getToken();
		
		FeatureResult<Integer> featureResult = null;

		FeatureResult<Integer> offsetResult = offsetFeature.check(innerWrapper, env);
		if (offsetResult!=null) {
			int index = token.getIndex();
			int initialOffset = offsetResult.getOutcome();
			
			if (initialOffset>0) 
				throw new TalismaneException("For BackwardLookup, initial offset must be <= 0");
			
			int matchingOffset = 0;
			int j = -1;
			for (int i=index+initialOffset; i>=0; i--) {
				Token oneToken = token.getTokenSequence().get(i);
				FeatureResult<Boolean> criterionResult = this.criterion.check(oneToken, env);
				if (criterionResult!=null && criterionResult.getOutcome()) {
					matchingOffset = j;
					break;
				}
				j--;
			}
			if (matchingOffset<0) {
				featureResult = this.generateResult(matchingOffset);
			}
		}
		return featureResult;
	}
}
