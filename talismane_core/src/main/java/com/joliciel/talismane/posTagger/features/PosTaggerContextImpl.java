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

import java.util.HashMap;
import java.util.Map;

import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.HasFeatureCache;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.tokeniser.Token;

/**
 * 
 * @author Assaf Urieli
 *
 */
final class PosTaggerContextImpl implements PosTaggerContext, HasFeatureCache {
	private Token token;
	private PosTagSequence history;
	private Map<String,FeatureResult<?>> featureResults = new HashMap<String, FeatureResult<?>>();
	
	public PosTaggerContextImpl(Token token, PosTagSequence history) {
		this.token = token;
		this.history = history;
	}
	
	public Token getToken() {
		return token;
	}

	@Override
	public PosTagSequence getHistory() {
		return this.history;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, Y> FeatureResult<Y> getResultFromCache(Feature<T, Y> feature, RuntimeEnvironment env) {
		FeatureResult<Y> result = null;
		
		String key = feature.getName() + env.getKey();
		if (this.featureResults.containsKey(key)) {
			result = (FeatureResult<Y>) this.featureResults.get(key);
		}
		return result;
	}

	@Override
	public <T, Y> void putResultInCache(Feature<T, Y> feature,
			FeatureResult<Y> featureResult, RuntimeEnvironment env) {
		String key = feature.getName() + env.getKey();
		this.featureResults.put(key, featureResult);	
	}

	@Override
	public String toString() {
		return "PosTaggerContext [token=" + token + ", history=" + history
				+ "]";
	}
	
	
}
