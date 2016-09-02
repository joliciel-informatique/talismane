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
import java.util.List;

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.features.AbstractFeature;
import com.joliciel.talismane.machineLearning.features.AbstractFeatureParser;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.DoubleFeature;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureClassContainer;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenAddressFunction;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenAddressFunctionWrapper;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenFeature;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureParser;

class ParserFeatureParser extends AbstractFeatureParser<ParseConfigurationWrapper> {
	private ParserFeatureServiceInternal parserFeatureServiceInternal;
	private final TalismaneSession talismaneSession;

	public ParserFeatureParser(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
		this.setExternalResourceFinder(talismaneSession.getExternalResourceFinder());
	}

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

	public ParserFeatureServiceInternal getParserFeatureServiceInternal() {
		return parserFeatureServiceInternal;
	}

	public void setParserFeatureServiceInternal(ParserFeatureServiceInternal parserFeatureServiceInternal) {
		this.parserFeatureServiceInternal = parserFeatureServiceInternal;
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

	private static class ParseConfigurationFeatureWrapper<Y> extends AbstractFeature<ParseConfigurationWrapper, Y> implements ParseConfigurationFeature<Y> {
		private Feature<ParseConfigurationWrapper, Y> wrappedFeature = null;

		public ParseConfigurationFeatureWrapper(Feature<ParseConfigurationWrapper, Y> wrappedFeature) {
			super();
			this.wrappedFeature = wrappedFeature;
			this.setName(wrappedFeature.getName());
			this.setCollectionName(wrappedFeature.getCollectionName());
		}

		@Override
		public FeatureResult<Y> check(ParseConfigurationWrapper context, RuntimeEnvironment env) {
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
		if (feature instanceof NeedsTalismaneSession) {
			((NeedsTalismaneSession) feature).setTalismaneSession(talismaneSession);
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
