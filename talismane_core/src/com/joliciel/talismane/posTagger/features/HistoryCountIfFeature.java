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


import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * Counts the pos-tagged tokens within a certain range matching a certain criterion.<br/>
 * The range is given in absolute indexes.<br/>
 * If start &gt; end, returns null.<br/>
 * If no end is provided, assumes it should go till end of the current history.<br/>
 * @author Assaf Urieli
 *
 */
public final class HistoryCountIfFeature extends AbstractPosTaggerFeature<Integer> implements IntegerFeature<PosTaggerContext> {
	private BooleanFeature<PosTaggedTokenWrapper> criterion;
	private IntegerFeature<PosTaggerContext> startIndexFeature = null;
	private IntegerFeature<PosTaggerContext> endIndexFeature = null;
	
	public HistoryCountIfFeature(BooleanFeature<PosTaggedTokenWrapper> criterion, IntegerFeature<PosTaggerContext> startIndexFeature) {
		this.criterion = criterion;
		this.startIndexFeature = startIndexFeature;
		this.setName(super.getName() + "(" + criterion.getName() + "," + startIndexFeature.getName() + ")");
	}
	
	public HistoryCountIfFeature(BooleanFeature<PosTaggedTokenWrapper> criterion, IntegerFeature<PosTaggerContext> startIndexFeature, IntegerFeature<PosTaggerContext> endIndexFeature) {
		this.criterion = criterion;
		this.startIndexFeature = startIndexFeature;
		this.endIndexFeature = endIndexFeature;
		this.setName(super.getName() + "(" + criterion.getName() + "," + startIndexFeature.getName() + "," + endIndexFeature.getName() + ")");
	}
		
	@Override
	public FeatureResult<Integer> checkInternal(PosTaggerContext context, RuntimeEnvironment env) {

		FeatureResult<Integer> featureResult = null;
		
		int startIndex = 0;
		int endIndex = context.getHistory().size()-1;

		FeatureResult<Integer> startIndexResult = startIndexFeature.check(context, env);
		if (startIndexResult!=null) {
			startIndex = startIndexResult.getOutcome();
		} else {
			return null;
		}
		
		if (endIndexFeature!=null) {
			FeatureResult<Integer> endIndexResult = endIndexFeature.check(context, env);
			if (endIndexResult!=null) {
				endIndex = endIndexResult.getOutcome();
			} else {
				return null;
			}
		}
		
		if (endIndex<startIndex)
			return null;
		
		if (startIndex<=0)
			startIndex = 0;
		
		int count = 0;
		for (int i=startIndex; i<context.getHistory().size() && i<=endIndex; i++) {
			PosTaggedToken oneToken = context.getHistory().get(i);
			FeatureResult<Boolean> criterionResult = this.criterion.check(oneToken, env);
			if (criterionResult!=null && criterionResult.getOutcome()) {
				count++;
			}
		}

		featureResult = this.generateResult(count);

		return featureResult;
	}
}
