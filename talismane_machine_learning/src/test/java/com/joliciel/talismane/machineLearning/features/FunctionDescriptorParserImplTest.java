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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;

import com.joliciel.talismane.machineLearning.features.DescriptorSyntaxException;
import com.joliciel.talismane.machineLearning.features.FeatureServiceImpl;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptorParserImpl;

public class FunctionDescriptorParserImplTest {
	private static final Logger LOG = LoggerFactory.getLogger(FunctionDescriptorParserImplTest.class);

	@Test
	public void testParseDescriptor() {
		FeatureServiceImpl featureService = new FeatureServiceImpl();
		FunctionDescriptorParserImpl parser = new FunctionDescriptorParserImpl();
		parser.setFeatureServiceInternal(featureService);
		
		FunctionDescriptor descriptor = parser.parseDescriptor("T");
		assertEquals("T", descriptor.getFunctionName());
		assertEquals(0, descriptor.getArguments().size());
		
		descriptor = parser.parseDescriptor("T(1)");
		assertEquals("T", descriptor.getFunctionName());
		assertEquals(1, descriptor.getArguments().size());
		assertEquals(1, descriptor.getArguments().get(0).getObject());

		descriptor = parser.parseDescriptor("T(-1)");
		assertEquals("T", descriptor.getFunctionName());
		assertEquals(1, descriptor.getArguments().size());
		assertEquals(-1, descriptor.getArguments().get(0).getObject());

		descriptor = parser.parseDescriptor("T(1,2)");
		assertEquals("T", descriptor.getFunctionName());
		assertEquals(2, descriptor.getArguments().size());
		assertEquals(1, descriptor.getArguments().get(0).getObject());
		assertEquals(2, descriptor.getArguments().get(1).getObject());

		descriptor = parser.parseDescriptor("T(1,-2)");
		assertEquals("T", descriptor.getFunctionName());
		assertEquals(2, descriptor.getArguments().size());
		assertEquals(1, descriptor.getArguments().get(0).getObject());
		assertEquals(-2, descriptor.getArguments().get(1).getObject());

		descriptor = parser.parseDescriptor("T(2-1)");
		assertEquals("T", descriptor.getFunctionName());
		assertEquals(1, descriptor.getArguments().size());
		FunctionDescriptor operator = descriptor.getArguments().get(0);
		assertEquals(2, operator.getArguments().size());
		assertEquals("-", operator.getFunctionName());
		assertEquals(2, operator.getArguments().get(0).getObject());
		assertEquals(1, operator.getArguments().get(1).getObject());
		
		descriptor = parser.parseDescriptor("T(A()-3)");
		assertEquals("T", descriptor.getFunctionName());
		assertEquals(1, descriptor.getArguments().size());
		operator = descriptor.getArguments().get(0);
		assertEquals(2, operator.getArguments().size());
		assertEquals("-", operator.getFunctionName());
		assertEquals("A", operator.getArguments().get(0).getFunctionName());
		assertEquals(3, operator.getArguments().get(1).getObject());

		descriptor = parser.parseDescriptor("T(A(B())>C(D()))");
		assertEquals("T", descriptor.getFunctionName());
		assertEquals(1, descriptor.getArguments().size());
		operator = descriptor.getArguments().get(0);
		assertEquals(2, operator.getArguments().size());
		assertEquals(">", operator.getFunctionName());
		assertEquals("A", operator.getArguments().get(0).getFunctionName());
		assertEquals("C", operator.getArguments().get(1).getFunctionName());

		// double-character operators
		descriptor = parser.parseDescriptor("T(A(B())>=C(D()))");
		assertEquals("T", descriptor.getFunctionName());
		assertEquals(1, descriptor.getArguments().size());
		operator = descriptor.getArguments().get(0);
		assertEquals(2, operator.getArguments().size());
		assertEquals(">=", operator.getFunctionName());
		assertEquals("A", operator.getArguments().get(0).getFunctionName());
		assertEquals("C", operator.getArguments().get(1).getFunctionName());

		descriptor = parser.parseDescriptor("T(A[x],B(y))");
		assertEquals("T", descriptor.getFunctionName());
		assertEquals(2, descriptor.getArguments().size());
		FunctionDescriptor innerFunction = descriptor.getArguments().get(0);
		assertEquals("A", innerFunction.getFunctionName());
		assertEquals(1, innerFunction.getArguments().size());
		assertEquals("x", innerFunction.getArguments().get(0).getFunctionName());
		innerFunction = descriptor.getArguments().get(1);
		assertEquals("B", innerFunction.getFunctionName());
		assertEquals(1, innerFunction.getArguments().size());
		assertEquals("y", innerFunction.getArguments().get(0).getFunctionName());

		descriptor = parser.parseDescriptor("T(A(B ( C , D),   E[]), F(y))");
		assertEquals("T", descriptor.getFunctionName());
		assertEquals(2, descriptor.getArguments().size());
		innerFunction = descriptor.getArguments().get(0);
		assertEquals("A", innerFunction.getFunctionName());
		assertEquals(2, innerFunction.getArguments().size());
		assertEquals("B", innerFunction.getArguments().get(0).getFunctionName());
		assertEquals(2, innerFunction.getArguments().get(0).getArguments().size());
		assertEquals("C", innerFunction.getArguments().get(0).getArguments().get(0).getFunctionName());
		assertEquals("D", innerFunction.getArguments().get(0).getArguments().get(1).getFunctionName());
		assertEquals("E", innerFunction.getArguments().get(1).getFunctionName());
		innerFunction = descriptor.getArguments().get(1);
		assertEquals("F", innerFunction.getFunctionName());
		assertEquals(1, innerFunction.getArguments().size());
		assertEquals("y", innerFunction.getArguments().get(0).getFunctionName());
		
		descriptor = parser.parseDescriptor("T(\"1 2\")");
		assertEquals("T", descriptor.getFunctionName());
		assertEquals(1, descriptor.getArguments().size());
		assertEquals("1 2", descriptor.getArguments().get(0).getObject());

		descriptor = parser.parseDescriptor("T(\"\\\"\\\\\")");
		assertEquals("T", descriptor.getFunctionName());
		assertEquals(1, descriptor.getArguments().size());
		assertEquals("\"\\", descriptor.getArguments().get(0).getObject());

		// exceptional conditions
		try {
			descriptor = parser.parseDescriptor("Test Space");
			fail("Expected exception");
		} catch (DescriptorSyntaxException fse) {
			LOG.debug(fse.getMessage());
		}
		
		try {
			descriptor = parser.parseDescriptor("Test(");
			fail("Expected exception");
		} catch (DescriptorSyntaxException fse) {
			LOG.debug(fse.getMessage());
		}

		try {
			descriptor = parser.parseDescriptor("Test(blah]");
			fail("Expected exception");
		} catch (DescriptorSyntaxException fse) {
			LOG.debug(fse.getMessage());
		}
		
		try {
			descriptor = parser.parseDescriptor("Test())");
			fail("Expected exception");
		} catch (DescriptorSyntaxException fse) {
			LOG.debug(fse.getMessage());
		}
		
		try {
			descriptor = parser.parseDescriptor("");
			fail("Expected exception");
		} catch (DescriptorSyntaxException fse) {
			LOG.debug(fse.getMessage());
		}
		
		try {
			descriptor = parser.parseDescriptor("Test, Blah");
			fail("Expected exception");
		} catch (DescriptorSyntaxException fse) {
			LOG.debug(fse.getMessage());
		}
		
		
		try {
			descriptor = parser.parseDescriptor("Blah(>2)");
			fail("Expected exception");
		} catch (DescriptorSyntaxException fse) {
			LOG.debug(fse.getMessage());
		}
		
		
		try {
			descriptor = parser.parseDescriptor("Blah(1,>2)");
			fail("Expected exception");
		} catch (DescriptorSyntaxException fse) {
			LOG.debug(fse.getMessage());
		}
	}

