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
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.tokeniser.patterns.TokenPatternMatch;
import com.joliciel.talismane.utils.PerformanceMonitor;

class TokenPatternMatchFeatureParser extends AbstractFeatureParser<TokenPatternMatch> {
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(TokenPatternMatchFeatureParser.class);

	TokenFeatureParser tokenFeatureParser;
	
	public TokenPatternMatchFeatureParser(FeatureService featureService) {
		super(featureService);
	}	

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<TokenPatternMatchFeature<?>> parseDescriptor(FunctionDescriptor descriptor) {
		MONITOR.startTask("parseDescriptor");
		try {
			List<TokenPatternMatchFeature<?>> wrappedFeatures = new ArrayList<TokenPatternMatchFeature<?>>();

			List<Feature<TokenPatternMatch, ?>> tokenPatternMatchFeatures = this.parse(descriptor);
			
			for (Feature<TokenPatternMatch, ?> tokenPatternMatchFeature : tokenPatternMatchFeatures) {
				TokenPatternMatchFeature<?> wrappedFeature = null;
				if (tokenPatternMatchFeature instanceof TokenPatternMatchFeature) {
					wrappedFeature = (TokenPatternMatchFeature<?>) tokenPatternMatchFeature;
				} else {
					wrappedFeature = new TokenPatternMatchFeatureWrapper(tokenPatternMatchFeature);
				}
				wrappedFeatures.add(wrappedFeature);
			}

			return wrappedFeatures;
		} finally {
			MONITOR.endTask("parseDescriptor");
		}
	}


	@Override
	public void addFeatureClasses(FeatureClassContainer container) {
		container.addFeatureClass("PatternOffset", PatternMatchOffsetAddressFunction.class);
		container.addFeatureClass("PatternWordForm", PatternMatchWordForm.class);
		container.addFeatureClass("PatternIndexInSentence", PatternMatchIndexInSentence.class);
		this.tokenFeatureParser.addFeatureClasses(container);
	}
	
	@Override
	public List<FunctionDescriptor> getModifiedDescriptors(
			FunctionDescriptor functionDescriptor) {
		return tokenFeatureParser.getModifiedDescriptors(functionDescriptor);
	}


	@SuppressWarnings({ "rawtypes" })
	@Override
	protected void injectDependencies(Feature feature) {
		// nothing to do
	}
	
	public TokenFeatureParser getTokenFeatureParser() {
		return tokenFeatureParser;
	}

	public void setTokenFeatureParser(
			TokenFeatureParser tokenFeatureParser) {
		this.tokenFeatureParser = tokenFeatureParser;
	}

	private static class TokenPatternMatchFeatureWrapper<T> extends AbstractFeature<TokenPatternMatch,T> implements
		TokenPatternMatchFeature<T>, FeatureWrapper<TokenPatternMatch, T> {
		private Feature<TokenPatternMatch,T> wrappedFeature = null;
		
		public TokenPatternMatchFeatureWrapper(
				Feature<TokenPatternMatch, T> wrappedFeature) {
			super();
			this.wrappedFeature = wrappedFeature;
			this.setName(wrappedFeature.getName());
			this.setCollectionName(wrappedFeature.getCollectionName());
		}
		
		@Override
		public FeatureResult<T> check(TokenPatternMatch context, RuntimeEnvironment env) {
			return wrappedFeature.check(context, env);
		}
		
		@Override
		public Feature<TokenPatternMatch, T> getWrappedFeature() {
			return wrappedFeature;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Class<? extends Feature> getFeatureType() {
			return wrappedFeature.getFeatureType();
		}
	}

	@Override
	protected boolean canConvert(Class<?> parameterType,
			Class<?> originalArgumentType) {
		return false;
	}

	@Override
	protected Feature<TokenPatternMatch, ?> convertArgument(
			Class<?> parameterType,
			Feature<TokenPatternMatch, ?> originalArgument) {
		return null;
	}

	
}
