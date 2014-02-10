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
package com.joliciel.talismane.posTagger.features;

import java.util.HashSet;
import java.util.Set;

import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * Is the pos-tag assigned to this token one of the given list.
 * @author Assaf Urieli
 *
 */
public final class AssignedPosTagInFeature<T> extends AbstractPosTaggedTokenFeature<T,Boolean> implements BooleanFeature<T> {
	StringFeature<PosTaggedTokenWrapper>[] posTagCodeFeatures;
	
	public AssignedPosTagInFeature(PosTaggedTokenAddressFunction<T> addressFunction, StringFeature<PosTaggedTokenWrapper>... posTagCodeFeatures) {
		super(addressFunction);
		this.posTagCodeFeatures = posTagCodeFeatures;
		String name = super.getName() + "(";
		boolean firstFeature = true;
		for (StringFeature<PosTaggedTokenWrapper> posTagCodeFeature : posTagCodeFeatures) {
			if (!firstFeature)
				name += ",";
			name += posTagCodeFeature.getName();
			firstFeature = false;
		}
		name += ")";
		this.setName(name);
		this.setAddressFunction(addressFunction);
	}
	
	public AssignedPosTagInFeature(StringFeature<PosTaggedTokenWrapper>... posTagCodeFeatures) {
		this(new ItsMeAddressFunction<T>(), posTagCodeFeatures);
	}

	@Override
	public FeatureResult<Boolean> checkInternal(T context, RuntimeEnvironment env) {
		PosTaggedTokenWrapper innerWrapper = this.getToken(context, env);
		if (innerWrapper==null)
			return null;
		PosTaggedToken posTaggedToken = innerWrapper.getPosTaggedToken();
		if (posTaggedToken==null)
			return null;
		
		FeatureResult<Boolean> featureResult = null;

		Set<String> posTagCodes = new HashSet<String>();
		for (StringFeature<PosTaggedTokenWrapper> posTagCodeFeature : posTagCodeFeatures) {
			FeatureResult<String> posTagCodeResult = posTagCodeFeature.check(innerWrapper, env);
			if (posTagCodeResult!=null)
				posTagCodes.add(posTagCodeResult.getOutcome());
		}
		
		boolean result = posTagCodes.contains(posTaggedToken.getTag().getCode());
		featureResult = this.generateResult(result);
		
		return featureResult;
	}
}