	@Test
	public void testGroupingParentheses() {
		FeatureServiceImpl featureService = new FeatureServiceImpl();
		FunctionDescriptorParserImpl parser = new FunctionDescriptorParserImpl();
		parser.setFeatureServiceInternal(featureService);
		
		FunctionDescriptor descriptor = parser.parseDescriptor("Test((1))");
		assertEquals("Test", descriptor.getFunctionName());
		assertEquals(1, descriptor.getArguments().size());
		assertEquals(1, descriptor.getArguments().get(0).getObject());

		// grouping parentheses
		descriptor = parser.parseDescriptor("((20-10)-5)");
		assertEquals("-", descriptor.getFunctionName());
		assertEquals(2, descriptor.getArguments().size());
		assertEquals("-", descriptor.getArguments().get(0).getFunctionName());
		assertEquals(5, descriptor.getArguments().get(1).getObject());
		descriptor = descriptor.getArguments().get(0);
		assertEquals(2, descriptor.getArguments().size());
		assertEquals(20, descriptor.getArguments().get(0).getObject());
		assertEquals(10, descriptor.getArguments().get(1).getObject());

		// grouping parentheses
		descriptor = parser.parseDescriptor("(20-(10-5))");
		assertEquals("-", descriptor.getFunctionName());
		assertEquals(2, descriptor.getArguments().size());
		assertEquals(20, descriptor.getArguments().get(0).getObject());
		assertEquals("-", descriptor.getArguments().get(1).getFunctionName());
		descriptor = descriptor.getArguments().get(1);
		assertEquals(2, descriptor.getArguments().size());
		assertEquals(10, descriptor.getArguments().get(0).getObject());
		assertEquals(5, descriptor.getArguments().get(1).getObject());
		
		// no grouping parentheses (always interpreted to have left-precedence)
		descriptor = parser.parseDescriptor("(6-5-3)");
		assertEquals("-", descriptor.getFunctionName());
		assertEquals(2, descriptor.getArguments().size());
		assertEquals("-", descriptor.getArguments().get(0).getFunctionName());
		assertEquals(3, descriptor.getArguments().get(1).getObject());
		descriptor = descriptor.getArguments().get(0);
		assertEquals(2, descriptor.getArguments().size());
		assertEquals(6, descriptor.getArguments().get(0).getObject());
		assertEquals(5, descriptor.getArguments().get(1).getObject());

		try {
			descriptor = parser.parseDescriptor("((1,2))");
			fail("Expected exception");
		} catch (DescriptorSyntaxException fse) {
			LOG.debug(fse.getMessage());
		}
	}
	
