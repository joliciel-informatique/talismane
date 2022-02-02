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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.NeedsSessionId;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.features.AbstractFeature;
import com.joliciel.talismane.machineLearning.features.AbstractFeatureParser;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.DoubleFeature;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureClassContainer;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptorParser;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.parser.Transition;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenAddressFunction;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenAddressFunctionWrapper;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenFeature;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureParser;

/**
 * The central class for parsing a descriptor containing parser features.<br>
 * The list of available features is given in
 * {@link #addFeatureClasses(FeatureClassContainer)}.
 * 
 * @author Assaf Urieli
 *
 */
public class ParserFeatureParser extends AbstractFeatureParser<ParseConfigurationWrapper> {
  private static final Logger LOG = LoggerFactory.getLogger(ParserFeatureParser.class);

  private final String sessionId;

  public ParserFeatureParser(String sessionId) {
    this.sessionId = sessionId;
    this.setExternalResourceFinder(TalismaneSession.get(sessionId).getExternalResourceFinder());
  }

  public Set<ParseConfigurationFeature<?>> getFeatures(List<String> featureDescriptors) {
    Set<ParseConfigurationFeature<?>> parseFeatures = new TreeSet<ParseConfigurationFeature<?>>();
    FunctionDescriptorParser descriptorParser = new FunctionDescriptorParser();

    for (String featureDescriptor : featureDescriptors) {
      if (featureDescriptor.trim().length() > 0 && !featureDescriptor.startsWith("#")) {
        FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
        List<ParseConfigurationFeature<?>> myFeatures = this.parseDescriptor(functionDescriptor);
        parseFeatures.addAll(myFeatures);
      }
    }
    return parseFeatures;
  }

