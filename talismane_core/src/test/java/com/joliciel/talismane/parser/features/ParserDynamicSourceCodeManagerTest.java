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
package com.joliciel.talismane.parser.features;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.machineLearning.features.DynamicSourceCodeBuilder;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptorParser;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.utils.compiler.DynamicCompiler;

public class ParserDynamicSourceCodeManagerTest {

	@Test
	public void testBasics() {
		ParserFeatureDynamiser manager = new ParserFeatureDynamiser(ParseConfigurationWrapper.class);
		DynamicCompiler compiler = manager.getCompiler();

		String src = "package foo.bar;\n" + "import com.joliciel.talismane.parser.features.ParseConfigurationWrapper;\n" + "public class Foo {\n"
				+ "  public void test(ParseConfigurationWrapper arg) {\n" + "    arg.getParseConfiguration();\n" + "  }\n" + "}";

		compiler.compile("foo.bar.Foo", src, null);
	}

	@Test
	public void testGetBuilder() {
		TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance("");
		ParserFeatureServiceLocator parserFeatureServiceLocator = talismaneServiceLocator.getParserFeatureServiceLocator();
		ParserFeatureServiceImpl parserFeatureService = (ParserFeatureServiceImpl) parserFeatureServiceLocator.getParserFeatureService();
		String descriptorString = "PosTag(Stack[0])";
		FunctionDescriptorParser descriptorParser = new FunctionDescriptorParser();
		ParserFeatureParser featureParser = parserFeatureService.getParserFeatureParser();
		FunctionDescriptor descriptor = descriptorParser.parseDescriptor(descriptorString);
		Feature<ParseConfigurationWrapper, ?> feature = featureParser.parse(descriptor).get(0);

		ParserFeatureDynamiser manager = new ParserFeatureDynamiser(ParseConfigurationWrapper.class);
		DynamicSourceCodeBuilder<ParseConfigurationWrapper> builder = manager.getBuilder(feature);

		Feature<ParseConfigurationWrapper, ?> newFeature = builder.getFeature();
		assertTrue(StringFeature.class.isAssignableFrom(newFeature.getClass()));

		descriptorString = "Concat(PosTag(Stack[0]),PosTag(Buffer[0]))";
		descriptor = descriptorParser.parseDescriptor(descriptorString);
		feature = featureParser.parse(descriptor).get(0);

		builder = manager.getBuilder(feature);

		newFeature = builder.getFeature();
		assertTrue(StringFeature.class.isAssignableFrom(newFeature.getClass()));
	}
}
