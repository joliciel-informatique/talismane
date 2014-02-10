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
package com.joliciel.talismane.sentenceDetector.features;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptorParser;

public class SentenceDetectorFeatureServiceImpl implements
		SentenceDetectorFeatureService {
	
	private FeatureService featureService;
	private MachineLearningService machineLearningService;
	private ExternalResourceFinder externalResourceFinder;

	@Override
	public Set<SentenceDetectorFeature<?>> getFeatureSet(
			List<String> featureDescriptors) {
		Set<SentenceDetectorFeature<?>> features = new TreeSet<SentenceDetectorFeature<?>>();
		
		FunctionDescriptorParser descriptorParser = this.getFeatureService().getFunctionDescriptorParser();
		SentenceDetectorFeatureParser sentenceDetectorFeatureParser = this.getSentenceDetectorFeatureParser();
		sentenceDetectorFeatureParser.setExternalResourceFinder(externalResourceFinder);
		
		for (String featureDescriptor : featureDescriptors) {
			if (featureDescriptor.length()>0 && !featureDescriptor.startsWith("#")) {
				FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
				List<SentenceDetectorFeature<?>> myFeatures = sentenceDetectorFeatureParser.parseDescriptor(functionDescriptor);
				features.addAll(myFeatures);
			}
		}
		return features;
	}

	SentenceDetectorFeatureParser getSentenceDetectorFeatureParser() {
		SentenceDetectorFeatureParser parser = new SentenceDetectorFeatureParser(this.getFeatureService());
		return parser;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}


	public ExternalResourceFinder getExternalResourceFinder() {
		if (this.externalResourceFinder==null) {
			this.externalResourceFinder = this.machineLearningService.getExternalResourceFinder();
		}
		return externalResourceFinder;
	}

	public void setExternalResourceFinder(
			ExternalResourceFinder externalResourceFinder) {
		this.externalResourceFinder = externalResourceFinder;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

}
