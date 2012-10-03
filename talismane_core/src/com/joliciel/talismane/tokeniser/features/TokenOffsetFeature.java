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

import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.tokeniser.Token;

/**
 * Allows to apply any other TokenFeature to a token
 * offset from the current token by a certain offset.<br/>
 * Returns null if the offset goes outside the token sequence.
 * @author Assaf Urieli
 *
 */
public class TokenOffsetFeature<Y> extends AbstractTokenFeature<Y> {
	Feature<TokenWrapper,Y> tokenFeature;
	IntegerFeature<TokenWrapper> offsetFeature;
	
	public TokenOffsetFeature(IntegerFeature<TokenWrapper> offset, Feature<TokenWrapper,Y> tokenFeature) {
		this.tokenFeature = tokenFeature;
		this.offsetFeature = offset;
		this.setName(tokenFeature.getName() + "[offset<" + this.offsetFeature.getName() + ">]");
	}
	
	@Override
	public FeatureResult<Y> checkInternal(TokenWrapper tokenWrapper) {
		Token token = tokenWrapper.getToken();
		FeatureResult<Y> result = null;
		Token offsetToken = null;
		FeatureResult<Integer> offsetResult = offsetFeature.check(tokenWrapper);
		if (offsetResult!=null) {
			int offset = offsetResult.getOutcome();
			if (offset==0)
				offsetToken = token;
			else  {
				int index = 0;
				if (token.isWhiteSpace()) {
					// Correctly handle index for white space:
					// e.g. if index is negative, start counting from the next non-whitespace token
					// and if index is positive start counting from the previous non-whitespace token
					if (offset>0) {
						if (token.getIndexWithWhiteSpace()-1>=0) {
							index = token.getTokenSequence().listWithWhiteSpace().get(token.getIndexWithWhiteSpace()-1).getIndex();
						} else {
							index = -1;
						}
					} else if (offset<0) {
						if (token.getIndexWithWhiteSpace()+1<token.getTokenSequence().listWithWhiteSpace().size()) {
							index = token.getTokenSequence().listWithWhiteSpace().get(token.getIndexWithWhiteSpace()+1).getIndex();
						} else {
							index = token.getTokenSequence().size();
						}
					}
				} else {
					// not whitespace
					index = token.getIndex();
				}
				int offsetIndex = index + offset;
				if (offsetIndex>=0 && offsetIndex<token.getTokenSequence().size()) {
					offsetToken = token.getTokenSequence().get(offsetIndex);
				}
			}
		}
		if (offsetToken!=null) {
			FeatureResult<Y> originalResult = tokenFeature.check(offsetToken);
			if (originalResult!=null)
				result = this.generateResult(originalResult.getOutcome());
		}
		
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return tokenFeature.getFeatureType();
	}

	public Feature<TokenWrapper, Y> getTokenFeature() {
		return tokenFeature;
	}
	
	
}
