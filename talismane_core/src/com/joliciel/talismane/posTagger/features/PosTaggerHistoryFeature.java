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

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * Applies a posTaggerFeature to a particular pos-tagged token in the current history.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class PosTaggerHistoryFeature<T> extends AbstractPosTaggerFeature<T> {
	private IntegerFeature<PosTaggerContext> offsetFeature = null;
	private Feature<PosTaggedTokenWrapper,T> posTaggerFeature = null;
	
	public PosTaggerHistoryFeature(IntegerFeature<PosTaggerContext> offset, Feature<PosTaggedTokenWrapper,T> posTaggerFeature) {
		this.posTaggerFeature = posTaggerFeature;
		this.offsetFeature = offset;
		this.setName(posTaggerFeature.getName() + "[offset<" + this.offsetFeature.getName() + ">]");
	}
	
	@Override
	protected FeatureResult<T> checkInternal(PosTaggerContext context, RuntimeEnvironment env) {
		FeatureResult<T> result = null;
		
		FeatureResult<Integer> offsetResult = offsetFeature.check(context, env);
		if (offsetResult!=null) {
			int n = offsetResult.getOutcome();
			if (n>=0) {
				throw new TalismaneException("Cannot call PosTaggerHistoryFeature with an offset >= 0");
			}
			n = 0-n;
			int i = context.getToken().getIndex();
			if (i >= n) {
				PosTaggedToken prevToken = context.getHistory().get(i-n);
				FeatureResult<T> wrappedFeatureResult = this.posTaggerFeature.check(prevToken, env);
				if (wrappedFeatureResult!=null)
					result = this.generateResult(wrappedFeatureResult.getOutcome());		
			}
		} // have n
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return this.posTaggerFeature.getFeatureType();
	}

	
}
