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

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.features.AbstractFeature;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureWrapper;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;

public final class PosTaggedTokenAddressFunctionWrapper<T> extends AbstractFeature<T, PosTaggedTokenWrapper>
		implements PosTaggedTokenAddressFunction<T>, FeatureWrapper<T, PosTaggedTokenWrapper> {

	private Feature<T, PosTaggedTokenWrapper> feature;

	public PosTaggedTokenAddressFunctionWrapper(Feature<T, PosTaggedTokenWrapper> feature) {
		this.feature = feature;
		this.setName(this.feature.getName());
		this.addArgument(feature);
	}

	@Override
	public FeatureResult<PosTaggedTokenWrapper> check(T context, RuntimeEnvironment env) throws TalismaneException {
		return this.feature.check(context, env);
	}

	@Override
	public Feature<T, PosTaggedTokenWrapper> getWrappedFeature() {
		return feature;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return PosTaggedTokenAddressFunction.class;
	}

}
