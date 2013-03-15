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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.joliciel.talismane.machineLearning.features.AbstractFeature;
import com.joliciel.talismane.machineLearning.features.AbstractFeatureParser;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureClassContainer;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.FeatureWrapper;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;
import com.joliciel.talismane.utils.PerformanceMonitor;

class TokeniserContextFeatureParser extends AbstractFeatureParser<TokeniserContext> {
	TokenFeatureParser tokenFeatureParser;
	private List<TokenPattern> patternList;
	private Map<String,TokenPattern> patternMap;
	
	public TokeniserContextFeatureParser(FeatureService featureService) {
		super(featureService);
	}	

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<TokeniserContextFeature<?>> parseDescriptor(FunctionDescriptor descriptor) {
		PerformanceMonitor.startTask("TokeniserContextFeatureParser.parseDescriptor");
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
			PerformanceMonitor.endTask("TokeniserContextFeatureParser.parseDescriptor");
		}
	}


	@Override
	public void addFeatureClasses(FeatureClassContainer container) {
		container.addFeatureClass("InsidePatternNgram", InsidePatternNgramFeature.class);
		container.addFeatureClass("PatternOffset", TokeniserPatternOffsetFeature.class);
		container.addFeatureClass("PatternWordForm", PatternWordForm.class);
		container.addFeatureClass("PatternIndexInSentence", PatternIndexInSentence.class);
		container.addFeatureClass("TokeniserPatterns", TokeniserPatternsFeature.class);
		container.addFeatureClass("TokeniserPatternsAndIndexes", TokeniserPatternsAndIndexesFeature.class);
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
				// do nothing in particular
			} else if (featureClass.equals(PatternWordForm.class)
					||featureClass.equals(PatternIndexInSentence.class)
					||featureClass.equals(TokeniserPatternOffsetFeature.class)) {
				// do nothing in particular
			} else if (featureClass.equals(TokenReferenceFeature.class)) {
				// do nothing in particular
			} else if (TokenFeature.class.isAssignableFrom(featureClass)) {
				// if the first argument is an address reference to another token
				// we need to replace the whole thing with a TokenReferenceFeature
				if (functionDescriptor.getArguments().size()>0) {
					FunctionDescriptor firstArgument = functionDescriptor.getArguments().get(0);
					if (firstArgument.isFunction()) {
						@SuppressWarnings("rawtypes")
						List<Class<? extends Feature>> firstArgumentClasses = this.getFeatureClasses(firstArgument.getFunctionName());
						@SuppressWarnings("rawtypes")
						Class<? extends Feature> firstArgumentClass = null;
						if (firstArgumentClasses!=null && firstArgumentClasses.size()>0)
							firstArgumentClass = firstArgumentClasses.get(0);
						if (firstArgumentClass!=null && TokenAddressFunction.class.isAssignableFrom(firstArgumentClass)) {
							// Our first argument is a token address function
							// We need to pull it out of the internal descriptor, and wrap it into a TokenReferenceFeature
							
							// create a descriptor for the TokenReferenceFeature
							String descriptor = this.getFeatureClassDescriptors(TokenReferenceFeature.class).get(0);
							FunctionDescriptor explicitAddressDescriptor = this.getFeatureService().getFunctionDescriptor(descriptor);
							explicitAddressDescriptor.setDescriptorName(functionDescriptor.getDescriptorName());
							FunctionDescriptor internalDescriptor = this.getFeatureService().getFunctionDescriptor(functionDescriptor.getFunctionName());
							
							// add the TokenAddressFunction as an argument
							explicitAddressDescriptor.addArgument(functionDescriptor.getArguments().get(0));
							
							// add the TokenFeature as an argument
							explicitAddressDescriptor.addArgument(internalDescriptor);
							
							// add all other arguments back to the TokenFeature
							for (int i=1; i<functionDescriptor.getArguments().size();i++) {
								internalDescriptor.addArgument(functionDescriptor.getArguments().get(i));
							}
							descriptors.add(explicitAddressDescriptor);
						} // first argument is an address function
					} // first argument is a function
				} // has arguments
			} // which feature class?
		} // have a feature class?
		
		if (descriptors.size()==0) {
			descriptors = tokenFeatureParser.getModifiedDescriptors(functionDescriptor);
		}
		
		return descriptors;
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void injectDependencies(Feature feature) {
		if (feature instanceof InsidePatternNgramFeature) {
			((InsidePatternNgramFeature) feature).setPatternMap(patternMap);
		} else if (feature instanceof PatternWordForm) {
			((PatternWordForm) feature).setPatternMap(patternMap);
		} else if (feature instanceof PatternIndexInSentence) {
			((PatternIndexInSentence) feature).setPatternMap(patternMap);
		} else if (feature instanceof TokeniserPatternOffsetFeature) {
			((TokeniserPatternOffsetFeature) feature).setPatternMap(patternMap);
		}
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
	
	public List<TokenPattern> getPatternList() {
		return patternList;
	}

	public void setPatternList(List<TokenPattern> patternList) {
		this.patternList = patternList;
		this.patternMap = new HashMap<String, TokenPattern>();
		for (TokenPattern tokenPattern : this.patternList) {
			this.patternMap.put(tokenPattern.getName(), tokenPattern);
		}
	}

	
}
