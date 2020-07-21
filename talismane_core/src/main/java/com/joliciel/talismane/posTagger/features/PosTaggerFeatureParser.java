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
package com.joliciel.talismane.posTagger.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.features.AbstractFeature;
import com.joliciel.talismane.machineLearning.features.AbstractFeatureParser;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.DoubleFeature;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureClassContainer;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureWrapper;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptorParser;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTaggerContext;
import com.joliciel.talismane.tokeniser.features.TokenAddressFunction;
import com.joliciel.talismane.tokeniser.features.TokenFeatureParser;
import com.joliciel.talismane.tokeniser.features.TokenWrapper;

/**
 * The central class for parsing a descriptor containing pos-tagger features.
 * <br/>
 * The list of available features is given in
 * {@link #addFeatureClasses(FeatureClassContainer)}.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTaggerFeatureParser extends AbstractFeatureParser<PosTaggerContext> {
  private static final Logger LOG = LoggerFactory.getLogger(PosTaggerFeatureParser.class);

  private final String sessionId;

  public PosTaggerFeatureParser(String sessionId) {
    this.sessionId = sessionId;
    this.setExternalResourceFinder(TalismaneSession.get(sessionId).getExternalResourceFinder());
  }

  public Set<PosTaggerFeature<?>> getFeatureSet(List<String> featureDescriptors) {
    Set<PosTaggerFeature<?>> features = new TreeSet<PosTaggerFeature<?>>();
    FunctionDescriptorParser descriptorParser = new FunctionDescriptorParser();

    for (String featureDescriptor : featureDescriptors) {
      LOG.debug(featureDescriptor);
      if (featureDescriptor.length() > 0 && !featureDescriptor.startsWith("#")) {
        FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
        List<PosTaggerFeature<?>> myFeatures = this.parseDescriptor(functionDescriptor);
        features.addAll(myFeatures);

      }
    }
    return features;
  }

  /**
   * 
   * @param ruleDescriptors
   * @return
   * @throws TalismaneException
   *           if a rule is incorrectly configured
   */
  public List<PosTaggerRule> getRules(List<String> ruleDescriptors) throws TalismaneException {
    List<PosTaggerRule> rules = new ArrayList<PosTaggerRule>();

    FunctionDescriptorParser descriptorParser = new FunctionDescriptorParser();

    for (String ruleDescriptor : ruleDescriptors) {
      LOG.debug(ruleDescriptor);
      if (ruleDescriptor.length() > 0 && !ruleDescriptor.startsWith("#")) {
        String[] ruleParts = ruleDescriptor.split("\t");
        String posTagCode = ruleParts[0];
        PosTag posTag = null;
        boolean negative = false;
        String descriptor = null;
        String descriptorName = null;
        if (ruleParts.length > 2) {
          descriptor = ruleParts[2];
          descriptorName = ruleParts[1];
        } else {
          descriptor = ruleParts[1];
        }

        if (posTagCode.length() == 0) {
          if (descriptorName == null) {
            throw new TalismaneException("Rule without PosTag must have a name.");
          }
        } else {
          if (posTagCode.startsWith("!")) {
            negative = true;
            posTagCode = posTagCode.substring(1);
          }
          posTag = TalismaneSession.get(sessionId).getPosTagSet().getPosTag(posTagCode);
        }

        FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(descriptor);
        if (descriptorName != null)
          functionDescriptor.setDescriptorName(descriptorName);
        List<PosTaggerFeature<?>> myFeatures = this.parseDescriptor(functionDescriptor);
        if (posTag != null) {
          for (PosTaggerFeature<?> feature : myFeatures) {
            if (feature instanceof BooleanFeature) {
              @SuppressWarnings("unchecked")
              BooleanFeature<PosTaggerContext> condition = (BooleanFeature<PosTaggerContext>) feature;
              PosTaggerRule rule = new PosTaggerRule(condition, posTag);
              rule.setNegative(negative);
              rules.add(rule);
            } else {
              throw new TalismaneException("Rule must be based on a boolean feature.");
            }
          } // next feature
        } // is it a rule, or just a descriptor
      } // proper rule descriptor
    } // next rule descriptor
    return rules;

  }

  /**
   * Adds the following feature class mappings:<br/>
   * - Ngram: {@link NgramFeature}<br/>
   * - History: {@link PosTaggerHistoryAddressFunction}<br/>
   * - HistoryAbs: {@link HistoryAbsoluteAddressFunction}<br/>
   * - HistoryCountIf: {@link HistoryCountIfFeature}<br/>
   * - HistoryHas: {@link HistoryHasFeature}<br/>
   * - HistorySearch: {@link HistorySearchFeature}<br/>
   * - All definitions in
   * {@link #addPosTaggedTokenFeatureClasses(FeatureClassContainer)}<br/>
   * - All definitions in
   * {@link TokenFeatureParser#addFeatureClasses(FeatureClassContainer)}<br/>
   */
  @Override
  public void addFeatureClasses(FeatureClassContainer container) {
    container.addFeatureClass("Ngram", NgramFeature.class);
    container.addFeatureClass("History", PosTaggerHistoryAddressFunction.class);
    container.addFeatureClass("HistoryAbs", HistoryAbsoluteAddressFunction.class);
    container.addFeatureClass("HistoryCountIf", HistoryCountIfFeature.class);
    container.addFeatureClass("HistoryHas", HistoryHasFeature.class);
    container.addFeatureClass("HistorySearch", HistorySearchFeature.class);
    PosTaggerFeatureParser.addPosTaggedTokenFeatureClasses(container);
    TokenFeatureParser.addFeatureClasses(container);
  }

  /**
   * Add pos-tagged token feature classes to the container provided, including:
   * <br/>
   * - AllLemmas: {@link AllLemmasFeature}<br/>
   * - Aspect: {@link VerbAspectFeature}<br/>
   * - Case: {@link GrammaticalCaseFeature}<br/>
   * - Category: {@link LexicalCategoryFeature}<br/>
   * - ClosedClass: {@link ClosedClassFeature}<br/>
   * - CombinedLexicalAttributes: {@link CombinedLexicalAttributesFeature} <br/>
   * - Gender: {@link GrammaticalGenderFeature}<br/>
   * - Index: {@link PosTaggedTokenIndexFeature}<br/>
   * - Lemma: {@link LemmaFeature}<br/>
   * - LexicalAttribute: {@link LexicalAttributeFeature}<br/>
   * - LexicalForm: {@link WordFormFeature}<br/>
   * - Mood: {@link VerbMoodFeature}<br/>
   * - Morphology: {@link MorphologyFeature}<br/>
   * - Number: {@link GrammaticalNumberFeature}<br/>
   * - Person: {@link GrammaticalPersonFeature}<br/>
   * - PosTag: {@link AssignedPosTagFeature}<br/>
   * - PosTagIn: {@link AssignedPosTagInFeature}<br/>
   * - PossessorNumber: {@link PossessorNumberFeature}<br/>
   * - SubCategory: {@link LexicalSubCategoryFeature}<br/>
   * - Tense: {@link VerbTenseFeature}<br/>
   * - TokenHas: {@link HistoryHasFeature}<br/>
   * - WordForm: {@link WordFormFeature}<br/>
   */
  public static void addPosTaggedTokenFeatureClasses(FeatureClassContainer container) {
    container.addFeatureClass("AllLemmas", AllLemmasFeature.class);
    container.addFeatureClass("Aspect", VerbAspectFeature.class);
    container.addFeatureClass("Case", GrammaticalCaseFeature.class);
    container.addFeatureClass("Category", LexicalCategoryFeature.class);
    container.addFeatureClass("ClosedClass", ClosedClassFeature.class);
    container.addFeatureClass("CombinedLexicalAttributes", CombinedLexicalAttributesFeature.class);
    container.addFeatureClass("Gender", GrammaticalGenderFeature.class);
    container.addFeatureClass("Index", PosTaggedTokenIndexFeature.class);
    container.addFeatureClass("Lemma", LemmaFeature.class);
    container.addFeatureClass("LexicalAttribute", LexicalAttributeFeature.class);
    container.addFeatureClass("LexicalForm", WordFormFeature.class);
    container.addFeatureClass("Mood", VerbMoodFeature.class);
    container.addFeatureClass("Morphology", MorphologyFeature.class);
    container.addFeatureClass("Number", GrammaticalNumberFeature.class);
    container.addFeatureClass("Person", GrammaticalPersonFeature.class);
    container.addFeatureClass("PosTag", AssignedPosTagFeature.class);
    container.addFeatureClass("PosTagIn", AssignedPosTagInFeature.class);
    container.addFeatureClass("PossessorNumber", PossessorNumberFeature.class);
    container.addFeatureClass("SubCategory", LexicalSubCategoryFeature.class);
    container.addFeatureClass("Tense", VerbTenseFeature.class);
    container.addFeatureClass("TokenHas", HistoryHasFeature.class);
    container.addFeatureClass("WordForm", WordFormFeature.class);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public List<PosTaggerFeature<?>> parseDescriptor(FunctionDescriptor functionDescriptor) {
    List<Feature<PosTaggerContext, ?>> posTaggerFeatures = this.parse(functionDescriptor);
    List<PosTaggerFeature<?>> wrappedFeatures = new ArrayList<PosTaggerFeature<?>>();
    for (Feature<PosTaggerContext, ?> posTaggerFeature : posTaggerFeatures) {
      PosTaggerFeature<?> wrappedFeature = null;
      if (posTaggerFeature instanceof PosTaggerFeature) {
        wrappedFeature = (PosTaggerFeature<?>) posTaggerFeature;
      } else if (posTaggerFeature instanceof BooleanFeature) {
        wrappedFeature = new PosTaggerBooleanFeatureWrapper((Feature<PosTaggerContext, Boolean>) posTaggerFeature);
      } else if (posTaggerFeature instanceof StringFeature) {
        wrappedFeature = new PosTaggerStringFeatureWrapper((Feature<PosTaggerContext, String>) posTaggerFeature);
      } else if (posTaggerFeature instanceof IntegerFeature) {
        wrappedFeature = new PosTaggerIntegerFeatureWrapper((Feature<PosTaggerContext, Integer>) posTaggerFeature);
      } else if (posTaggerFeature instanceof DoubleFeature) {
        wrappedFeature = new PosTaggerDoubleFeatureWrapper((Feature<PosTaggerContext, Double>) posTaggerFeature);
      } else {
        wrappedFeature = new PosTaggerFeatureWrapper(posTaggerFeature);
      }
      wrappedFeatures.add(wrappedFeature);
    }
    return wrappedFeatures;
  }

  @Override
  public List<FunctionDescriptor> getModifiedDescriptors(FunctionDescriptor functionDescriptor) {
    List<FunctionDescriptor> descriptors = new ArrayList<FunctionDescriptor>();
    descriptors.add(functionDescriptor);
    return descriptors;
  }

  private static class PosTaggerFeatureWrapper<T> extends AbstractFeature<PosTaggerContext, T>
      implements PosTaggerFeature<T>, FeatureWrapper<PosTaggerContext, T> {
    private Feature<PosTaggerContext, T> wrappedFeature = null;

    public PosTaggerFeatureWrapper(Feature<PosTaggerContext, T> wrappedFeature) {
      super();
      this.wrappedFeature = wrappedFeature;
      this.setName(wrappedFeature.getName());
      this.setCollectionName(wrappedFeature.getCollectionName());
    }

    @Override
    public FeatureResult<T> check(PosTaggerContext context, RuntimeEnvironment env) throws TalismaneException {
      return wrappedFeature.check(context, env);
    }

    @Override
    public Feature<PosTaggerContext, T> getWrappedFeature() {
      return this.wrappedFeature;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class<? extends Feature> getFeatureType() {
      return wrappedFeature.getFeatureType();
    }

    @Override
    public String toString() {
      return wrappedFeature.toString();
    }

  }

  private class PosTaggerBooleanFeatureWrapper extends PosTaggerFeatureWrapper<Boolean>implements BooleanFeature<PosTaggerContext> {
    public PosTaggerBooleanFeatureWrapper(Feature<PosTaggerContext, Boolean> wrappedFeature) {
      super(wrappedFeature);
    }
  }

  private class PosTaggerStringFeatureWrapper extends PosTaggerFeatureWrapper<String>implements StringFeature<PosTaggerContext> {
    public PosTaggerStringFeatureWrapper(Feature<PosTaggerContext, String> wrappedFeature) {
      super(wrappedFeature);
    }
  }

  private class PosTaggerDoubleFeatureWrapper extends PosTaggerFeatureWrapper<Double>implements DoubleFeature<PosTaggerContext> {
    public PosTaggerDoubleFeatureWrapper(Feature<PosTaggerContext, Double> wrappedFeature) {
      super(wrappedFeature);
    }
  }

  private class PosTaggerIntegerFeatureWrapper extends PosTaggerFeatureWrapper<Integer>implements IntegerFeature<PosTaggerContext> {
    public PosTaggerIntegerFeatureWrapper(Feature<PosTaggerContext, Integer> wrappedFeature) {
      super(wrappedFeature);
    }
  }

  @Override
  public void injectDependencies(@SuppressWarnings("rawtypes") Feature feature) {
    TokenFeatureParser.injectDependencies(feature, sessionId);
  }

  @Override
  protected boolean canConvert(Class<?> parameterType, Class<?> originalArgumentType) {
    if (TokenAddressFunction.class.isAssignableFrom(parameterType) && PosTaggedTokenAddressFunction.class.isAssignableFrom(originalArgumentType))
      return true;
    return false;
  }

  @Override
  protected Feature<PosTaggerContext, ?> convertArgument(Class<?> parameterType, Feature<PosTaggerContext, ?> originalArgument) {
    if (TokenAddressFunction.class.isAssignableFrom(parameterType) && (originalArgument instanceof PosTaggedTokenAddressFunction)) {
      @SuppressWarnings("unchecked")
      PosTaggedTokenAddressFunction<PosTaggerContext> originalAddressFunction = (PosTaggedTokenAddressFunction<PosTaggerContext>) originalArgument;
      Feature<PosTaggerContext, ?> convertedFunction = new TokenAddressFunctionWrapper(originalAddressFunction);
      return convertedFunction;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Feature<PosTaggerContext, ?> convertFeatureCustomType(Feature<PosTaggerContext, ?> feature) {
    Feature<PosTaggerContext, ?> convertedFeature = feature;
    if (feature.getFeatureType().equals(PosTaggedTokenAddressFunction.class) && !(feature instanceof PosTaggedTokenAddressFunction)) {
      convertedFeature = new PosTaggedTokenAddressFunctionWrapper<PosTaggerContext>((Feature<PosTaggerContext, PosTaggedTokenWrapper>) feature);
    }
    return convertedFeature;
  }

  private static final class TokenAddressFunctionWrapper extends AbstractFeature<PosTaggerContext, TokenWrapper>
      implements TokenAddressFunction<PosTaggerContext> {
    PosTaggedTokenAddressFunction<PosTaggerContext> posTaggedTokenAddressFunction = null;

    public TokenAddressFunctionWrapper(PosTaggedTokenAddressFunction<PosTaggerContext> posTaggedTokenAddressFunction) {
      this.posTaggedTokenAddressFunction = posTaggedTokenAddressFunction;
      this.setName(this.posTaggedTokenAddressFunction.getName());
    }

    @Override
    public FeatureResult<TokenWrapper> check(PosTaggerContext context, RuntimeEnvironment env) throws TalismaneException {
      FeatureResult<PosTaggedTokenWrapper> posTaggedTokenResult = posTaggedTokenAddressFunction.check(context, env);
      FeatureResult<TokenWrapper> result = null;
      if (posTaggedTokenResult != null) {
        result = this.generateResult(posTaggedTokenResult.getOutcome().getPosTaggedToken());
      }
      return result;
    }

  }
}
