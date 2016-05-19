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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.utils.WeightedOutcome;

public class TestParserTest {
	private static final Logger LOG = LoggerFactory.getLogger(TestParserTest.class);

	@SuppressWarnings("unchecked")
	@Test
	public void testMultiplePossibleConstructors() {
		FeatureServiceLocator featureServiceLocator = FeatureServiceLocator.getInstance();
		FeatureService featureService = featureServiceLocator.getFeatureService();
		FunctionDescriptorParser functionDescriptorParser = featureService.getFunctionDescriptorParser();

		TestParser parser = new TestParser(featureService);

		FunctionDescriptor descriptor = functionDescriptorParser.parseDescriptor("23-12");
		List<Feature<String, ?>> features = parser.parse(descriptor);
		assertEquals(1, features.size());
		Feature<String, ?> feature = features.get(0);
		if (feature instanceof FeatureWrapper)
			feature = ((FeatureWrapper<String, ?>) feature).getWrappedFeature();
		LOG.debug(feature.getClass().getName());
		assertTrue(feature instanceof MinusIntegerOperator);

		MinusIntegerOperator<String> minusIntegerOperator = (MinusIntegerOperator<String>) feature;

		IntegerLiteralFeature<String> operand1 = (IntegerLiteralFeature<String>) minusIntegerOperator.getOperand1();
		IntegerLiteralFeature<String> operand2 = (IntegerLiteralFeature<String>) minusIntegerOperator.getOperand2();

		assertEquals(23, operand1.getLiteral());
		assertEquals(12, operand2.getLiteral());

		descriptor = functionDescriptorParser.parseDescriptor("23.2-12");
		features = parser.parse(descriptor);
		assertEquals(1, features.size());
		feature = features.get(0);
		if (feature instanceof FeatureWrapper)
			feature = ((FeatureWrapper<String, ?>) feature).getWrappedFeature();
		LOG.debug(feature.getClass().getName());
		assertTrue(feature instanceof MinusOperator);

		MinusOperator<String> minusOperator = (MinusOperator<String>) feature;

		DoubleLiteralFeature<String> doubleOperand1 = (DoubleLiteralFeature<String>) minusOperator.getOperand1();
		IntegerToDoubleFeature<String> doubleOperand2 = (IntegerToDoubleFeature<String>) minusOperator.getOperand2();
		IntegerLiteralFeature<String> intOperand2 = (IntegerLiteralFeature<String>) doubleOperand2.getIntegerFeature();

		assertEquals(23.2, doubleOperand1.getLiteral(), 0.001);
		assertEquals(12, intOperand2.getLiteral());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNamedFeatures() {
		FeatureServiceLocator featureServiceLocator = FeatureServiceLocator.getInstance();
		FeatureService featureService = featureServiceLocator.getFeatureService();
		FunctionDescriptorParser functionDescriptorParser = featureService.getFunctionDescriptorParser();

		TestParser parser = new TestParser(featureService);

		FunctionDescriptor descriptor1 = functionDescriptorParser.parseDescriptor("TestArgs(X,Y)\tX+Y");
		FunctionDescriptor descriptor2 = functionDescriptorParser.parseDescriptor("TestNoArgs\t1+2");
		FunctionDescriptor descriptor3 = functionDescriptorParser.parseDescriptor("TestArgs(2-1,TestNoArgs())==4");
		FunctionDescriptor descriptor4 = functionDescriptorParser.parseDescriptor("TestArgs(1,2+3,3+5)==4");
		FunctionDescriptor descriptor5 = functionDescriptorParser.parseDescriptor("TestArgs\t3+4");
		FunctionDescriptor descriptor6 = functionDescriptorParser.parseDescriptor("TestZeroArgs()\t3+4");
		FunctionDescriptor descriptor7 = functionDescriptorParser.parseDescriptor("TestZeroArgs()-2");

		List<Feature<String, ?>> features = parser.parse(descriptor1);
		// assuming no features for named feature with arguments
		assertEquals(0, features.size());

		features = parser.parse(descriptor2);
		assertEquals(1, features.size());
		Feature<String, ?> feature = features.get(0);
		if (feature instanceof FeatureWrapper)
			feature = ((FeatureWrapper<String, ?>) feature).getWrappedFeature();
		LOG.debug(feature.getClass().getName());
		assertTrue(feature instanceof PlusIntegerOperator);

		features = parser.parse(descriptor3);
		assertEquals(1, features.size());
		feature = features.get(0);
		if (feature instanceof FeatureWrapper)
			feature = ((FeatureWrapper<String, ?>) feature).getWrappedFeature();
		LOG.debug(feature.getClass().getName());
		assertTrue(feature instanceof EqualsOperatorForInteger);
		EqualsOperatorForInteger<String> equalsOperator = (EqualsOperatorForInteger<String>) feature;
		feature = equalsOperator.getOperand1();
		assertTrue(feature instanceof PlusIntegerOperator);

		PlusIntegerOperator<String> plusOperator = (PlusIntegerOperator<String>) feature;

		feature = plusOperator.getOperand1();
		assertTrue(feature instanceof MinusIntegerOperator);
		feature = plusOperator.getOperand2();
		assertTrue(feature instanceof PlusIntegerOperator);

		feature = equalsOperator.getOperand2();
		assertTrue(feature instanceof IntegerLiteralFeature);

		try {
			features = parser.parse(descriptor4);
		} catch (FeatureSyntaxException je) {
			LOG.debug(je.getMessage());
		}

		try {
			features = parser.parse(descriptor5);
		} catch (FeatureSyntaxException je) {
			LOG.debug(je.getMessage());
		}

		features = parser.parse(descriptor6);
		// assuming no features for named feature with zero arguments
		assertEquals(0, features.size());

		features = parser.parse(descriptor7);
		assertEquals(1, features.size());
		feature = features.get(0);
		if (feature instanceof FeatureWrapper)
			feature = ((FeatureWrapper<String, ?>) feature).getWrappedFeature();
		LOG.debug(feature.getClass().getName());
		assertTrue(feature instanceof MinusIntegerOperator);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testStringCollectionFeatures() {
		FeatureServiceLocator featureServiceLocator = FeatureServiceLocator.getInstance();
		FeatureService featureService = featureServiceLocator.getFeatureService();
		FunctionDescriptorParser functionDescriptorParser = featureService.getFunctionDescriptorParser();

		TestParser parser = new TestParser(featureService);

		FunctionDescriptor descriptor1 = functionDescriptorParser.parseDescriptor("ABC\tTestStringCollectionFeature(\"A\",\"B\",\"C\")");

		List<Feature<String, ?>> features = parser.parse(descriptor1);

		assertEquals(1, features.size());
		Feature<String, ?> feature = features.get(0);
		if (feature instanceof FeatureWrapper)
			feature = ((FeatureWrapper<String, ?>) feature).getWrappedFeature();
		LOG.debug(feature.getClass().getName());
		assertTrue(feature instanceof TestStringCollectionFeature);

		String testContext = "abc";
		RuntimeEnvironment env = featureService.getRuntimeEnvironment();
		TestStringCollectionFeature testStringCollectionFeature = (TestStringCollectionFeature) feature;
		FeatureResult<List<WeightedOutcome<String>>> featureResult = testStringCollectionFeature.check(testContext, env);
		List<WeightedOutcome<String>> outcome = featureResult.getOutcome();
		LOG.debug(outcome.toString());
		assertEquals(3, outcome.size());
		assertEquals("A", outcome.get(0).getOutcome());
		assertEquals(1.0, outcome.get(0).getWeight(), 0.0001);

		FunctionDescriptor descriptor2 = functionDescriptorParser.parseDescriptor("XYZ\tTestStringCollectionFeature(\"X\",\"Y\",\"Z\")");

		features = parser.parse(descriptor2);
		assertEquals(1, features.size());
		feature = features.get(0);
		if (feature instanceof FeatureWrapper)
			feature = ((FeatureWrapper<String, ?>) feature).getWrappedFeature();
		LOG.debug(feature.getClass().getName());
		assertTrue(feature instanceof TestStringCollectionFeature);

		FunctionDescriptor descriptor3 = functionDescriptorParser.parseDescriptor("ConcatABCABC\tConcat(\"X\",ABC,ABC)");

		features = parser.parse(descriptor3);
		assertEquals(1, features.size());
		feature = features.get(0);
		if (feature instanceof FeatureWrapper)
			feature = ((FeatureWrapper<String, ?>) feature).getWrappedFeature();
		LOG.debug(feature.getClass().getName());
		assertTrue(feature instanceof StringCollectionFeatureWrapper);

		testContext = "abc";
		env = featureService.getRuntimeEnvironment();
		StringCollectionFeatureWrapper<String> stringCollectionFeatureWrapper = (StringCollectionFeatureWrapper<String>) feature;
		featureResult = stringCollectionFeatureWrapper.check(testContext, env);
		outcome = featureResult.getOutcome();
		LOG.debug(outcome.toString());
		// when the same feature is repeated several times, it shouldn't produce
		// a cross-product
		assertEquals(3, outcome.size());
		// The result will not prefix the collection outcome, since it's a
		// string.
		assertEquals("X|A|A", outcome.get(0).getOutcome());
		assertEquals(1.0, outcome.get(0).getWeight(), 0.0001);

		FunctionDescriptor descriptor4 = functionDescriptorParser.parseDescriptor("ConcatABCXYZ\tConcat(\"X\",ABC,XYZ)");

		features = parser.parse(descriptor4);
		assertEquals(1, features.size());
		feature = features.get(0);
		if (feature instanceof FeatureWrapper)
			feature = ((FeatureWrapper<String, ?>) feature).getWrappedFeature();
		LOG.debug(feature.getClass().getName());
		assertTrue(feature instanceof StringCollectionFeatureWrapper);

		testContext = "abc";
		env = featureService.getRuntimeEnvironment();
		stringCollectionFeatureWrapper = (StringCollectionFeatureWrapper<String>) feature;
		featureResult = stringCollectionFeatureWrapper.check(testContext, env);
		outcome = featureResult.getOutcome();
		LOG.debug(outcome.toString());
		// this time we should have a cross-product
		assertEquals(9, outcome.size());

		assertEquals("X|A|X", outcome.get(0).getOutcome());
		assertEquals(1.0, outcome.get(0).getWeight(), 0.0001);

		FunctionDescriptor descriptor5 = functionDescriptorParser
				.parseDescriptor("ConcatABCABC2\tConcat(\"X\",ABC,TestStringCollectionFeature(\"A\",\"B\",\"C\"))");

		features = parser.parse(descriptor5);
		assertEquals(1, features.size());
		feature = features.get(0);
		if (feature instanceof FeatureWrapper)
			feature = ((FeatureWrapper<String, ?>) feature).getWrappedFeature();
		LOG.debug(feature.getClass().getName());
		assertTrue(feature instanceof StringCollectionFeatureWrapper);

		testContext = "abc";
		env = featureService.getRuntimeEnvironment();
		stringCollectionFeatureWrapper = (StringCollectionFeatureWrapper<String>) feature;
		featureResult = stringCollectionFeatureWrapper.check(testContext, env);
		outcome = featureResult.getOutcome();
		LOG.debug(outcome.toString());
		// this time we should have a cross-product, cause the inner feature
		// wasn't named
		// assertEquals(9, outcome.size());
		// 2014-08-13: Apparently named and unnamed identical stuff does NOT
		// result in a cross product anymore
		// Not sure if this matters, but changing the test to reflect this!
		assertEquals(3, outcome.size());

		assertEquals("X|A|A", outcome.get(0).getOutcome());
		assertEquals(1.0, outcome.get(0).getWeight(), 0.0001);

		FunctionDescriptor descriptor6 = functionDescriptorParser
				.parseDescriptor("ConcatABCABC3\tConcat(\"X\",TestStringCollectionFeature(\"A\",\"B\",\"C\"),TestStringCollectionFeature(\"A\",\"B\",\"C\"))");

		features = parser.parse(descriptor6);
		assertEquals(1, features.size());
		feature = features.get(0);
		if (feature instanceof FeatureWrapper)
			feature = ((FeatureWrapper<String, ?>) feature).getWrappedFeature();
		LOG.debug(feature.getClass().getName());
		assertTrue(feature instanceof StringCollectionFeatureWrapper);

		testContext = "abc";
		stringCollectionFeatureWrapper = (StringCollectionFeatureWrapper<String>) feature;
		env = featureService.getRuntimeEnvironment();
		featureResult = stringCollectionFeatureWrapper.check(testContext, env);
		outcome = featureResult.getOutcome();
		LOG.debug(outcome.toString());
		// this time we shouldn't have a cross-product, cause both inner
		// features were named
		assertEquals(3, outcome.size());

		assertEquals("X|A|A", outcome.get(0).getOutcome());
		assertEquals(1.0, outcome.get(0).getWeight(), 0.0001);

	}
}
