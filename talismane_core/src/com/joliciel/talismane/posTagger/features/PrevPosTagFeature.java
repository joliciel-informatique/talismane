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
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.posTagger.PosTaggedToken;


/**
 * Retrieves the pos-tag assigned at position n with respect to the current token,
 * where n is a negative integer.
 * @author Assaf Urieli
 *
 */
public class PrevPosTagFeature extends AbstractPosTaggerFeature<String> implements StringFeature<PosTaggerContext> {
	private IntegerFeature<PosTaggerContext> nFeature;
	
	public PrevPosTagFeature(IntegerFeature<PosTaggerContext> nFeature) {
		this.nFeature = nFeature;
		this.setName(super.getName() + "(" + nFeature.getName() + ")");
	}
	
	@Override
	public FeatureResult<String> checkInternal(PosTaggerContext context) {
		FeatureResult<String> result = null;
		
		FeatureResult<Integer> nResult = nFeature.check(context);
		if (nResult!=null) {
			int n = nResult.getOutcome();
			if (n>=0) {
				throw new TalismaneException("Cannot call PrevPosTag with an offset >= 0");
			}
			n = 0-n;
			int i = context.getToken().getIndex();
			if (i >= n) {
				PosTaggedToken prevToken = context.getHistory().get(i-n);
				String posTag = prevToken.getTag().getCode();
				result = this.generateResult(posTag);		
			}
		} // have n
		return result;
	}
}
