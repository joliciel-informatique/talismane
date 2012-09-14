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
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.utils.features.BooleanFeature;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.features.IntegerFeature;
import com.joliciel.talismane.utils.features.StringFeature;


/**
 * Retrieves the pos-tag assigned at position n with respect to the current token,
 * where n is a negative integer, and checks if it's equal to a certain PosTag code.
 * @author Assaf Urieli
 *
 */
public class PrevPosTagIsFeature extends AbstractPosTaggerFeature<Boolean> implements BooleanFeature<PosTaggerContext> {
	private IntegerFeature<PosTaggerContext> nFeature;
	private StringFeature<PosTaggerContext> posTagCodeFeature;
	
	public PrevPosTagIsFeature(IntegerFeature<PosTaggerContext> nFeature, StringFeature<PosTaggerContext> posTagCodeFeature) {
		this.nFeature = nFeature;
		this.posTagCodeFeature = posTagCodeFeature;
		this.setName(super.getName() + "(" + nFeature.getName() + "," + posTagCodeFeature.getName() + ")");
	}
	
	@Override
	public FeatureResult<Boolean> checkInternal(PosTaggerContext context) {
		FeatureResult<Boolean> result = null;
		
		FeatureResult<Integer> nResult = nFeature.check(context);
		FeatureResult<String> posTagCodeResult = posTagCodeFeature.check(context);
		if (nResult!=null && posTagCodeResult!=null) {
			int n = nResult.getOutcome();
			if (n>=0) {
				throw new TalismaneException("Cannot call PrevPosTagIs with an offset >= 0");
			}
			n = 0-n;
			int i = context.getToken().getIndex();
			if (i >= n) {
				PosTaggedToken prevToken = context.getHistory().get(i-n);
				String posTag = prevToken.getTag().getCode();
				boolean matches = posTag.equals(posTagCodeResult.getOutcome());
				result = this.generateResult(matches);		
			}
		} // have n
		return result;
	}
}
