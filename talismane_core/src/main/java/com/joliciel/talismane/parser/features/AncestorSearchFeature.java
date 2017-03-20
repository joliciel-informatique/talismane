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

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenAddressFunction;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;

/**
 * Looks at all of the ancestors of a given reference token in sequence, and
 * returns the first one matching certain criteria. If includeMeFeature is true
 * (false by default) will also look at the reference token itself.
 * 
 * @author Assaf Urieli
 *
 */
public final class AncestorSearchFeature extends AbstractAddressFunction {
	private PosTaggedTokenAddressFunction<ParseConfigurationWrapper> referenceTokenFeature;
	private BooleanFeature<PosTaggedTokenWrapper> criterionFeature;
	private BooleanFeature<ParseConfigurationWrapper> includeMeFeature;

	public AncestorSearchFeature(PosTaggedTokenAddressFunction<ParseConfigurationWrapper> referenceTokenFeature,
			BooleanFeature<PosTaggedTokenWrapper> criterionFeature) {
		super();
		this.referenceTokenFeature = referenceTokenFeature;
		this.criterionFeature = criterionFeature;

		this.setName(super.getName() + "(" + referenceTokenFeature.getName() + "," + criterionFeature.getName() + ")");
	}

	public AncestorSearchFeature(PosTaggedTokenAddressFunction<ParseConfigurationWrapper> referenceTokenFeature,
			BooleanFeature<PosTaggedTokenWrapper> criterionFeature, BooleanFeature<ParseConfigurationWrapper> includeMeFeature) {
		super();
		this.referenceTokenFeature = referenceTokenFeature;
		this.criterionFeature = criterionFeature;
		this.includeMeFeature = includeMeFeature;

		this.setName(super.getName() + "(" + referenceTokenFeature.getName() + "," + criterionFeature.getName() + ")");
	}

	@Override
	public FeatureResult<PosTaggedTokenWrapper> check(ParseConfigurationWrapper wrapper, RuntimeEnvironment env) throws TalismaneException {
		ParseConfiguration configuration = wrapper.getParseConfiguration();
		PosTaggedToken resultToken = null;
		FeatureResult<PosTaggedTokenWrapper> referenceTokenResult = referenceTokenFeature.check(wrapper, env);

		if (referenceTokenResult != null) {
			PosTaggedToken referenceToken = referenceTokenResult.getOutcome().getPosTaggedToken();

			boolean includeMe = false;
			if (includeMeFeature != null) {
				FeatureResult<Boolean> includeMeResult = includeMeFeature.check(wrapper, env);
				if (includeMeResult == null)
					return null;
				includeMe = includeMeResult.getOutcome();
			}

			PosTaggedToken ancestor = null;
			if (includeMe)
				ancestor = referenceToken;
			else
				ancestor = configuration.getHead(referenceToken);

			ParseConfigurationAddress parseConfigurationAddress = new ParseConfigurationAddress(env);
			parseConfigurationAddress.setParseConfiguration(configuration);
			while (ancestor != null) {
				parseConfigurationAddress.setPosTaggedToken(ancestor);
				FeatureResult<Boolean> criterionResult = criterionFeature.check(parseConfigurationAddress, env);
				if (criterionResult != null) {
					boolean criterion = criterionResult.getOutcome();
					if (criterion) {
						resultToken = ancestor;
						break;
					}
				}
				ancestor = configuration.getHead(ancestor);
			}
		}
		FeatureResult<PosTaggedTokenWrapper> featureResult = null;
		if (resultToken != null)
			featureResult = this.generateResult(resultToken);
		return featureResult;
	}
}
