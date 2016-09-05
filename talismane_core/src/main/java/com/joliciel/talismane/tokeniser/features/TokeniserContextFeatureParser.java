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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;
import com.joliciel.talismane.utils.PerformanceMonitor;

public class TokeniserContextFeatureParser extends AbstractFeatureParser<TokeniserContext> {
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(TokeniserContextFeatureParser.class);

	private final List<TokenPattern> patternList;
	private final Map<String, TokenPattern> patternMap;
	private final TalismaneSession talismaneSession;

	public TokeniserContextFeatureParser(TalismaneSession talismaneSession, List<TokenPattern> patternList) {
		this.talismaneSession = talismaneSession;
		this.setExternalResourceFinder(talismaneSession.getExternalResourceFinder());

		this.patternList = patternList;
		this.patternMap = new HashMap<String, TokenPattern>();
		for (TokenPattern tokenPattern : this.patternList) {
			this.patternMap.put(tokenPattern.getName(), tokenPattern);
		}
	}

	public Set<TokeniserContextFeature<?>> getTokeniserContextFeatureSet(List<String> featureDescriptors) {
		Set<TokeniserContextFeature<?>> features = new TreeSet<TokeniserContextFeature<?>>();
		FunctionDescriptorParser descriptorParser = new FunctionDescriptorParser();

		MONITOR.startTask("findFeatureSet");
		try {
			for (String featureDescriptor : featureDescriptors) {
				if (featureDescriptor.length() > 0 && !featureDescriptor.startsWith("#")) {
					FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
					List<TokeniserContextFeature<?>> myFeatures = this.parseDescriptor(functionDescriptor);
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
	public List<TokeniserContextFeature<?>> parseDescriptor(FunctionDescriptor descriptor) {
		MONITOR.startTask("parseDescriptor");
		try {
			List<TokeniserContextFeature<?>> wrappedFeatures = new ArrayList<TokeniserContextFeature<?>>();

			List<Feature<TokeniserContext, ?>> tokeniserContextFeatures = this.parse(descriptor);

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
			MONITOR.endTask();
		}
	}

	@Override
	public void addFeatureClasses(FeatureClassContainer container) {
		container.addFeatureClass("InsidePatternNgram", InsidePatternNgramFeature.class);
		container.addFeatureClass("PatternOffset", PatternOffsetAddressFunction.class);
		container.addFeatureClass("PatternWordForm", PatternWordFormFeature.class);
		container.addFeatureClass("PatternIndexInSentence", PatternIndexInSentenceFeature.class);
		container.addFeatureClass("TokeniserPatterns", TokeniserPatternsFeature.class);
		container.addFeatureClass("TokeniserPatternsAndIndexes", TokeniserPatternsAndIndexesFeature.class);
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

		if (feature instanceof InsidePatternNgramFeature) {
			((InsidePatternNgramFeature) feature).setPatternMap(patternMap);
		} else if (feature instanceof PatternWordFormFeature) {
			((PatternWordFormFeature) feature).setPatternMap(patternMap);
		} else if (feature instanceof PatternIndexInSentenceFeature) {
			((PatternIndexInSentenceFeature) feature).setPatternMap(patternMap);
		} else if (feature instanceof PatternOffsetAddressFunction) {
			((PatternOffsetAddressFunction) feature).setPatternMap(patternMap);
		}
	}

	private static class TokeniserContextFeatureWrapper<T> extends AbstractFeature<TokeniserContext, T>
			implements TokeniserContextFeature<T>, FeatureWrapper<TokeniserContext, T> {
		private Feature<TokeniserContext, T> wrappedFeature = null;

		public TokeniserContextFeatureWrapper(Feature<TokeniserContext, T> wrappedFeature) {
			super();
			this.wrappedFeature = wrappedFeature;
			this.setName(wrappedFeature.getName());
			this.setCollectionName(wrappedFeature.getCollectionName());
		}

		@Override
		public FeatureResult<T> check(TokeniserContext context, RuntimeEnvironment env) {
			return wrappedFeature.check(context, env);
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

	@Override
	protected boolean canConvert(Class<?> parameterType, Class<?> originalArgumentType) {
		return false;
	}

	@Override
	protected Feature<TokeniserContext, ?> convertArgument(Class<?> parameterType, Feature<TokeniserContext, ?> originalArgument) {
		return null;
	}

	@Override
	public Feature<TokeniserContext, ?> convertFeatureCustomType(Feature<TokeniserContext, ?> feature) {
		return null;
	}

}
