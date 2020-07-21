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
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenAddressFunction;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;

/**
 * Returns the number of dependents for a given head and label.
 * 
 * @author Assaf Urieli
 *
 */
public final class ValencyByLabelFeature extends AbstractParseConfigurationFeature<Integer>
    implements IntegerFeature<ParseConfigurationWrapper> {
  private PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction;
  private StringFeature<ParseConfigurationWrapper> dependencyLabelFeature;

  public ValencyByLabelFeature(PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction,
      StringFeature<ParseConfigurationWrapper> dependencyLabelFeature) {
    super();
    this.addressFunction = addressFunction;
    this.dependencyLabelFeature = dependencyLabelFeature;
    this.setName(super.getName() + "(" + this.addressFunction.getName() + "," + this.dependencyLabelFeature.getName() + ")");
  }

  @Override
  public FeatureResult<Integer> check(ParseConfigurationWrapper wrapper, RuntimeEnvironment env) throws TalismaneException {
    ParseConfiguration configuration = wrapper.getParseConfiguration();
    FeatureResult<PosTaggedTokenWrapper> tokenResult = addressFunction.check(wrapper, env);
    FeatureResult<Integer> featureResult = null;
    if (tokenResult != null) {
      FeatureResult<String> depLabelResult = dependencyLabelFeature.check(wrapper, env);
      if (depLabelResult != null) {
        PosTaggedToken posTaggedToken = tokenResult.getOutcome().getPosTaggedToken();
        String label = depLabelResult.getOutcome();
        int valency = configuration.getDependents(posTaggedToken, label).size();
        featureResult = this.generateResult(valency);
      }
    }
    return featureResult;
  }

  public PosTaggedTokenAddressFunction<ParseConfigurationWrapper> getAddressFunction() {
    return addressFunction;
  }

  public void setAddressFunction(PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction) {
    this.addressFunction = addressFunction;
  }

  public StringFeature<ParseConfigurationWrapper> getDependencyLabelFeature() {
    return dependencyLabelFeature;
  }

  public void setDependencyLabelFeature(StringFeature<ParseConfigurationWrapper> dependencyLabelFeature) {
    this.dependencyLabelFeature = dependencyLabelFeature;
  }

}
