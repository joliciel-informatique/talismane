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
package com.joliciel.talismane.posTagger.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptorParser;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.features.TokenFeatureService;

public class PosTaggerFeatureServiceImpl implements PosTaggerFeatureService {
    private static final Log LOG = LogFactory.getLog(PosTaggerFeatureServiceImpl.class);
	private FeatureService featureService;
	private TokenFeatureService tokenFeatureService;
	
	public PosTaggerContext getContext(Token token, PosTagSequence history) {
		PosTaggerContextImpl context = new PosTaggerContextImpl(token, history);
		return context;
	}

	public Set<PosTaggerFeature<?>> getFeatureSet(List<String> featureDescriptors) {
		Set<PosTaggerFeature<?>> features = new TreeSet<PosTaggerFeature<?>>();
		FunctionDescriptorParser descriptorParser = this.getFeatureService().getFunctionDescriptorParser();
		PosTagFeatureParser posTagFeatureParser = this.getPosTagFeatureParser();
		
		for (String featureDescriptor : featureDescriptors) {
			LOG.debug(featureDescriptor);
			if (featureDescriptor.length()>0 && !featureDescriptor.startsWith("#")) {
				FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
				List<PosTaggerFeature<?>> myFeatures = posTagFeatureParser.parseDescriptor(functionDescriptor);
				features.addAll(myFeatures);
				
			}
		}
		return features;
	}
	

	@Override
	public List<PosTaggerRule> getRules(List<String> ruleDescriptors) {
		List<PosTaggerRule> rules = new ArrayList<PosTaggerRule>();
		
		FunctionDescriptorParser descriptorParser = this.getFeatureService().getFunctionDescriptorParser();
		PosTagFeatureParser posTagFeatureParser = this.getPosTagFeatureParser();

		for (String ruleDescriptor : ruleDescriptors) {
			LOG.debug(ruleDescriptor);
			if (ruleDescriptor.length()>0 && !ruleDescriptor.startsWith("#")) {
				String[] ruleParts = ruleDescriptor.split("\t");
				String posTagCode = ruleParts[0];
				PosTag posTag = null;
				boolean negative = false;
				String descriptor = null;
				String descriptorName = null;
				if (ruleParts.length>2) {
					descriptor = ruleParts[2];
					descriptorName = ruleParts[1];
				} else {
					descriptor = ruleParts[1];
				}
				
				if (posTagCode.length()==0) {
					if (descriptorName==null) {
						throw new TalismaneException("Rule without PosTag must have a name.");
					}
				} else {
					if (posTagCode.startsWith("!")) {
						negative = true;
						posTagCode = posTagCode.substring(1);
					}
					posTag = TalismaneSession.getPosTagSet().getPosTag(posTagCode);
				}

				FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(descriptor);
				if (descriptorName!=null)
					functionDescriptor.setDescriptorName(descriptorName);
				List<PosTaggerFeature<?>> myFeatures = posTagFeatureParser.parseDescriptor(functionDescriptor);
				if (posTag!=null) {
					for (PosTaggerFeature<?> feature : myFeatures) {
						if (feature instanceof BooleanFeature) {
							@SuppressWarnings("unchecked")
							BooleanFeature<PosTaggerContext> condition = (BooleanFeature<PosTaggerContext>) feature;
							PosTaggerRule rule = this.getPosTaggerRule(condition, posTag);
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


	private PosTagFeatureParser getPosTagFeatureParser() {
		PosTagFeatureParser posTagFeatureParser = new PosTagFeatureParser(featureService);
		posTagFeatureParser.setTokenFeatureParser(this.tokenFeatureService.getTokenFeatureParser());
		return posTagFeatureParser;
	}
	
	public PosTaggerRule getPosTaggerRule(BooleanFeature<PosTaggerContext> condition, PosTag posTag) {
		PosTaggerRuleImpl rule = new PosTaggerRuleImpl(condition, posTag);
		return rule;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}

	public TokenFeatureService getTokenFeatureService() {
		return tokenFeatureService;
	}

	public void setTokenFeatureService(TokenFeatureService tokenFeatureService) {
		this.tokenFeatureService = tokenFeatureService;
	}

}
