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
package com.joliciel.talismane.machineLearning.features;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.joliciel.talismane.machineLearning.features.DoubleLiteralFeature;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.FeatureServiceLocator;
import com.joliciel.talismane.machineLearning.features.FeatureWrapper;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptorParser;
import com.joliciel.talismane.machineLearning.features.IntegerLiteralFeature;
import com.joliciel.talismane.machineLearning.features.IntegerToDoubleFeature;
import com.joliciel.talismane.machineLearning.features.MinusIntegerOperator;
import com.joliciel.talismane.machineLearning.features.MinusOperator;
import com.joliciel.talismane.machineLearning.features.TestContext;
import com.joliciel.talismane.machineLearning.features.TestParser;

public class TestParserTest {
	private static final Log LOG = LogFactory.getLog(TestParserTest.class);
	
	@SuppressWarnings("unchecked")
	@Test
	public void testMultiplePossibleConstructors() {
		FeatureServiceLocator featureServiceLocator = FeatureServiceLocator.getInstance();
		FeatureService featureService = featureServiceLocator.getFeatureService();
		FunctionDescriptorParser functionDescriptorParser = featureService.getFunctionDescriptorParser();
		
		TestParser parser = new TestParser(featureService);
		
		FunctionDescriptor descriptor = functionDescriptorParser.parseDescriptor("23-12");
		List<Feature<TestContext, ?>> features = parser.parse(descriptor);
		assertEquals(1, features.size());
		Feature<TestContext,?> feature = features.get(0);
		if (feature instanceof FeatureWrapper)
			feature = ((FeatureWrapper<TestContext,?>) feature).getWrappedFeature();
		LOG.debug(feature.getClass());
		assertTrue(feature instanceof MinusIntegerOperator);
		
		MinusIntegerOperator<TestContext> minusIntegerOperator = (MinusIntegerOperator<TestContext>) feature;
		
		IntegerLiteralFeature<TestContext> operand1 = (IntegerLiteralFeature<TestContext>) minusIntegerOperator.getOperand1();
		IntegerLiteralFeature<TestContext> operand2 = (IntegerLiteralFeature<TestContext>) minusIntegerOperator.getOperand2();
		
		assertEquals(23, operand1.getLiteral());
		assertEquals(12, operand2.getLiteral());
		
		descriptor = functionDescriptorParser.parseDescriptor("23.2-12");
		features = parser.parse(descriptor);
		assertEquals(1, features.size());
		feature = features.get(0);
		if (feature instanceof FeatureWrapper)
			feature = ((FeatureWrapper<TestContext,?>) feature).getWrappedFeature();
		LOG.debug(feature.getClass());
		assertTrue(feature instanceof MinusOperator);
		
		MinusOperator<TestContext> minusOperator = (MinusOperator<TestContext>) feature;
		
		DoubleLiteralFeature<TestContext> doubleOperand1 = (DoubleLiteralFeature<TestContext>) minusOperator.getOperand1();
		IntegerToDoubleFeature<TestContext> doubleOperand2 = (IntegerToDoubleFeature<TestContext>) minusOperator.getOperand2();
		IntegerLiteralFeature<TestContext> intOperand2 = (IntegerLiteralFeature<TestContext>) doubleOperand2.getIntegerFeature();
		
		assertEquals(23.2, doubleOperand1.getLiteral(), 0.001);
		assertEquals(12, intOperand2.getLiteral());
		
	}
}
