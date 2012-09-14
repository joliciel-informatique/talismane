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

import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.utils.features.AbstractFeature;
import com.joliciel.talismane.utils.features.AbstractFeatureParser;
import com.joliciel.talismane.utils.features.Feature;
import com.joliciel.talismane.utils.features.FeatureClassContainer;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.features.FeatureService;
import com.joliciel.talismane.utils.features.FunctionDescriptor;

class ParserFeatureParser extends AbstractFeatureParser<ParseConfiguration> {
	private ParserFeatureServiceInternal parserFeatureServiceInternal;
	
	public ParserFeatureParser(FeatureService featureService) {
		super(featureService);
	}	

	@Override
	public void addFeatureClasses(FeatureClassContainer container) {
		container.addFeatureClass("PosTag", ParserPosTagFeature.class);
		container.addFeatureClass("LexicalForm", ParserLexicalFormFeature.class);
		container.addFeatureClass("Lemma", ParserLemmaFeature.class);
		container.addFeatureClass("Morphology", ParserMorphologyFeature.class);
		container.addFeatureClass("Gender", ParserGenderFeature.class);
		container.addFeatureClass("Number", ParserNumberFeature.class);
		container.addFeatureClass("Person", ParserPersonFeature.class);
		container.addFeatureClass("Tense", ParserTenseFeature.class);
		container.addFeatureClass("Category", ParserCategoryFeature.class);
		container.addFeatureClass("SubCategory", ParserSubCategoryFeature.class);
		container.addFeatureClass("PossessorNumber", ParserPossessorNumberFeature.class);
		container.addFeatureClass("DependencyLabel", DependencyLabelFeature.class);
		container.addFeatureClass("DependentCountIf", DependentCountIf.class);
		container.addFeatureClass("BetweenCountIf", BetweenCountIf.class);
		container.addFeatureClass("Distance", DistanceFeature.class);
		container.addFeatureClass("Index", ParserIndexFeature.class);
		container.addFeatureClass("Stack", AddressFunctionStack.class);
		container.addFeatureClass("Buffer", AddressFunctionBuffer.class);
		container.addFeatureClass("Head", AddressFunctionHead.class);
		container.addFeatureClass("LDep", AddressFunctionLDep.class);
		container.addFeatureClass("RDep", AddressFunctionRDep.class);
		container.addFeatureClass("Dep", AddressFunctionDep.class);
		container.addFeatureClass("Offset", AddressFunctionOffset.class);
		container.addFeatureClass("Seq", AddressFunctionSequence.class);
		container.addFeatureClass("ExplicitAddress", ExplicitAddressFeature.class);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<ParseConfigurationFeature<?>> parseDescriptor(FunctionDescriptor functionDescriptor) {
		List<Feature<ParseConfiguration, ?>> tokenFeatures = this.parse(functionDescriptor);
		List<ParseConfigurationFeature<?>> wrappedFeatures = new ArrayList<ParseConfigurationFeature<?>>();
		for (Feature<ParseConfiguration, ?> tokenFeature : tokenFeatures) {
			ParseConfigurationFeature<?> wrappedFeature = null;
			if (tokenFeature instanceof ParseConfigurationFeature) {
				wrappedFeature = (ParseConfigurationFeature<?>) tokenFeature;
			} else {
				wrappedFeature = new ParseConfigurationFeatureWrapper(tokenFeature);
			}
			wrappedFeatures.add(wrappedFeature);
		}
		return wrappedFeatures;
	}
	
	@Override
	protected Object parseArgument(FunctionDescriptor argumentDescriptor) {
		return null;
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

		if (featureClass!=null && ParseConfigurationAddressFeature.class.isAssignableFrom(featureClass)) {
			if (functionDescriptor.getArguments().size()>0) {
				FunctionDescriptor explicitAddressDescriptor = this.getFeatureService().getFunctionDescriptor("ExplicitAddress");
				FunctionDescriptor internalDescriptor = this.getFeatureService().getFunctionDescriptor(functionDescriptor.getFunctionName());
				explicitAddressDescriptor.addArgument(internalDescriptor);
				explicitAddressDescriptor.addArgument(functionDescriptor.getArguments().get(0));
				descriptors.add(explicitAddressDescriptor);
			}
		}
		
		if (descriptors.size()==0) {
			descriptors.add(functionDescriptor);
		}
		return descriptors;
	}

	private static class ParseConfigurationFeatureWrapper<Y> extends AbstractFeature<ParseConfiguration, Y> implements
			ParseConfigurationFeature<Y> {
		private Feature<ParseConfiguration,Y> wrappedFeature = null;
		
		public ParseConfigurationFeatureWrapper(
				Feature<ParseConfiguration, Y> wrappedFeature) {
			super();
			this.wrappedFeature = wrappedFeature;
			this.setName(wrappedFeature.getName());
			this.setGroupName(wrappedFeature.getGroupName());
		}
		
		@Override
		public FeatureResult<Y> check(ParseConfiguration context) {
			return wrappedFeature.check(context);
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		public Class<? extends Feature> getFeatureType() {
			return wrappedFeature.getFeatureType();
		}
	}
}
