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
 * Returns a token offset from the current token by a certain offset.<br/>
 * Returns null if the offset goes outside the token sequence.
 * 
 * @author Assaf Urieli
 *
 */
public final class TokenOffsetAddressFunction extends AbstractTokenAddressFunction {
  IntegerFeature<TokenWrapper> offsetFeature;

  public TokenOffsetAddressFunction(IntegerFeature<TokenWrapper> offset) {
    this.offsetFeature = offset;
    this.setName("TokenOffset(" + this.offsetFeature.getName() + ")");
  }

  public TokenOffsetAddressFunction(TokenAddressFunction<TokenWrapper> addressFunction, IntegerFeature<TokenWrapper> offset) {
    this(offset);
    this.setAddressFunction(addressFunction);
  }

  @Override
  public FeatureResult<TokenWrapper> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) throws TalismaneException {
    TokenWrapper innerWrapper = this.getToken(tokenWrapper, env);
    if (innerWrapper == null)
      return null;
    Token token = innerWrapper.getToken();

    FeatureResult<TokenWrapper> result = null;
    Token offsetToken = null;
    FeatureResult<Integer> offsetResult = offsetFeature.check(innerWrapper, env);
    if (offsetResult != null) {
      int offset = offsetResult.getOutcome();
      if (offset == 0)
        offsetToken = token;
      else {
        int index = 0;
        if (token.isWhiteSpace()) {
          // Correctly handle index for white space:
          // e.g. if index is negative, start counting from the next
          // non-whitespace token
          // and if index is positive start counting from the previous
          // non-whitespace token
          if (offset > 0) {
            if (token.getIndexWithWhiteSpace() - 1 >= 0) {
              index = token.getTokenSequence().listWithWhiteSpace().get(token.getIndexWithWhiteSpace() - 1).getIndex();
            } else {
              index = -1;
            }
          } else if (offset < 0) {
            if (token.getIndexWithWhiteSpace() + 1 < token.getTokenSequence().listWithWhiteSpace().size()) {
              index = token.getTokenSequence().listWithWhiteSpace().get(token.getIndexWithWhiteSpace() + 1).getIndex();
            } else {
              index = token.getTokenSequence().size();
            }
          }
        } else {
          // not whitespace
          index = token.getIndex();
        }
        int offsetIndex = index + offset;
        if (offsetIndex >= 0 && offsetIndex < token.getTokenSequence().size()) {
          offsetToken = token.getTokenSequence().get(offsetIndex);
        }
      }
    }
    if (offsetToken != null) {
      result = this.generateResult(offsetToken);
    }

    return result;
  }
}