  public List<ParserRule> getRules(List<String> ruleDescriptors) throws TalismaneException {
    List<ParserRule> rules = new ArrayList<ParserRule>();

    FunctionDescriptorParser descriptorParser = new FunctionDescriptorParser();

    for (String ruleDescriptor : ruleDescriptors) {
      LOG.debug(ruleDescriptor);
      if (ruleDescriptor.trim().length() > 0 && !ruleDescriptor.startsWith("#")) {
        String[] ruleParts = ruleDescriptor.split("\t");
        String transitionCode = ruleParts[0];
        Transition transition = null;
        Set<Transition> transitions = null;
        boolean negative = false;
        String descriptor = null;
        String descriptorName = null;
        if (ruleParts.length > 2) {
          descriptor = ruleParts[2];
          descriptorName = ruleParts[1];
        } else {
          descriptor = ruleParts[1];
        }

        if (transitionCode.length() == 0) {
          if (descriptorName == null) {
            throw new TalismaneException("Rule without Transition must have a name.");
          }
        } else {
          if (transitionCode.startsWith("!")) {
            negative = true;
            String[] transitionCodes = transitionCode.substring(1).split(";");
            transitions = new HashSet<Transition>();
            for (String code : transitionCodes) {
              Transition oneTransition = TalismaneSession.get(sessionId).getTransitionSystem().getTransitionForCode(code);
              transitions.add(oneTransition);
            }
            transition = transitions.iterator().next();
          } else {
            transition = TalismaneSession.get(sessionId).getTransitionSystem().getTransitionForCode(transitionCode);
          }

        }
        FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(descriptor);
        if (descriptorName != null)
          functionDescriptor.setDescriptorName(descriptorName);
        List<ParseConfigurationFeature<?>> myFeatures = this.parseDescriptor(functionDescriptor);
        if (transition != null) {
          for (ParseConfigurationFeature<?> feature : myFeatures) {
            if (feature instanceof BooleanFeature) {
              @SuppressWarnings("unchecked")
              BooleanFeature<ParseConfigurationWrapper> condition = (BooleanFeature<ParseConfigurationWrapper>) feature;

              if (negative) {
                ParserRule rule = new ParserRule(condition, transitions, true);
                rules.add(rule);
              } else {
                ParserRule rule = new ParserRule(condition, transition, false);
                rules.add(rule);
              }
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
   * Adds the following feature class mappings:<br>
   * - AncestorSearch: {@link AncestorSearchFeature}<br>
   * - BackwardSearch: {@link BackwardSearchFeature}<br>
   * - BetweenCountIf: {@link BetweenCountIf}<br>
   * - Buffer: {@link AddressFunctionBuffer}<br>
   * - Dep: {@link AddressFunctionDep}<br>
   * - DepCountIf: {@link DependencyCountIf}<br>
   * - DepLabel: {@link DependencyLabelFeature}<br>
   * - DependencyLabel: {@link DependencyLabelFeature}<br>
   * - DepLabelSet: {@link DependencyLabelSetFeature}<br>
   * - DepSearch: {@link DependencySearchFeature}<br>
   * - Distance: {@link DistanceFeature}<br>
   * - ForwardSearch: {@link ForwardSearchFeature}<br>
   * - Head: {@link AddressFunctionHead}<br>
   * - LDep: {@link AddressFunctionLDep}<br>
   * - Offset: {@link AddressFunctionOffset}<br>
   * - Placeholder: {@link ImplicitAddressFeature}<br>
   * - RDep: {@link AddressFunctionRDep}<br>
   * - Seq: {@link AddressFunctionSequence}<br>
   * - Stack: {@link AddressFunctionStack}<br>
   * - StackSearch: {@link StackSearchFeature}<br>
   * - TokenSearch: {@link TokenSearchFeature}<br>
   * - Valency: {@link ValencyFeature}<br>
   * - Valency: {@link ValencyByLabelFeature}<br>
   * - All definitions in
   * {@link PosTaggerFeatureParser#addPosTaggedTokenFeatureClasses(FeatureClassContainer)}
   * <br>
   */
  @Override
  public void addFeatureClasses(FeatureClassContainer container) {
    container.addFeatureClass("AncestorSearch", AncestorSearchFeature.class);
    container.addFeatureClass("BackwardSearch", BackwardSearchFeature.class);
    container.addFeatureClass("BetweenCountIf", BetweenCountIf.class);
    container.addFeatureClass("Buffer", AddressFunctionBuffer.class);
    container.addFeatureClass("Dep", AddressFunctionDep.class);
    container.addFeatureClass("DepCountIf", DependencyCountIf.class);
    container.addFeatureClass("DepLabel", DependencyLabelFeature.class);
    container.addFeatureClass("DependencyLabel", DependencyLabelFeature.class);
    container.addFeatureClass("DepLabelSet", DependencyLabelSetFeature.class);
    container.addFeatureClass("DepSearch", DependencySearchFeature.class);
    container.addFeatureClass("Distance", DistanceFeature.class);
    container.addFeatureClass("ForwardSearch", ForwardSearchFeature.class);
    container.addFeatureClass("Head", AddressFunctionHead.class);
    container.addFeatureClass("LDep", AddressFunctionLDep.class);
    container.addFeatureClass("Offset", AddressFunctionOffset.class);
    container.addFeatureClass("Placeholder", ImplicitAddressFeature.class);
    container.addFeatureClass("RDep", AddressFunctionRDep.class);
    container.addFeatureClass("Seq", AddressFunctionSequence.class);
    container.addFeatureClass("Stack", AddressFunctionStack.class);
    container.addFeatureClass("StackSearch", StackSearchFeature.class);
    container.addFeatureClass("TokenSearch", TokenSearchFeature.class);
    container.addFeatureClass("Valency", ValencyFeature.class);
    container.addFeatureClass("Valency", ValencyByLabelFeature.class);

    PosTaggerFeatureParser.addPosTaggedTokenFeatureClasses(container);

  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public List<ParseConfigurationFeature<?>> parseDescriptor(FunctionDescriptor functionDescriptor) {
    List<Feature<ParseConfigurationWrapper, ?>> parseFeatures = this.parse(functionDescriptor);
    List<ParseConfigurationFeature<?>> wrappedFeatures = new ArrayList<ParseConfigurationFeature<?>>();
    for (Feature<ParseConfigurationWrapper, ?> parseFeature : parseFeatures) {
      ParseConfigurationFeature<?> wrappedFeature = null;
      if (parseFeature instanceof ParseConfigurationFeature) {
        wrappedFeature = (ParseConfigurationFeature<?>) parseFeature;
      } else if (parseFeature instanceof BooleanFeature) {
        wrappedFeature = new ParseConfigurationBooleanFeatureWrapper((Feature<ParseConfigurationWrapper, Boolean>) parseFeature);
      } else if (parseFeature instanceof StringFeature) {
        wrappedFeature = new ParseConfigurationStringFeatureWrapper((Feature<ParseConfigurationWrapper, String>) parseFeature);
      } else if (parseFeature instanceof IntegerFeature) {
        wrappedFeature = new ParseConfigurationIntegerFeatureWrapper((Feature<ParseConfigurationWrapper, Integer>) parseFeature);
      } else if (parseFeature instanceof DoubleFeature) {
        wrappedFeature = new ParseConfigurationDoubleFeatureWrapper((Feature<ParseConfigurationWrapper, Double>) parseFeature);
      } else {
        wrappedFeature = new ParseConfigurationFeatureWrapper(parseFeature);
      }

      wrappedFeatures.add(wrappedFeature);
    }
    return wrappedFeatures;
  }

  @Override
  public List<FunctionDescriptor> getModifiedDescriptors(FunctionDescriptor functionDescriptor) {
    List<FunctionDescriptor> descriptors = new ArrayList<FunctionDescriptor>();
    String functionName = functionDescriptor.getFunctionName();

    @SuppressWarnings("rawtypes")
    List<Class<? extends Feature>> featureClasses = this.getFeatureClasses(functionName);

    @SuppressWarnings("rawtypes")
    Class<? extends Feature> featureClass = null;
    if (featureClasses != null && featureClasses.size() > 0)
      featureClass = featureClasses.get(0);

    if (featureClass != null) {
      if (featureClass.equals(DependencyCountIf.class)) {
        if (functionDescriptor.getArguments().size() == 1) {
          String descriptor = this.getFeatureClassDescriptors(ImplicitAddressFeature.class).get(0);
          FunctionDescriptor implicitAddressDescriptor = new FunctionDescriptor(descriptor);
          functionDescriptor.addArgument(0, implicitAddressDescriptor);
        }
      } else if (PosTaggedTokenFeature.class.isAssignableFrom(featureClass) || ParseConfigurationAddressFeature.class.isAssignableFrom(featureClass)) {
        if (functionDescriptor.getArguments().size() == 0) {
          String descriptor = this.getFeatureClassDescriptors(ImplicitAddressFeature.class).get(0);
          FunctionDescriptor implicitAddressDescriptor = new FunctionDescriptor(descriptor);
          functionDescriptor.addArgument(implicitAddressDescriptor);

        } // has arguments
      }
    }

    if (descriptors.size() == 0) {
      descriptors.add(functionDescriptor);
    }
    return descriptors;
  }

  private static class ParseConfigurationFeatureWrapper<Y> extends AbstractFeature<ParseConfigurationWrapper, Y>implements ParseConfigurationFeature<Y> {
    private Feature<ParseConfigurationWrapper, Y> wrappedFeature = null;

    public ParseConfigurationFeatureWrapper(Feature<ParseConfigurationWrapper, Y> wrappedFeature) {
      super();
      this.wrappedFeature = wrappedFeature;
      this.setName(wrappedFeature.getName());
      this.setCollectionName(wrappedFeature.getCollectionName());
    }

    @Override
    public FeatureResult<Y> check(ParseConfigurationWrapper context, RuntimeEnvironment env) throws TalismaneException {
      return wrappedFeature.check(context, env);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class<? extends Feature> getFeatureType() {
      return wrappedFeature.getFeatureType();
    }
  }

  private final class ParseConfigurationBooleanFeatureWrapper extends ParseConfigurationFeatureWrapper<Boolean>
      implements BooleanFeature<ParseConfigurationWrapper> {
    public ParseConfigurationBooleanFeatureWrapper(Feature<ParseConfigurationWrapper, Boolean> wrappedFeature) {
      super(wrappedFeature);
    }
  }

  private final class ParseConfigurationStringFeatureWrapper extends ParseConfigurationFeatureWrapper<String>
      implements StringFeature<ParseConfigurationWrapper> {
    public ParseConfigurationStringFeatureWrapper(Feature<ParseConfigurationWrapper, String> wrappedFeature) {
      super(wrappedFeature);
    }
  }

  private final class ParseConfigurationDoubleFeatureWrapper extends ParseConfigurationFeatureWrapper<Double>
      implements DoubleFeature<ParseConfigurationWrapper> {
    public ParseConfigurationDoubleFeatureWrapper(Feature<ParseConfigurationWrapper, Double> wrappedFeature) {
      super(wrappedFeature);
    }
  }

  private final class ParseConfigurationIntegerFeatureWrapper extends ParseConfigurationFeatureWrapper<Integer>
      implements IntegerFeature<ParseConfigurationWrapper> {
    public ParseConfigurationIntegerFeatureWrapper(Feature<ParseConfigurationWrapper, Integer> wrappedFeature) {
      super(wrappedFeature);
    }
  }

  @Override
  public void injectDependencies(@SuppressWarnings("rawtypes") Feature feature) {
    if (feature instanceof NeedsSessionId) {
      ((NeedsSessionId) feature).setSessionId(sessionId);
    }
  }

  @Override
  protected boolean canConvert(Class<?> parameterType, Class<?> originalArgumentType) {
    return false;
  }

  @Override
  protected Feature<ParseConfigurationWrapper, ?> convertArgument(Class<?> parameterType, Feature<ParseConfigurationWrapper, ?> originalArgument) {
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Feature<ParseConfigurationWrapper, ?> convertFeatureCustomType(Feature<ParseConfigurationWrapper, ?> feature) {
    Feature<ParseConfigurationWrapper, ?> convertedFeature = feature;
    if (PosTaggedTokenAddressFunction.class.isAssignableFrom(feature.getFeatureType()) && !(feature instanceof PosTaggedTokenAddressFunction)) {
      convertedFeature = new PosTaggedTokenAddressFunctionWrapper<ParseConfigurationWrapper>(
          (Feature<ParseConfigurationWrapper, PosTaggedTokenWrapper>) feature);
    }

    return convertedFeature;
  }
}
