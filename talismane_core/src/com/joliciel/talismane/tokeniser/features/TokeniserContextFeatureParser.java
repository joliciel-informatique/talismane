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
package com.joliciel.talismane.tokeniser.features;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.machineLearning.features.AbstractFeature;
import com.joliciel.talismane.machineLearning.features.AbstractFeatureParser;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureClassContainer;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.FeatureWrapper;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;
import com.joliciel.talismane.utils.PerformanceMonitor;

class TokeniserContextFeatureParser extends AbstractFeatureParser<TokeniserContext> {
	TokenFeatureParser tokenFeatureParser;
	private List<TokenPattern> patternList;

	public TokeniserContextFeatureParser(FeatureService featureService) {
		super(featureService);
	}	

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<TokeniserContextFeature<?>> parseDescriptor(FunctionDescriptor functionDescriptor) {
		PerformanceMonitor.startTask("TokeniserContextFeatureParser.parseDescriptor");
		try {
			List<Feature<TokeniserContext, ?>> tokeniserContextFeatures = this.parse(functionDescriptor);
			List<TokeniserContextFeature<?>> wrappedFeatures = new ArrayList<TokeniserContextFeature<?>>();
			for (Feature<TokeniserContext, ?> tokeniserContextFeature : tokeniserContextFeatures) {
				TokeniserContextFeature<?> wrappedFeature = null;
				if (tokeniserContextFeature instanceof TokeniserContextFeature) {
					wrappedFeature = (TokeniserContextFeature<?>) tokeniserContextFeature;
				} else {
					wrappedFeature = new TokeniserContextFeatureWrapper(tokeniserContextFeature);
				}
				wrappedFeatures.add(wrappedFeature);
			}
			return wrappedFeatures;
		} finally {
			PerformanceMonitor.endTask("TokeniserContextFeatureParser.parseDescriptor");
		}
	}
	
	
	@Override
	protected Object parseArgument(FunctionDescriptor argumentDescriptor) {
		return null;
	}

	@Override
	public void addFeatureClasses(FeatureClassContainer container) {
		container.addFeatureClass("InsidePatternNgram", InsidePatternNgramFeature.class);
		this.tokenFeatureParser.addFeatureClasses(container);
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
		
		if (featureClass!=null) {
			if (featureClass.equals(InsidePatternNgramFeature.class)) {
				for (TokenPattern tokeniserPattern : patternList) {
					boolean firstIndex = true;
					for (int indexToTest : tokeniserPattern.getIndexesToTest()) {
						if (firstIndex)
							firstIndex = false;
						else {
							FunctionDescriptor descriptor = this.getFeatureService().getFunctionDescriptor(functionName);
							descriptor.addArgument(tokeniserPattern);
							descriptor.addArgument(indexToTest);
							descriptors.add(descriptor);
						}
					} // next index
				} // next pattern
			} // which feature class?
		} // have a feature class?
		
		if (descriptors.size()==0) {
			descriptors = tokenFeatureParser.getModifiedDescriptors(functionDescriptor);
		}
		
		return descriptors;
	}

	public TokenFeatureParser getTokenFeatureParser() {
		return tokenFeatureParser;
	}

	public void setTokenFeatureParser(
			TokenFeatureParser tokenFeatureParser) {
		this.tokenFeatureParser = tokenFeatureParser;
	}

	private static class TokeniserContextFeatureWrapper<T> extends AbstractFeature<TokeniserContext,T> implements
		TokeniserContextFeature<T>, FeatureWrapper<TokeniserContext, T> {
		private Feature<TokeniserContext,T> wrappedFeature = null;
		
		public TokeniserContextFeatureWrapper(
				Feature<TokeniserContext, T> wrappedFeature) {
			super();
			this.wrappedFeature = wrappedFeature;
			this.setName(wrappedFeature.getName());
			this.setGroupName(wrappedFeature.getGroupName());
		}
		
		@Override
		public FeatureResult<T> check(TokeniserContext context) {
			return wrappedFeature.check(context);
		}
		
		@Override
		public Feature<TokeniserContext, T> getWrappedFeature() {
			return wrappedFeature;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Class<? extends Feature> getFeatureType() {
			return wrappedFeature.getFeatureType();
		}
	}
	
	public List<TokenPattern> getPatternList() {
		return patternList;
	}

	public void setPatternList(List<TokenPattern> patternList) {
		this.patternList = patternList;
	}
	
	
}
