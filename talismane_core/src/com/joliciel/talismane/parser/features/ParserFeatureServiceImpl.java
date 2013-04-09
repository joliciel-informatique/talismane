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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptorParser;
import com.joliciel.talismane.parser.Transition;

public class ParserFeatureServiceImpl implements ParserFeatureServiceInternal {
	private static final Log LOG = LogFactory.getLog(ParserFeatureServiceImpl.class);
	private FeatureService featureService;
	private MachineLearningService machineLearningService;
	private ExternalResourceFinder externalResourceFinder;
	
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
		parserFeatureParser.setExternalResourceFinder(externalResourceFinder);
		
		return parserFeatureParser;
	}

	public List<ParserRule> getRules(List<String> ruleDescriptors) {
		List<ParserRule> rules = new ArrayList<ParserRule>();
		
		FunctionDescriptorParser descriptorParser = this.getFeatureService().getFunctionDescriptorParser();
		ParserFeatureParser parserFeatureParser = this.getParserFeatureParser();

		for (String ruleDescriptor : ruleDescriptors) {
			LOG.trace(ruleDescriptor);
			if (ruleDescriptor.length()>0 && !ruleDescriptor.startsWith("#")) {
				String[] ruleParts = ruleDescriptor.split("\t");
				String transitionCode = ruleParts[0];
				Transition transition = null;
				boolean negative = false;
				String descriptor = null;
				String descriptorName = null;
				if (ruleParts.length>2) {
					descriptor = ruleParts[2];
					descriptorName = ruleParts[1];
				} else {
					descriptor = ruleParts[1];
				}
				
				if (transitionCode.length()==0) {
					if (descriptorName==null) {
						throw new TalismaneException("Rule without Transition must have a name.");
					}
				} else {
					if (transitionCode.startsWith("!")) {
						negative = true;
						transitionCode = transitionCode.substring(1);
					}
					transition = TalismaneSession.getTransitionSystem().getTransitionForCode(transitionCode);
				}

				FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(descriptor);
				if (descriptorName!=null)
					functionDescriptor.setDescriptorName(descriptorName);
				List<ParseConfigurationFeature<?>> myFeatures = parserFeatureParser.parseDescriptor(functionDescriptor);
				if (transition!=null) {
					for (ParseConfigurationFeature<?> feature : myFeatures) {
						if (feature instanceof BooleanFeature) {
							@SuppressWarnings("unchecked")
							BooleanFeature<ParseConfigurationWrapper> condition = (BooleanFeature<ParseConfigurationWrapper>) feature;
							ParserRule rule = this.getParserRule(condition, transition);
							rule.setNegative(negative);
							rules.add(rule);
						} else {
							throw new TalismaneException("Rule must be based on a boolean feature.");
						}
					} // next feature
				} // is it a rule, or just a descriptor
			} // proper rule descriptor
		} // next rule descriptor
		return rules;
		
	}
	
	public ParserRule getParserRule(
			BooleanFeature<ParseConfigurationWrapper> condition,
			Transition transition) {
		ParserRule parserRule = new ParserRuleImpl(condition, transition);
		return parserRule;
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
