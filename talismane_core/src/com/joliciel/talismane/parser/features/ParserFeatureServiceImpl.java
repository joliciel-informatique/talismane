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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.Dynamiser;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptorParser;
import com.joliciel.talismane.parser.Transition;
import com.joliciel.talismane.utils.PerformanceMonitor;

public class ParserFeatureServiceImpl implements ParserFeatureServiceInternal {
	private static final Log LOG = LogFactory.getLog(ParserFeatureServiceImpl.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(ParserFeatureServiceImpl.class);
	private FeatureService featureService;
	private MachineLearningService machineLearningService;
	private TalismaneService talismaneService;
	private ExternalResourceFinder externalResourceFinder;
	
	@Override
	public Set<ParseConfigurationFeature<?>> getFeatures(
			List<String>  featureDescriptors) {
		return this.getFeatures(featureDescriptors, false);
	}

	@Override
	public Set<ParseConfigurationFeature<?>> getFeatures(
			List<String> featureDescriptors, boolean dynamise) {
		MONITOR.startTask("getFeatures");
		try {
			Set<ParseConfigurationFeature<?>> parseFeatures = new TreeSet<ParseConfigurationFeature<?>>();
			FunctionDescriptorParser descriptorParser = this.getFeatureService().getFunctionDescriptorParser();
			ParserFeatureParser featureParser = this.getParserFeatureParser();
			
			if (dynamise) {
				Dynamiser<ParseConfigurationWrapper> dynamiser = this.getParserFeatureDynamiser();
				featureParser.setDynamiser(dynamiser);
			}
			
			for (String featureDescriptor : featureDescriptors) {
				if (featureDescriptor.trim().length()>0 && !featureDescriptor.startsWith("#")) {
					FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
					List<ParseConfigurationFeature<?>> myFeatures = featureParser.parseDescriptor(functionDescriptor);
					parseFeatures.addAll(myFeatures);
				}
			}
			return parseFeatures;
		} finally {
			MONITOR.endTask();
		}
	}
	
	public Dynamiser<ParseConfigurationWrapper> getParserFeatureDynamiser() {
		ParserFeatureDynamiser dynamiser = new ParserFeatureDynamiser(ParseConfigurationWrapper.class);
		return dynamiser;
	}


	public ParserFeatureParser getParserFeatureParser() {
		ParserFeatureParser parserFeatureParser = new ParserFeatureParser(featureService);
		parserFeatureParser.setParserFeatureServiceInternal(this);
		parserFeatureParser.setTalismaneService(this.getTalismaneService());
		parserFeatureParser.setExternalResourceFinder(externalResourceFinder);
		
		return parserFeatureParser;
	}

	public List<ParserRule> getRules(List<String> ruleDescriptors) {
		return this.getRules(ruleDescriptors, false);
	}
	

	@Override
	public List<ParserRule> getRules(List<String> ruleDescriptors,
			boolean dynamise) {
		MONITOR.startTask("getRules");
		try {
			List<ParserRule> rules = new ArrayList<ParserRule>();
			
			FunctionDescriptorParser descriptorParser = this.getFeatureService().getFunctionDescriptorParser();
			ParserFeatureParser parserFeatureParser = this.getParserFeatureParser();
			
			if (dynamise) {
				Dynamiser<ParseConfigurationWrapper> dynamiser = this.getParserFeatureDynamiser();
				parserFeatureParser.setDynamiser(dynamiser);
			}
			
			for (String ruleDescriptor : ruleDescriptors) {
				LOG.debug(ruleDescriptor);
				if (ruleDescriptor.trim().length()>0 && !ruleDescriptor.startsWith("#")) {
					String[] ruleParts = ruleDescriptor.split("\t");
					String transitionCode = ruleParts[0];
					Transition transition = null;
					Set<Transition> transitions = null;
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
							String[] transitionCodes = transitionCode.substring(1).split(";");
							transitions = new HashSet<Transition>();
							for (String code : transitionCodes) {
								Transition oneTransition = talismaneService.getTalismaneSession().getTransitionSystem().getTransitionForCode(code);
								transitions.add(oneTransition);
							}
							transition = transitions.iterator().next();
						} else {
							transition = talismaneService.getTalismaneSession().getTransitionSystem().getTransitionForCode(transitionCode);
						}
	
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
								
								if (negative) {
									ParserRule rule = this.getParserRule(condition, transitions);
									rule.setNegative(true);
									rules.add(rule);
								} else {
									ParserRule rule = this.getParserRule(condition, transition);
									rule.setNegative(false);
									rules.add(rule);
								}
							} else {
								throw new TalismaneException("Rule must be based on a boolean feature.");
							}
						} // next feature
					} // is it a rule, or just a descriptor
				} // proper rule descriptor
			} // next rule descriptor
			return rules;
		} finally {
			MONITOR.endTask();
		}
	}
	
	public ParserRule getParserRule(
			BooleanFeature<ParseConfigurationWrapper> condition,
			Transition transition) {
		ParserRule parserRule = new ParserRuleImpl(condition, transition);
		return parserRule;
	}
	

	public ParserRule getParserRule(
			BooleanFeature<ParseConfigurationWrapper> condition,
			Set<Transition> transitions) {
		ParserRule parserRule = new ParserRuleImpl(condition, transitions);
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

	public TalismaneService getTalismaneService() {
		return talismaneService;
	}

	public void setTalismaneService(TalismaneService talismaneService) {
		this.talismaneService = talismaneService;
	}

}
