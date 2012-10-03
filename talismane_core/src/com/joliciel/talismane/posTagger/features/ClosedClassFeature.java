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

import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.posTagger.PosTagOpenClassIndicator;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * Is this token's pos-tag closed class or open class.
 * @author Assaf Urieli
 *
 */
public class ClosedClassFeature extends AbstractPosTaggedTokenFeature<Boolean> implements BooleanFeature<PosTaggedTokenWrapper> {
	public ClosedClassFeature() {
		super();
		this.setName(super.getName());
	}

	@Override
	public FeatureResult<Boolean> checkInternal(PosTaggedTokenWrapper wrapper) {
		PosTaggedToken posTaggedToken = wrapper.getPosTaggedToken();
		if (posTaggedToken==null)
			return null;
		FeatureResult<Boolean> featureResult = null;
		boolean isClosedClass = posTaggedToken.getTag().getOpenClassIndicator().equals(PosTagOpenClassIndicator.CLOSED);
		featureResult = this.generateResult(isClosedClass);
		
		return featureResult;
	}

}
