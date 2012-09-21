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

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.posTagger.PosTag;


/**
 * Retrieves the tags assigned to the previous N tokens.<br/>
 * Will only return results if the current index >= N-2 (to avoid multiple start tokens).<br/>
 * This ensures that we don't repeat exactly the same information in 4-grams, trigrams, bigrams, etc.
 * @author Assaf Urieli
 *
 */
public class NgramFeature extends AbstractPosTaggerFeature<String> implements StringFeature<PosTaggerContext> {
    static final String START_TOKEN = "[[START]]";
	private IntegerFeature<PosTaggerContext> nFeature;
	
	public NgramFeature(IntegerFeature<PosTaggerContext> nFeature) {
		this.nFeature = nFeature;
		this.setName(super.getName() + "(" + nFeature.getName() + ")");
	}
	
	@Override
	public FeatureResult<String> checkInternal(PosTaggerContext context) {
		FeatureResult<String> result = null;
		
		FeatureResult<Integer> nResult = nFeature.check(context);
		if (nResult!=null) {
			int n = nResult.getOutcome();
			int historyToFind = n-1;
			int historyFound = 0;
			if (context.getToken().getIndex() >= historyToFind-1) {
				String ngram = "";
				int i = 0;
				while (historyFound < historyToFind) {
					String posTagCode = null;
					boolean isEmptyTag = false;
					if (context.getHistory().size()>i) {
						PosTag posTag = context.getHistory().get(context.getHistory().size()-i-1).getTag();
						posTagCode = posTag.getCode();
						if (posTag.isEmpty())
							isEmptyTag = true;
					} else {
						posTagCode = START_TOKEN;
					}
					if (!isEmptyTag) {
						if (historyFound>0)
							ngram = "," + ngram;
						ngram = posTagCode + ngram;
						historyFound++;
					}
					i++;
				}
				result = this.generateResult( ngram);		
			}
		} // have n
		return result;
	}
}
