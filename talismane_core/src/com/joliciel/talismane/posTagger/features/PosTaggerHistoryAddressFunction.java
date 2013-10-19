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
 * Returns a pos-tagged token in the current history, at a position relative to the current token.
 * @author Assaf Urieli
 */
public class PosTaggerHistoryAddressFunction extends AbstractPosTaggerFeature<PosTaggedTokenWrapper> implements PosTaggedTokenAddressFunction<PosTaggerContext> {
	private IntegerFeature<PosTaggerContext> offsetFeature = null;
	
	public PosTaggerHistoryAddressFunction(IntegerFeature<PosTaggerContext> offset) {
		this.offsetFeature = offset;
		this.setName("History(" + offsetFeature.getName() + ")");
	}
	
	@Override
	protected FeatureResult<PosTaggedTokenWrapper> checkInternal(PosTaggerContext context, RuntimeEnvironment env) {
		FeatureResult<PosTaggedTokenWrapper> result = null;
		
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
				if (prevToken!=null)
					result = this.generateResult(prevToken);		
			}
		} // have n
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return PosTaggedTokenAddressFunction.class;
	}

	
}
