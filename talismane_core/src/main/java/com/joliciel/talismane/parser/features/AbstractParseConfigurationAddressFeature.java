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
import com.joliciel.talismane.machineLearning.features.AbstractFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenAddressFunction;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;

abstract class AbstractParseConfigurationAddressFeature<T> extends AbstractFeature<ParseConfigurationWrapper, T>
    implements ParseConfigurationAddressFeature<T> {
  PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction;

  public AbstractParseConfigurationAddressFeature(PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction) {
    this.addressFunction = addressFunction;
  }

  public PosTaggedTokenAddressFunction<ParseConfigurationWrapper> getAddressFunction() {
    return addressFunction;
  }

  public void setAddressFunction(PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction) {
    this.addressFunction = addressFunction;
  }

  protected PosTaggedTokenWrapper getToken(ParseConfigurationWrapper parseConfiguration, RuntimeEnvironment env) throws TalismaneException {
    FeatureResult<PosTaggedTokenWrapper> tokenResult = addressFunction.check(parseConfiguration, env);
    if (tokenResult == null)
      return null;
    return tokenResult.getOutcome();
  }
}
