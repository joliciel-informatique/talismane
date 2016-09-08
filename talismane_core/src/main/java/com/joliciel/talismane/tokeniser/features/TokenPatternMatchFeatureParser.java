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
package com.joliciel.talismane.tokeniser.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.features.AbstractFeature;
import com.joliciel.talismane.machineLearning.features.AbstractFeatureParser;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureClassContainer;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureWrapper;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptorParser;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.tokeniser.patterns.TokenPatternMatch;
import com.joliciel.talismane.utils.PerformanceMonitor;

public class TokenPatternMatchFeatureParser extends AbstractFeatureParser<TokenPatternMatch> {
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(TokenPatternMatchFeatureParser.class);

	private final TalismaneSession talismaneSession;

	public TokenPatternMatchFeatureParser(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
		this.setExternalResourceFinder(talismaneSession.getExternalResourceFinder());
	}

	public Set<TokenPatternMatchFeature<?>> getTokenPatternMatchFeatureSet(List<String> featureDescriptors) {
		Set<TokenPatternMatchFeature<?>> features = new TreeSet<TokenPatternMatchFeature<?>>();
		FunctionDescriptorParser descriptorParser = new FunctionDescriptorParser();

		MONITOR.startTask("findFeatureSet");
		try {
			for (String featureDescriptor : featureDescriptors) {
				if (featureDescriptor.length() > 0 && !featureDescriptor.startsWith("#")) {
					FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
					List<TokenPatternMatchFeature<?>> myFeatures = this.parseDescriptor(functionDescriptor);
					MONITOR.startTask("add features");
					try {
						features.addAll(myFeatures);
					} finally {
						MONITOR.endTask();
					}
				}
			}
		} finally {
			MONITOR.endTask();
		}
		return features;
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
			MONITOR.endTask();
		}
	}

	@Override
	public void addFeatureClasses(FeatureClassContainer container) {
		container.addFeatureClass("CurrentPattern", PatternNameFeature.class);
		container.addFeatureClass("CurrentGroup", PatternGroupNameFeature.class);
		container.addFeatureClass("PatternOffset", PatternMatchOffsetAddressFunction.class);
		container.addFeatureClass("PatternWordForm", PatternMatchWordFormFeature.class);
		container.addFeatureClass("PatternIndexInSentence", PatternMatchIndexInSentenceFeature.class);
		TokenFeatureParser.addFeatureClasses(container);
	}

	@Override
	public List<FunctionDescriptor> getModifiedDescriptors(FunctionDescriptor functionDescriptor) {
		List<FunctionDescriptor> descriptors = new ArrayList<FunctionDescriptor>();
		descriptors.add(functionDescriptor);
		return descriptors;
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public void injectDependencies(Feature feature) {
		TokenFeatureParser.injectDependencies(feature, talismaneSession);
	}

	private static class TokenPatternMatchFeatureWrapper<T> extends AbstractFeature<TokenPatternMatch, T>
			implements TokenPatternMatchFeature<T>, FeatureWrapper<TokenPatternMatch, T> {
		private Feature<TokenPatternMatch, T> wrappedFeature = null;

		public TokenPatternMatchFeatureWrapper(Feature<TokenPatternMatch, T> wrappedFeature) {
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
	protected boolean canConvert(Class<?> parameterType, Class<?> originalArgumentType) {
		return false;
	}

	@Override
	protected Feature<TokenPatternMatch, ?> convertArgument(Class<?> parameterType, Feature<TokenPatternMatch, ?> originalArgument) {
		return null;
	}

	@Override
	public Feature<TokenPatternMatch, ?> convertFeatureCustomType(Feature<TokenPatternMatch, ?> feature) {
		return null;
	}

}
