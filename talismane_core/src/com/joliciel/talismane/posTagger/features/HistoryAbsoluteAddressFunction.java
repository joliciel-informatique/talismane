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

import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * Returns a pos-tagged token from the current history by its absolute index in the sentence.
 * @author Assaf Urieli
 */
public class HistoryAbsoluteAddressFunction extends AbstractPosTaggerFeature<PosTaggedTokenWrapper> implements PosTaggedTokenAddressFunction<PosTaggerContext> {
	private IntegerFeature<PosTaggerContext> indexFeature = null;
	
	public HistoryAbsoluteAddressFunction(IntegerFeature<PosTaggerContext> index) {
		this.indexFeature = index;
		this.setName("HistoryAbs(" + indexFeature.getName() + ")");
	}
	
	@Override
	protected FeatureResult<PosTaggedTokenWrapper> checkInternal(PosTaggerContext context, RuntimeEnvironment env) {
		FeatureResult<PosTaggedTokenWrapper> result = null;
		
		FeatureResult<Integer> indexResult = indexFeature.check(context, env);
		if (indexResult!=null) {
			int n = indexResult.getOutcome();
			if (n<0) {
				return null;
			} else if (n>=context.getHistory().size()) {
				return null;
			}

			PosTaggedToken prevToken = context.getHistory().get(n);
			result = this.generateResult(prevToken);		

		} // have n
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return PosTaggedTokenAddressFunction.class;
	}

	
}
