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

import static org.junit.Assert.*;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.machineLearning.features.AndFeature;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.ConcatenateFeature;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.FeatureWrapper;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptorParser;
import com.joliciel.talismane.machineLearning.features.OrFeatureAllowNulls;
import com.joliciel.talismane.machineLearning.features.StringFeature;

public class TokeniserContextFeatureParserTest {
	private static final Log LOG = LogFactory.getLog(TokeniserContextFeatureParserTest.class);

	@Test
	public void testParseAndFeature() {
		TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance("");
		FeatureService featureService = talismaneServiceLocator.getFeatureServiceLocator().getFeatureService();
		FunctionDescriptorParser functionDescriptorParser = featureService.getFunctionDescriptorParser();
		
		TokenFeatureParser tokenFeatureParser = new TokenFeatureParserImpl(featureService);
		TokeniserContextFeatureParser parser = new TokeniserContextFeatureParser(featureService);
		parser.setTokenFeatureParser(tokenFeatureParser);
		
		FunctionDescriptor descriptor = functionDescriptorParser.parseDescriptor("And(FirstWordInSentence(),UnknownWord(),Regex(\".+ .+\"))");
		List<Feature<TokeniserContext, ?>> features = parser.parse(descriptor);
		assertEquals(1, features.size());
		Feature<TokeniserContext,?> feature = features.get(0);
		assertTrue(feature instanceof AndFeature);
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		BooleanFeature<TokenWrapper>[] booleanFeatures = ((AndFeature)feature).getBooleanFeatures();
		assertEquals(3, booleanFeatures.length);
		
		assertTrue(booleanFeatures[0] instanceof FirstWordInSentenceFeature);
		assertTrue(booleanFeatures[1] instanceof UnknownWordFeature);
		assertTrue(booleanFeatures[2] instanceof RegexFeature);
	}
	
	@Test
	public void testParseWordFeature() {
		TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance("");
		FeatureService featureService = talismaneServiceLocator.getFeatureServiceLocator().getFeatureService();
		FunctionDescriptorParser functionDescriptorParser = featureService.getFunctionDescriptorParser();
		
		TokenFeatureParser tokenFeatureParser = new TokenFeatureParserImpl(featureService);
		TokeniserContextFeatureParser parser = new TokeniserContextFeatureParser(featureService);
		parser.setTokenFeatureParser(tokenFeatureParser);
		FunctionDescriptor descriptor = functionDescriptorParser.parseDescriptor("Word(\"le\",\"la\",\"les\",\"l'\")");
		List<Feature<TokeniserContext, ?>> features = parser.parse(descriptor);
		assertEquals(1, features.size());
		Feature<?,?> feature = features.get(0);
		assertTrue(feature instanceof WordFeature);
		
		StringFeature<TokenWrapper>[] words = ((WordFeature)feature).getWords();
		assertEquals(4, words.length);
		
		assertEquals("\"le\"", words[0].getName());
		assertEquals("\"la\"", words[1].getName());
		assertEquals("\"les\"", words[2].getName());
		assertEquals("\"l'\"", words[3].getName());
	}

	@Test
	public void testParseConcatenateFeature() {
		TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance("");
		FeatureService featureService = talismaneServiceLocator.getFeatureServiceLocator().getFeatureService();
		FunctionDescriptorParser functionDescriptorParser = featureService.getFunctionDescriptorParser();
		
		TokenFeatureParser tokenFeatureParser = new TokenFeatureParserImpl(featureService);
		TokeniserContextFeatureParser parser = new TokeniserContextFeatureParser(featureService);
		parser.setTokenFeatureParser(tokenFeatureParser);
		
		FunctionDescriptor descriptor = functionDescriptorParser.parseDescriptor("Concat(WordForm(),NLetterPrefix(5),NLetterSuffix(3))");
		List<Feature<TokeniserContext, ?>> features = parser.parse(descriptor);
		assertEquals(1, features.size());
		Feature<TokeniserContext,?> feature = features.get(0);
		assertEquals(ConcatenateFeature.class.getSimpleName(), feature.getClass().getSimpleName());
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		StringFeature<TokenWrapper>[] stringFeatures = ((ConcatenateFeature)feature).getStringFeatures();
		assertEquals(3, stringFeatures.length);
		
		assertTrue(stringFeatures[0] instanceof WordFormFeature);
		assertTrue(stringFeatures[1] instanceof NLetterPrefixFeature);
		assertTrue(stringFeatures[2] instanceof NLetterSuffixFeature);
	}
	
	
	@SuppressWarnings({ "unchecked" })
	@Test
	public void testNamedFeatures() {
		TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance("");
		FeatureService featureService = talismaneServiceLocator.getFeatureServiceLocator().getFeatureService();
		FunctionDescriptorParser functionDescriptorParser = featureService.getFunctionDescriptorParser();
		
		TokenFeatureParser tokenFeatureParser = new TokenFeatureParserImpl(featureService);
		TokeniserContextFeatureParser parser = new TokeniserContextFeatureParser(featureService);
		parser.setTokenFeatureParser(tokenFeatureParser);
		
		FunctionDescriptor descriptor = functionDescriptorParser.parseDescriptor("IsMonthName\tWord(\"janvier\",\"février\",\"mars\",\"avril\",\"mai\",\"juin\",\"juillet\",\"août\",\"septembre\",\"octobre\",\"novembre\",\"décembre\")");
		List<Feature<TokeniserContext, ?>> features = parser.parse(descriptor);
		assertEquals(1, features.size());
		Feature<?,?> feature = features.get(0);
		if (feature instanceof FeatureWrapper)
			feature = ((FeatureWrapper<TokeniserContext,?>) feature).getWrappedFeature();
		LOG.debug(feature.getClass());
		assertTrue(feature instanceof WordFeature);
		WordFeature wordFeature = (WordFeature) feature;
		assertEquals("IsMonthName", feature.getName());
		assertEquals(12, wordFeature.words.length);
	}
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void testOrOperator() {
		TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance("");
		FeatureService featureService = talismaneServiceLocator.getFeatureServiceLocator().getFeatureService();
		FunctionDescriptorParser functionDescriptorParser = featureService.getFunctionDescriptorParser();
		
		TokenFeatureParser tokenFeatureParser = new TokenFeatureParserImpl(featureService);
		TokeniserContextFeatureParser parser = new TokeniserContextFeatureParser(featureService);
		parser.setTokenFeatureParser(tokenFeatureParser);
		
		FunctionDescriptor descriptor = functionDescriptorParser.parseDescriptor("(WordForm()==\"le\")|(WordForm()==\"la\")");
		List<Feature<TokeniserContext, ?>> features = parser.parse(descriptor);
		Feature<TokeniserContext,?> feature = features.get(0);
		if (feature instanceof FeatureWrapper)
			feature = ((FeatureWrapper<TokeniserContext,?>) feature).getWrappedFeature();
		LOG.debug(feature.getClass());
		assertEquals(OrFeatureAllowNulls.class.getSimpleName(), feature.getClass().getSimpleName());


	}

}
