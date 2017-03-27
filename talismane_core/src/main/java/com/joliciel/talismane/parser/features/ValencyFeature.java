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
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenAddressFunction;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;

/**
 * Returns the number of dependents.
 * 
 * @author Assaf Urieli
 *
 */
public final class ValencyFeature extends AbstractParseConfigurationFeature<Integer>implements IntegerFeature<ParseConfigurationWrapper> {
  private PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction;

  public ValencyFeature(PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction) {
    super();
    this.addressFunction = addressFunction;
    this.setName(super.getName() + "(" + this.addressFunction.getName() + ")");
  }

  @Override
  public FeatureResult<Integer> check(ParseConfigurationWrapper wrapper, RuntimeEnvironment env) throws TalismaneException {
    ParseConfiguration configuration = wrapper.getParseConfiguration();
    FeatureResult<PosTaggedTokenWrapper> tokenResult = addressFunction.check(wrapper, env);
    FeatureResult<Integer> featureResult = null;
    if (tokenResult != null) {
      PosTaggedToken posTaggedToken = tokenResult.getOutcome().getPosTaggedToken();
      int valency = configuration.getDependents(posTaggedToken).size();
      featureResult = this.generateResult(valency);
    }
    return featureResult;
  }

}
