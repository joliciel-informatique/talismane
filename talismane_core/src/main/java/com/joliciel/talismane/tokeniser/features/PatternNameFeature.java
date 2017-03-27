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

import com.joliciel.talismane.machineLearning.features.AbstractCachableFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.tokeniser.patterns.TokenPatternMatch;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;

/**
 * The current pattern's name.
 * 
 * @author Assaf Urieli
 *
 */
public final class PatternNameFeature extends AbstractCachableFeature<TokenPatternMatch, String>implements StringFeature<TokenPatternMatch> {
  public PatternNameFeature() {
  }

  @Override
  public FeatureResult<String> checkInternal(TokenPatternMatch tokenPatternMatch, RuntimeEnvironment env) {
    FeatureResult<String> result = null;

    TokenPattern pattern = tokenPatternMatch.getPattern();
    result = this.generateResult(pattern.getName());

    return result;
  }
}
