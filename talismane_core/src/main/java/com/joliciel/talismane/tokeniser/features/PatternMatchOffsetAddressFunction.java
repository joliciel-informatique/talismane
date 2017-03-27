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
import com.joliciel.talismane.machineLearning.features.AbstractCachableFeature;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;
import com.joliciel.talismane.tokeniser.patterns.TokenPatternMatch;

/**
 * Returns a token offset from the TokeniserPattern containing the present
 * token.<br/>
 * This allows us to find the word preceding a given compound candidate, or
 * following a given compound candidate.<br/>
 * Returns null if the offset goes outside the token sequence.<br/>
 * 
 * @author Assaf Urieli
 *
 */
public final class PatternMatchOffsetAddressFunction extends AbstractCachableFeature<TokenPatternMatch, TokenWrapper>
    implements TokenAddressFunction<TokenPatternMatch> {
  IntegerFeature<TokenPatternMatch> offsetFeature;

  public PatternMatchOffsetAddressFunction(IntegerFeature<TokenPatternMatch> offsetFeature) {
    this.offsetFeature = offsetFeature;
    this.setName("PatternOffset(" + this.offsetFeature.getName() + ")");
  }

  @Override
  public FeatureResult<TokenWrapper> checkInternal(TokenPatternMatch tokenPatternMatch, RuntimeEnvironment env) throws TalismaneException {
    Token token = tokenPatternMatch.getToken();
    FeatureResult<TokenWrapper> result = null;

    TokenPattern tokenPattern = tokenPatternMatch.getPattern();
    int testIndex = tokenPattern.getIndexesToTest().get(0);

    FeatureResult<Integer> offsetResult = offsetFeature.check(tokenPatternMatch, env);
    if (offsetResult != null) {
      int offset = offsetResult.getOutcome();

      Token offsetToken = null;
      if (offset == 0)
        offsetToken = token;
      else {
        // baseIndex should be the last non-whitespace word in the
        // pattern if offset > 0
        // or the first non-whitespace word in the pattern if offset < 0
        int baseIndex = 0;
        int j = token.getIndexWithWhiteSpace() - testIndex;
        for (int i = 0; i < tokenPattern.getTokenCount(); i++) {
          if (j >= 0 && j < token.getTokenSequence().listWithWhiteSpace().size()) {
            Token tokenInPattern = token.getTokenSequence().listWithWhiteSpace().get(j);
            if (!tokenInPattern.isWhiteSpace()) {
              baseIndex = tokenInPattern.getIndex();
              if (offset < 0) {
                break;
              }
            }
          }
          j++;
        }

        int offsetIndex = baseIndex + offset;
        if (offsetIndex >= 0 && offsetIndex < token.getTokenSequence().size()) {
          offsetToken = token.getTokenSequence().get(offsetIndex);
        }
      }
      if (offsetToken != null) {
        result = this.generateResult(offsetToken);
      } // we have an offset token
    } // we have an offset result

    return result;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Class<? extends Feature> getFeatureType() {
    return TokenAddressFunction.class;
  }
}
