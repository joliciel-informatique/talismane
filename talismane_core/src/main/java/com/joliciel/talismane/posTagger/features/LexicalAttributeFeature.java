package com.joliciel.talismane.posTagger.features;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringCollectionFeature;
import com.joliciel.talismane.machineLearning.features.StringFeature;

/**
 * For a given set of lexical attributes, returns one String per combination
 * found in a lexical entry associated with this pos-tagged token.
 * 
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class LexicalAttributeFeature<T> extends AbstractLexicalAttributeFeature<T>implements StringCollectionFeature<T> {
  StringFeature<PosTaggedTokenWrapper>[] attributeNameFeatures;

  @SafeVarargs
  public LexicalAttributeFeature(PosTaggedTokenAddressFunction<T> addressFunction, StringFeature<PosTaggedTokenWrapper>... attributeNameFeatures) {
    super(addressFunction);
    this.attributeNameFeatures = attributeNameFeatures;
    String name = super.getName() + "(";
    boolean firstFeature = true;
    for (StringFeature<PosTaggedTokenWrapper> lexicalAttributeNameFeature : attributeNameFeatures) {
      if (!firstFeature)
        name += ",";
      name += lexicalAttributeNameFeature.getName();
      firstFeature = false;
    }
    name += ")";
    this.setName(name);
    this.setAddressFunction(addressFunction);
  }

  @Override
  protected List<String> getAttributes(PosTaggedTokenWrapper innerWrapper, RuntimeEnvironment env) throws TalismaneException {
    List<String> attributes = new ArrayList<>(attributeNameFeatures.length);
    for (StringFeature<PosTaggedTokenWrapper> lexicalAttributeNameFeature : attributeNameFeatures) {
      FeatureResult<String> attributeResult = lexicalAttributeNameFeature.check(innerWrapper, env);
      if (attributeResult != null)
        attributes.add(attributeResult.getOutcome());
    }
    return attributes;
  }
}
