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
package com.joliciel.talismane.tokeniser.patterns;

import java.util.Collection;
import java.util.Set;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ExternalResource;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.features.TokenPatternMatchFeature;
import com.joliciel.talismane.tokeniser.features.TokenPatternMatchFeatureParser;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeatureParser;

public class PatternTokeniserFactory {
	public enum PatternTokeniserType {
		Interval, Compound
	}

	public static final String PATTERN_DESCRIPTOR_KEY = "pattern";

	private final TalismaneSession talismaneSession;

	public PatternTokeniserFactory(TalismaneSession talismaneSession) {
		super();
		this.talismaneSession = talismaneSession;
	}

	public Tokeniser getPatternTokeniser(ClassificationModel tokeniserModel, int beamWidth) {
		TokeniserPatternManager patternManager = new TokeniserPatternManager(tokeniserModel.getDescriptors().get(PATTERN_DESCRIPTOR_KEY));

		PatternTokeniserType patternTokeniserType = PatternTokeniserType
				.valueOf((String) tokeniserModel.getModelAttributes().get(PatternTokeniserType.class.getSimpleName()));

		Tokeniser tokeniser = null;
		if (patternTokeniserType == PatternTokeniserType.Interval) {
			TokeniserContextFeatureParser featureParser = new TokeniserContextFeatureParser(talismaneSession, patternManager.getParsedTestPatterns());
			Collection<ExternalResource<?>> externalResources = tokeniserModel.getExternalResources();
			if (externalResources != null) {
				for (ExternalResource<?> externalResource : externalResources) {
					featureParser.getExternalResourceFinder().addExternalResource(externalResource);
				}
			}
			Set<TokeniserContextFeature<?>> tokeniserContextFeatures = featureParser.getTokeniserContextFeatureSet(tokeniserModel.getFeatureDescriptors());

			tokeniser = new IntervalPatternTokeniser(tokeniserModel.getDecisionMaker(), patternManager, tokeniserContextFeatures, beamWidth, talismaneSession);
		} else {
			TokenPatternMatchFeatureParser featureParser = new TokenPatternMatchFeatureParser(talismaneSession);
			Collection<ExternalResource<?>> externalResources = tokeniserModel.getExternalResources();
			if (externalResources != null) {
				for (ExternalResource<?> externalResource : externalResources) {
					featureParser.getExternalResourceFinder().addExternalResource(externalResource);
				}
			}
			Set<TokenPatternMatchFeature<?>> features = featureParser.getTokenPatternMatchFeatureSet(tokeniserModel.getFeatureDescriptors());

			tokeniser = new CompoundPatternTokeniser(tokeniserModel.getDecisionMaker(), patternManager, features, beamWidth, talismaneSession);
		}
		return tokeniser;
	}
}
