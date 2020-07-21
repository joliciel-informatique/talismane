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
package com.joliciel.talismane.parser.features;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenAddressFunction;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;

/**
 * Returns the distance between the token referred to by addressFunction1 and
 * the token referred to by addressFunction2, as an absolute value from 0 to n.
 * 
 * @author Assaf Urieli
 *
 */
public final class DistanceFeature extends AbstractParseConfigurationFeature<Integer>
    implements IntegerFeature<ParseConfigurationWrapper> {
  private PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction1;
  private PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction2;

  public DistanceFeature(PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction1,
      PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction2) {
    super();
    this.addressFunction1 = addressFunction1;
    this.addressFunction2 = addressFunction2;

    this.setName(super.getName() + "(" + addressFunction1.getName() + "," + addressFunction2.getName() + ")");
  }

  @Override
  public FeatureResult<Integer> check(ParseConfigurationWrapper wrapper, RuntimeEnvironment env) throws TalismaneException {
    FeatureResult<PosTaggedTokenWrapper> tokenResult1 = addressFunction1.check(wrapper, env);
    FeatureResult<PosTaggedTokenWrapper> tokenResult2 = addressFunction2.check(wrapper, env);
    FeatureResult<Integer> featureResult = null;
    if (tokenResult1 != null && tokenResult2 != null) {
      PosTaggedToken posTaggedToken1 = tokenResult1.getOutcome().getPosTaggedToken();
      PosTaggedToken posTaggedToken2 = tokenResult2.getOutcome().getPosTaggedToken();
      int distance = posTaggedToken2.getToken().getIndex() - posTaggedToken1.getToken().getIndex();
      if (distance < 0)
        distance = 0 - distance;
      featureResult = this.generateResult(distance);
    }
    return featureResult;
  }

}
