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
package com.joliciel.talismane.parser.features;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.joliciel.talismane.utils.features.FeatureService;
import com.joliciel.talismane.utils.features.FunctionDescriptor;
import com.joliciel.talismane.utils.features.FunctionDescriptorParser;

public class ParserFeatureServiceImpl implements ParserFeatureServiceInternal {
	private FeatureService featureService;
	
	@Override
	public Set<ParseConfigurationFeature<?>> getFeatures(
			List<String>  featureDescriptors) {
		Set<ParseConfigurationFeature<?>> parseFeatures = new TreeSet<ParseConfigurationFeature<?>>();
		FunctionDescriptorParser descriptorParser = this.getFeatureService().getFunctionDescriptorParser();
		ParserFeatureParser featureParser = this.getParserFeatureParser();
		for (String featureDescriptor : featureDescriptors) {
			if (featureDescriptor.length()>0 && !featureDescriptor.startsWith("#")) {
				FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
				List<ParseConfigurationFeature<?>> myFeatures = featureParser.parseDescriptor(functionDescriptor);
				parseFeatures.addAll(myFeatures);
			}
		}
		return parseFeatures;
	}

	public ParserFeatureParser getParserFeatureParser() {
		ParserFeatureParser parserFeatureParser = new ParserFeatureParser(featureService);
		parserFeatureParser.setParserFeatureServiceInternal(this);
		
		return parserFeatureParser;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}

}
