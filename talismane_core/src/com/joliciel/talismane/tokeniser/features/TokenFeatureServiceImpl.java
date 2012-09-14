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
package com.joliciel.talismane.tokeniser.features;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;
import com.joliciel.talismane.utils.features.FeatureService;
import com.joliciel.talismane.utils.features.FunctionDescriptor;
import com.joliciel.talismane.utils.features.FunctionDescriptorParser;
import com.joliciel.talismane.utils.util.PerformanceMonitor;

public class TokenFeatureServiceImpl implements TokenFeatureService {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(TokenFeatureServiceImpl.class);
	private FeatureService featureService;

	TokeniserContextFeatureParser getTokeniserContextFeatureParser(List<TokenPattern> patternList) {
		TokeniserContextFeatureParser parser = new TokeniserContextFeatureParser(this.getFeatureService());
		parser.setPatternList(patternList);
		parser.setTokenFeatureParser(this.getTokenFeatureParser(patternList));
		return parser;
	}
	
	@Override
	public TokenFeatureParser getTokenFeatureParser() {
		return this.getTokenFeatureParser(null);
	}
	
	public TokenFeatureParser getTokenFeatureParser(List<TokenPattern> patternList) {
		TokenFeatureParserImpl tokenFeatureParser = new TokenFeatureParserImpl(this.getFeatureService());
		tokenFeatureParser.setLexiconService(TalismaneSession.getLexiconService());
		tokenFeatureParser.setPosTagSet(TalismaneSession.getPosTagSet());
		tokenFeatureParser.setPatternList(patternList);
		return tokenFeatureParser;
	}
	
	@Override
	public Set<TokeniserContextFeature<?>> getTokeniserContextFeatureSet(List<String> featureDescriptors,
			List<TokenPattern> patternList) {
		Set<TokeniserContextFeature<?>> features = new TreeSet<TokeniserContextFeature<?>>();

		FunctionDescriptorParser descriptorParser = this.getFeatureService().getFunctionDescriptorParser();
		TokeniserContextFeatureParser tokeniserContextFeatureParser = this.getTokeniserContextFeatureParser(patternList);
		tokeniserContextFeatureParser.setPatternList(patternList);
		
		PerformanceMonitor.startTask("TokenFeatureServiceImpl.findFeatureSet");
		try {
			for (String featureDescriptor : featureDescriptors) {
				if (featureDescriptor.length()>0 && !featureDescriptor.startsWith("#")) {
					FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
					List<TokeniserContextFeature<?>> myFeatures = tokeniserContextFeatureParser.parseDescriptor(functionDescriptor);
					PerformanceMonitor.startTask("TokenFeatureServiceImpl.add features");
					features.addAll(myFeatures);
					PerformanceMonitor.endTask("TokenFeatureServiceImpl.add features");
				}
			}
		} finally {
			PerformanceMonitor.endTask("TokenFeatureServiceImpl.findFeatureSet");
		}
		return features;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}



}
