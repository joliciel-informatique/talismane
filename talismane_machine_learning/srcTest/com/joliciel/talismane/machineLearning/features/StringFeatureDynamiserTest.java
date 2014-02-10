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
package com.joliciel.talismane.machineLearning.features;

import static org.junit.Assert.*;

import org.junit.Test;

public class StringFeatureDynamiserTest {

	@Test
	public void testGetBuilder() {
		StringLengthTestFeature stringLengthFeature = new StringLengthTestFeature();
		IntegerToDoubleFeature<String> stringLengthDoubleFeature = new IntegerToDoubleFeature<String>(stringLengthFeature);
		DoubleLiteralFeature<String> doubleFeature1 = new DoubleLiteralFeature<String>(1);
		DoubleLiteralFeature<String> doubleFeature5 = new DoubleLiteralFeature<String>(5);
		
		GreaterThanOperator<String> greaterThan = new GreaterThanOperator<String>(stringLengthDoubleFeature, doubleFeature1);
		
		LessThanOperator<String> lessThan = new LessThanOperator<String>(stringLengthDoubleFeature, doubleFeature5);
		
		@SuppressWarnings("unchecked")
		AndFeature<String> andFeature = new AndFeature<String>(greaterThan, lessThan);
		
		StringFeatureDynamiser manager = new StringFeatureDynamiser(String.class);
		DynamicSourceCodeBuilder<String> builder = manager.getBuilder(andFeature);
		@SuppressWarnings("unchecked")
		BooleanFeature<String> newAndFeature = (BooleanFeature<String>) builder.getFeature();
		RuntimeEnvironment env = new RuntimeEnvironmentImpl();
		FeatureResult<Boolean> result = newAndFeature.check("blah", env);
		assertTrue(result.getOutcome());
		result = newAndFeature.check("a", env);
		assertFalse(result.getOutcome());
		result = newAndFeature.check("extensively", env);
		assertFalse(result.getOutcome());
	}

	@Test
	public void testBuildFromParser() {
		FeatureServiceLocator featureServiceLocator = FeatureServiceLocator.getInstance();
		FeatureService featureService = featureServiceLocator.getFeatureService();
		FunctionDescriptorParser functionDescriptorParser = featureService.getFunctionDescriptorParser();
		
		TestParser parser = new TestParser(featureService);
		
		FunctionDescriptor descriptor = functionDescriptorParser.parseDescriptor("IfThenElse(IsNull(Substring(5,3)),\"Null\",Substring(5,3))");
		@SuppressWarnings("unchecked")
		Feature<String,String> feature = (Feature<String, String>) parser.parse(descriptor).get(0);
		
		RuntimeEnvironment env = featureService.getRuntimeEnvironment();
		String context = "blahdiblah";
		String result = feature.check(context, env).getOutcome();
		assertEquals("Null",result);
		
		StringFeatureDynamiser manager = new StringFeatureDynamiser(String.class);
		DynamicSourceCodeBuilder<String> builder = manager.getBuilder(feature);
		@SuppressWarnings("unchecked")
		StringFeature<String> newFeature = (StringFeature<String>) builder.getFeature();
		result = newFeature.check(context, env).getOutcome();
		assertEquals("Null",result);
		
		FunctionDescriptor descriptor2 = functionDescriptorParser.parseDescriptor("IfThenElse(IsNull(Substring(3,5)),\"Null\",Substring(3,5))");
		@SuppressWarnings("unchecked")
		Feature<String,String> feature2 = (Feature<String, String>) parser.parse(descriptor2).get(0);
		
		RuntimeEnvironment env2 = featureService.getRuntimeEnvironment();
		String context2 = "blahdiblah";
		String result2 = feature2.check(context2, env2).getOutcome();
		assertEquals("hd",result2);
		
		DynamicSourceCodeBuilder<String> builder2 = manager.getBuilder(feature2);
		@SuppressWarnings("unchecked")
		StringFeature<String> newFeature2 = (StringFeature<String>) builder2.getFeature();
		result = newFeature2.check(context2, env2).getOutcome();
		assertEquals("hd",result2);
	}
}
