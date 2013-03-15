///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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

import com.joliciel.talismane.machineLearning.features.AbstractFeature;
import com.joliciel.talismane.machineLearning.features.AbstractFeatureParser;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.DoubleFeature;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureClassContainer;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.posTagger.features.PosTagFeatureParser;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenFeature;

class ParserFeatureParser extends AbstractFeatureParser<ParseConfigurationWrapper> {
	private ParserFeatureServiceInternal parserFeatureServiceInternal;
	
	public ParserFeatureParser(FeatureService featureService) {
		super(featureService);
	}	

	@Override
	public void addFeatureClasses(FeatureClassContainer container) {
		container.addFeatureClass("DependencyLabel", DependencyLabelFeature.class);
		container.addFeatureClass("DependentCountIf", DependentCountIf.class);
		container.addFeatureClass("BetweenCountIf", BetweenCountIf.class);
		container.addFeatureClass("Distance", DistanceFeature.class);
		container.addFeatureClass("Stack", AddressFunctionStack.class);
		container.addFeatureClass("Buffer", AddressFunctionBuffer.class);
		container.addFeatureClass("Head", AddressFunctionHead.class);
		container.addFeatureClass("LDep", AddressFunctionLDep.class);
		container.addFeatureClass("RDep", AddressFunctionRDep.class);
		container.addFeatureClass("Dep", AddressFunctionDep.class);
		container.addFeatureClass("Offset", AddressFunctionOffset.class);
		container.addFeatureClass("Seq", AddressFunctionSequence.class);
		container.addFeatureClass("ForwardSearch", ForwardSearchFeature.class);
		container.addFeatureClass("BackwardSearch", BackwardSearchFeature.class);
		container.addFeatureClass("ExplicitAddress", ExplicitAddressFeature.class);
		
		PosTagFeatureParser.addPosTaggedTokenFeatureClasses(container);

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

	public void setParserFeatureServiceInternal(
			ParserFeatureServiceInternal parserFeatureServiceInternal) {
		this.parserFeatureServiceInternal = parserFeatureServiceInternal;
	}

	@Override
	public List<FunctionDescriptor> getModifiedDescriptors(
			FunctionDescriptor functionDescriptor) {
		List<FunctionDescriptor> descriptors = new ArrayList<FunctionDescriptor>();
		String functionName = functionDescriptor.getFunctionName();
		
		@SuppressWarnings("rawtypes")
		List<Class<? extends Feature>> featureClasses = this.getFeatureClasses(functionName);
		
		@SuppressWarnings("rawtypes")
		Class<? extends Feature> featureClass = null;
		if (featureClasses!=null && featureClasses.size()>0)
			featureClass = featureClasses.get(0);

		if (featureClass!=null &&
				(PosTaggedTokenFeature.class.isAssignableFrom(featureClass)
					|| ParseConfigurationAddressFeature.class.isAssignableFrom(featureClass))) {
			if (functionDescriptor.getArguments().size()>0) {
				FunctionDescriptor firstArgument = functionDescriptor.getArguments().get(0);
				if (firstArgument.isFunction()) {
					@SuppressWarnings("rawtypes")
					List<Class<? extends Feature>> firstArgumentClasses = this.getFeatureClasses(firstArgument.getFunctionName());
					@SuppressWarnings("rawtypes")
					Class<? extends Feature> firstArgumentClass = null;
					if (firstArgumentClasses!=null && firstArgumentClasses.size()>0)
						firstArgumentClass = firstArgumentClasses.get(0);
					if (firstArgumentClass!=null && AddressFunction.class.isAssignableFrom(firstArgumentClass)) {
						// Our first argument is an address function
						// We need to pull it out of the internal descriptor, and wrap it into an ExplicitAddressFeature
						
						// create a descriptor for the ExplicitAddressFeature
						String descriptor = this.getFeatureClassDescriptors(ExplicitAddressFeature.class).get(0);
						FunctionDescriptor explicitAddressDescriptor = this.getFeatureService().getFunctionDescriptor(descriptor);
						explicitAddressDescriptor.setDescriptorName(functionDescriptor.getDescriptorName());
						FunctionDescriptor internalDescriptor = this.getFeatureService().getFunctionDescriptor(functionDescriptor.getFunctionName());
						
						// add the ParseConfigurationAddressFeature or PosTaggedTokenFeature as an argument
						explicitAddressDescriptor.addArgument(internalDescriptor);
						
						// add the address function as an argument
						explicitAddressDescriptor.addArgument(functionDescriptor.getArguments().get(0));
						
						// add all other arguments back to the ParseConfigurationAddressFeature or PosTaggedTokenFeature
						for (int i=1; i<functionDescriptor.getArguments().size();i++) {
							internalDescriptor.addArgument(functionDescriptor.getArguments().get(i));
						}
						descriptors.add(explicitAddressDescriptor);
					} // first argument is an address function
				} // first argument is a function
			} // has arguments
		} // is a PosTaggedTokenFeature or ParseConfigurationAddressFeature
		
		if (descriptors.size()==0) {
			descriptors.add(functionDescriptor);
		}
		return descriptors;
	}

	private static class ParseConfigurationFeatureWrapper<Y> extends AbstractFeature<ParseConfigurationWrapper, Y> implements
			ParseConfigurationFeature<Y> {
		private Feature<ParseConfigurationWrapper,Y> wrappedFeature = null;
		
		public ParseConfigurationFeatureWrapper(
				Feature<ParseConfigurationWrapper, Y> wrappedFeature) {
			super();
			this.wrappedFeature = wrappedFeature;
			this.setName(wrappedFeature.getName());
			this.setGroupName(wrappedFeature.getGroupName());
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
	
	private class ParseConfigurationBooleanFeatureWrapper extends ParseConfigurationFeatureWrapper<Boolean> implements BooleanFeature<ParseConfigurationWrapper> {
		public ParseConfigurationBooleanFeatureWrapper(
				Feature<ParseConfigurationWrapper, Boolean> wrappedFeature) {
			super(wrappedFeature);
		}
	}
	
	private class ParseConfigurationStringFeatureWrapper extends ParseConfigurationFeatureWrapper<String> implements StringFeature<ParseConfigurationWrapper> {
		public ParseConfigurationStringFeatureWrapper(
				Feature<ParseConfigurationWrapper, String> wrappedFeature) {
			super(wrappedFeature);
		}
	}
	
	private class ParseConfigurationDoubleFeatureWrapper extends ParseConfigurationFeatureWrapper<Double> implements DoubleFeature<ParseConfigurationWrapper> {
		public ParseConfigurationDoubleFeatureWrapper(
				Feature<ParseConfigurationWrapper, Double> wrappedFeature) {
			super(wrappedFeature);
		}
	}
	
	private class ParseConfigurationIntegerFeatureWrapper extends ParseConfigurationFeatureWrapper<Integer> implements IntegerFeature<ParseConfigurationWrapper> {
		public ParseConfigurationIntegerFeatureWrapper(
				Feature<ParseConfigurationWrapper, Integer> wrappedFeature) {
			super(wrappedFeature);
		}
	}

	@Override
	protected void injectDependencies(@SuppressWarnings("rawtypes") Feature feature) {
		// no dependencies to inject
	}
}
