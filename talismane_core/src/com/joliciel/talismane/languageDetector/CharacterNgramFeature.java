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
package com.joliciel.talismane.languageDetector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringCollectionFeature;
import com.joliciel.talismane.utils.WeightedOutcome;

public class CharacterNgramFeature extends AbstractLanguageDetectorFeature<List<WeightedOutcome<String>>> implements StringCollectionFeature<String> {
	int n;
	
	
	public CharacterNgramFeature(int n) {
		super();
		this.n = n;
		
		String name = super.getName() + "(" + n + ")";
		this.setName(name);
	}

	@Override
	public FeatureResult<List<WeightedOutcome<String>>> check(String context,
			RuntimeEnvironment env) {
		Map<String, Integer> ngrams = new HashMap<String, Integer>();
		for (int i=0; i<=context.length()-n; i++) {
			String ngram = context.substring(i, i+n);
			Integer countObj = ngrams.get(ngram);
			int count = countObj==null ? 0 : countObj.intValue();
			count += 1;
			ngrams.put(ngram, count);
		}
		List<WeightedOutcome<String>> ngramList = new ArrayList<WeightedOutcome<String>>();
		for (String ngram : ngrams.keySet()) {
			WeightedOutcome<String> weightedNgram = new WeightedOutcome<String>(ngram, ngrams.get(ngram));
			ngramList.add(weightedNgram);
		}
		
		return this.generateResult(ngramList);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return StringCollectionFeature.class;
	}
}
