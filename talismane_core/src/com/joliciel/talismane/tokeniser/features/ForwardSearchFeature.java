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
import com.joliciel.talismane.tokeniser.Token;

/**
 * Returns the first token following this one which matches a certain criterion, or null if no such token is found.<br/>
 * If a start offset is provided as a second argument (must be &gt;=0), will start looking at this offset.<br/>
 * If an end offset is provided as a third argument (must be &gt;=0), will continue until the end offset and then stop.<br/>
 * Note that, by default, it doesn't look at the current token (e.g. default start offset = 1) - to include the current
 * token, set start offset = 0.<br/>
 * @author Assaf Urieli
 *
 */
public class ForwardSearchFeature extends AbstractTokenAddressFunction implements TokenAddressFunction {
	private BooleanFeature<TokenWrapper> criterion;
	private IntegerFeature<TokenWrapper> startOffsetFeature = null;
	private IntegerFeature<TokenWrapper> endOffsetFeature = null;
	
	public ForwardSearchFeature(BooleanFeature<TokenWrapper> criterion) {
		this.criterion = criterion;
		this.setName(super.getName() + "(" + criterion.getName() + ")");
	}

	public ForwardSearchFeature(BooleanFeature<TokenWrapper> criterion, IntegerFeature<TokenWrapper> startOffsetFeature) {
		this.criterion = criterion;
		this.startOffsetFeature = startOffsetFeature;
		this.setName(super.getName() + "(" + criterion.getName() + "," + startOffsetFeature.getName() + ")");
	}

	public ForwardSearchFeature(BooleanFeature<TokenWrapper> criterion, IntegerFeature<TokenWrapper> startOffsetFeature, IntegerFeature<TokenWrapper> endOffsetFeature) {
		this.criterion = criterion;
		this.startOffsetFeature = startOffsetFeature;
		this.setName(super.getName() + "(" + criterion.getName() + "," + startOffsetFeature.getName() + "," + endOffsetFeature.getName() + ")");
	}
	
	@Override
	public FeatureResult<Token> checkInternal(TokenWrapper tokenWrapper) {
		Token token = tokenWrapper.getToken();
		FeatureResult<Token> featureResult = null;
		
		int startOffset = 1;
		int endOffset = token.getTokenSequence().size();
		
		if (startOffsetFeature!=null) {
			FeatureResult<Integer> startOffsetResult = startOffsetFeature.check(tokenWrapper);
			if (startOffsetResult!=null) {
				startOffset = startOffsetResult.getOutcome();
			} else {
				return featureResult;
			}
		}
		
		if (endOffsetFeature!=null) {
			FeatureResult<Integer> endOffsetResult = endOffsetFeature.check(tokenWrapper);
			if (endOffsetResult!=null) {
				endOffset = endOffsetResult.getOutcome();
			} else {
				return featureResult;
			}
		}
		
		int index = token.getIndex();
		
		if (startOffset<0) 
			throw new TalismaneException("For ForwardLookup, start offset must be >= 0");
		if (endOffset<0) 
			throw new TalismaneException("For ForwardLookup, end offset must be >= 0");
		
		if (endOffset<startOffset)
			return featureResult;
		
		Token matchingToken = null;
		for (int i=index+startOffset; i<token.getTokenSequence().size() && i<=index+endOffset; i++) {
			Token oneToken = token.getTokenSequence().get(i);
			FeatureResult<Boolean> criterionResult = this.criterion.check(oneToken);
			if (criterionResult!=null && criterionResult.getOutcome()) {
				matchingToken = oneToken;
				break;
			}
		}
		if (matchingToken!=null) {
			featureResult = this.generateResult(matchingToken);
		}

		return featureResult;
	}
}