	@Test
	public void testOperatorPrecedence() {
		FeatureServiceImpl featureService = new FeatureServiceImpl();
		FunctionDescriptorParserImpl parser = new FunctionDescriptorParserImpl();
		parser.setFeatureServiceInternal(featureService);
		
		FunctionDescriptor descriptor = parser.parseDescriptor("9-3*2");
		assertEquals("-", descriptor.getFunctionName());
		assertEquals(2, descriptor.getArguments().size());
		assertEquals(9, descriptor.getArguments().get(0).getObject());
		assertEquals("*", descriptor.getArguments().get(1).getFunctionName());
		descriptor = descriptor.getArguments().get(1);
		assertEquals(2, descriptor.getArguments().size());
		assertEquals(3, descriptor.getArguments().get(0).getObject());
		assertEquals(2, descriptor.getArguments().get(1).getObject());
		
		descriptor = parser.parseDescriptor("9-4+3");
		assertEquals("+", descriptor.getFunctionName());
		assertEquals(2, descriptor.getArguments().size());
		assertEquals("-", descriptor.getArguments().get(0).getFunctionName());
		assertEquals(3, descriptor.getArguments().get(1).getObject());
		descriptor = descriptor.getArguments().get(0);
		assertEquals(2, descriptor.getArguments().size());
		assertEquals(9, descriptor.getArguments().get(0).getObject());
		assertEquals(4, descriptor.getArguments().get(1).getObject());

		descriptor = parser.parseDescriptor("9*4+3");
		assertEquals("+", descriptor.getFunctionName());
		assertEquals(2, descriptor.getArguments().size());
		assertEquals("*", descriptor.getArguments().get(0).getFunctionName());
		assertEquals(3, descriptor.getArguments().get(1).getObject());
		descriptor = descriptor.getArguments().get(0);
		assertEquals(2, descriptor.getArguments().size());
		assertEquals(9, descriptor.getArguments().get(0).getObject());
		assertEquals(4, descriptor.getArguments().get(1).getObject());

		descriptor = parser.parseDescriptor("3<4|5<6");
		assertEquals("|", descriptor.getFunctionName());
		assertEquals(2, descriptor.getArguments().size());
		assertEquals("<", descriptor.getArguments().get(0).getFunctionName());
		assertEquals("<", descriptor.getArguments().get(1).getFunctionName());
		FunctionDescriptor leftDescriptor = descriptor.getArguments().get(0);
		assertEquals(2, leftDescriptor.getArguments().size());
		assertEquals(3, leftDescriptor.getArguments().get(0).getObject());
		assertEquals(4, leftDescriptor.getArguments().get(1).getObject());
		FunctionDescriptor rightDescriptor = descriptor.getArguments().get(1);
		assertEquals(2, rightDescriptor.getArguments().size());
		assertEquals(5, rightDescriptor.getArguments().get(0).getObject());
		assertEquals(6, rightDescriptor.getArguments().get(1).getObject());
		
		descriptor = parser.parseDescriptor("A()-B()*C()");
		assertEquals("-", descriptor.getFunctionName());
		assertEquals(2, descriptor.getArguments().size());
		assertEquals("A", descriptor.getArguments().get(0).getFunctionName());
		assertEquals("*", descriptor.getArguments().get(1).getFunctionName());
		descriptor = descriptor.getArguments().get(1);
		assertEquals(2, descriptor.getArguments().size());
		assertEquals("B", descriptor.getArguments().get(0).getFunctionName());
		assertEquals("C", descriptor.getArguments().get(1).getFunctionName());
	}
}
