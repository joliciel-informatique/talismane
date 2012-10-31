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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureClassContainer;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerLexicon;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;

class TokenFeatureParserImpl implements TokenFeatureParser {
	private FeatureService featureService;
	private PosTaggerLexicon lexiconService;	
	private PosTagSet posTagSet;
	private List<TokenPattern> patternList;
	private FeatureClassContainer container;
	
	public TokenFeatureParserImpl(FeatureService featureService) {
		this.featureService = featureService;
	}	

	/* (non-Javadoc)
	 * @see com.joliciel.talismane.tokeniser.features.TokenFeatureParser#addFeatureClasses(com.joliciel.nlp.features.FeatureClassContainer)
	 */
	@Override
	public void addFeatureClasses(FeatureClassContainer container) {
		container.addFeatureClass("CurrentPattern", CurrentPatternFeature.class);
		container.addFeatureClass("CurrentPatternWordForm", CurrentPatternWordForm.class);
		container.addFeatureClass("FirstWordInCompound", FirstWordInCompoundFeature.class);
		container.addFeatureClass("FirstWordInSentence", FirstWordInSentenceFeature.class);
		container.addFeatureClass("LastWordInCompound", LastWordInCompoundFeature.class);
		container.addFeatureClass("LastWordInSentence", LastWordInSentenceFeature.class);
		container.addFeatureClass("LexiconAllPosTags", LexiconAllPosTagsFeature.class);
		container.addFeatureClass("LexiconPosTag", LexiconPosTagFeature.class);
		container.addFeatureClass("NLetterPrefix", NLetterPrefixFeature.class);
		container.addFeatureClass("NLetterSuffix", NLetterSuffixFeature.class);
		container.addFeatureClass("Offset", TokenOffsetFeature.class);
		container.addFeatureClass("PatternOffset", TokeniserPatternOffsetFeature.class);
		container.addFeatureClass("Regex", RegexFeature.class);
		container.addFeatureClass("WordForm", WordFormFeature.class);
		container.addFeatureClass("UnknownWord", UnknownWordFeature.class);
		container.addFeatureClass("Word", WordFeature.class);
		container.addFeatureClass("AndRange", AndRangeFeature.class);
		container.addFeatureClass("OrRange", OrRangeFeature.class);
		container.addFeatureClass("ForwardLookup", ForwardLookupFeature.class);
		container.addFeatureClass("BackwardLookup", BackwardLookupFeature.class);
		container.addFeatureClass("HasClosedClassesOnly", HasClosedClassesOnlyFeature.class);
		
		this.container = container;
	}

	/* (non-Javadoc)
	 * @see com.joliciel.talismane.tokeniser.features.TokenFeatureParser#getModifiedDescriptors(com.joliciel.nlp.features.FunctionDescriptor)
	 */
	@Override
	public List<FunctionDescriptor> getModifiedDescriptors(FunctionDescriptor functionDescriptor) {
		List<FunctionDescriptor> descriptors = new ArrayList<FunctionDescriptor>();
		String functionName = functionDescriptor.getFunctionName();
		
		@SuppressWarnings("rawtypes")
		List<Class<? extends Feature>> featureClasses = container.getFeatureClasses(functionName);
		
		@SuppressWarnings("rawtypes")
		Class<? extends Feature> featureClass = null;
		if (featureClasses!=null && featureClasses.size()>0)
			featureClass = featureClasses.get(0);
		
		if (featureClass==null) {
			descriptors.add(functionDescriptor);
		} else if (featureClass.equals(LexiconPosTagFeature.class)) {
			Set<PosTag> posTags = null;
			if (functionDescriptor.getArguments().size()>0) {
				// posTagCode already specified
				String posTagCode = (String) functionDescriptor.getArguments().get(0).getObject();
				PosTag posTag = this.posTagSet.getPosTag(posTagCode);
				posTags = new HashSet<PosTag>();
				posTags.add(posTag);
			} else {
				// no posTagCode specified - take all of 'em
				posTags = this.posTagSet.getTags();
			}
			
			PosTag[] posTagArray = new PosTag[0];
			posTagArray = posTags.toArray(posTagArray);
			
			FunctionDescriptor descriptor = this.getFeatureService().getFunctionDescriptor(functionName);
			descriptor.addArgument(posTagArray);
			descriptors.add(descriptor);
		} else if (featureClass.equals(CurrentPatternFeature.class)
				||featureClass.equals(CurrentPatternWordForm.class)) {
			TokenPattern[] patternArray = new TokenPattern[0];
			patternArray = patternList.toArray(patternArray);
			FunctionDescriptor descriptor = this.getFeatureService().getFunctionDescriptor(functionName);
			descriptor.addArgument(patternArray);
			for (FunctionDescriptor argument : functionDescriptor.getArguments())
				descriptor.addArgument(argument);
			descriptors.add(descriptor);
			
		} else if (featureClass.equals(TokeniserPatternOffsetFeature.class)) {
			TokenPattern[] patternArray = new TokenPattern[0];
			patternArray = patternList.toArray(patternArray);
			FunctionDescriptor descriptor = this.getFeatureService().getFunctionDescriptor(functionName);
			descriptor.addArgument(patternArray);
			for (FunctionDescriptor argument : functionDescriptor.getArguments())
				descriptor.addArgument(argument);
			descriptors.add(descriptor);
		} else {
			descriptors.add(functionDescriptor);
		}
		return descriptors;
	}

	/* (non-Javadoc)
	 * @see com.joliciel.talismane.tokeniser.features.TokenFeatureParser#getPosTagSet()
	 */
	@Override
	public PosTagSet getPosTagSet() {
		return posTagSet;
	}

	/* (non-Javadoc)
	 * @see com.joliciel.talismane.tokeniser.features.TokenFeatureParser#setPosTagSet(com.joliciel.talismane.posTagger.PosTagSet)
	 */
	@Override
	public void setPosTagSet(PosTagSet posTagSet) {
		this.posTagSet = posTagSet;
	}

	/* (non-Javadoc)
	 * @see com.joliciel.talismane.tokeniser.features.TokenFeatureParser#getLexiconService()
	 */
	@Override
	public PosTaggerLexicon getLexiconService() {
		return lexiconService;
	}

	/* (non-Javadoc)
	 * @see com.joliciel.talismane.tokeniser.features.TokenFeatureParser#setLexiconService(com.joliciel.talismane.posTagger.PosTaggerLexiconService)
	 */
	@Override
	public void setLexiconService(PosTaggerLexicon lexiconService) {
		this.lexiconService = lexiconService;
	}

	/* (non-Javadoc)
	 * @see com.joliciel.talismane.tokeniser.features.TokenFeatureParser#getPatternList()
	 */
	@Override
	public List<TokenPattern> getPatternList() {
		return patternList;
	}

	/* (non-Javadoc)
	 * @see com.joliciel.talismane.tokeniser.features.TokenFeatureParser#setPatternList(java.util.List)
	 */
	@Override
	public void setPatternList(List<TokenPattern> patternList) {
		this.patternList = patternList;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}
	
	
}
