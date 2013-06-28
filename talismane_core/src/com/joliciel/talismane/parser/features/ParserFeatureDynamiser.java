///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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

import com.joliciel.talismane.machineLearning.features.AbstractDynamiser;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenAddressFunction;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;

final class ParserFeatureDynamiser extends
		AbstractDynamiser<ParseConfigurationWrapper> {

	public ParserFeatureDynamiser(Class<ParseConfigurationWrapper> clazz) {
		super(clazz);
	}

	@Override
	protected Class<?> getOutcomeTypeExtended(
			Feature<ParseConfigurationWrapper, ?> feature) {
		Class<?> outcomeType = null;
		
		if (feature.getFeatureType().equals(ParserAddressFunction.class)) {
			outcomeType = PosTaggedTokenWrapper.class;
		} else if (feature.getFeatureType().equals(PosTaggedTokenAddressFunction.class)) {
			outcomeType = PosTaggedTokenWrapper.class;
		}
		
		return outcomeType;
	}

}
