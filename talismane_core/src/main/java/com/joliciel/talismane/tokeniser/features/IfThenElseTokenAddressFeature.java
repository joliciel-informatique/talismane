package com.joliciel.talismane.tokeniser.features;

import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.IfThenElseGenericFeature;

public class IfThenElseTokenAddressFeature extends IfThenElseGenericFeature<TokenWrapper, TokenWrapper>implements TokenAddressFunction<TokenWrapper> {

  public IfThenElseTokenAddressFeature(BooleanFeature<TokenWrapper> condition, Feature<TokenWrapper, TokenWrapper> thenFeature,
      Feature<TokenWrapper, TokenWrapper> elseFeature) {
    super(condition, thenFeature, elseFeature);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Class<? extends Feature> getFeatureType() {
    return TokenAddressFunction.class;
  }
}
