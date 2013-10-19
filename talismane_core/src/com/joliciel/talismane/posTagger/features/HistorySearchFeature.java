///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * Returns the first token in a given range which matches a certain criterion, or null if no such token is found.<br/>
 * If a start index is provided as a second argument, will start looking at this index, otherwise at the current token's index-1.<br/>
 * If an end index is provided as a third argument, will continue until the end index, otherwise until token 0.<br/>
 * All indexes must be &lt;= the current history size, otherwise only valid indexes will be looked at.<br/>
 * If start index &gt;= end index, will look backwards, otherwise will look forwards.
 * @author Assaf Urieli
 *
 */
public final class HistorySearchFeature extends AbstractPosTaggerFeature<PosTaggedTokenWrapper> implements PosTaggedTokenAddressFunction<PosTaggerContext>  {
	private BooleanFeature<PosTaggedTokenWrapper> criterion;
	private BooleanFeature<PosTaggedTokenWrapper> stopCriterion;
	private IntegerFeature<PosTaggerContext> startIndexFeature = null;
	private IntegerFeature<PosTaggerContext> endIndexFeature = null;
	
	public HistorySearchFeature(BooleanFeature<PosTaggedTokenWrapper> criterion) {
		this.criterion = criterion;
		this.setName(super.getName() + "(" + criterion.getName() + ")");
	}
	
	public HistorySearchFeature(BooleanFeature<PosTaggedTokenWrapper> criterion, IntegerFeature<PosTaggerContext> startIndexFeature) {
		this.criterion = criterion;
		this.startIndexFeature = startIndexFeature;
		this.setName(super.getName() + "(" + criterion.getName() + "," + startIndexFeature.getName() + ")");
	}

	public HistorySearchFeature(BooleanFeature<PosTaggedTokenWrapper> criterion, IntegerFeature<PosTaggerContext> startIndexFeature, IntegerFeature<PosTaggerContext> endIndexFeature) {
		this.criterion = criterion;
		this.startIndexFeature = startIndexFeature;
		this.endIndexFeature = endIndexFeature;
		this.setName(super.getName() + "(" + criterion.getName() + "," + startIndexFeature.getName() + "," + endIndexFeature.getName() + ")");
	}

	public HistorySearchFeature(BooleanFeature<PosTaggedTokenWrapper> criterion, BooleanFeature<PosTaggedTokenWrapper> stopCriterion, IntegerFeature<PosTaggerContext> startIndexFeature, IntegerFeature<PosTaggerContext> endIndexFeature) {
		this.criterion = criterion;
		this.stopCriterion = stopCriterion;
		this.startIndexFeature = startIndexFeature;
		this.endIndexFeature = endIndexFeature;
		this.setName(super.getName() + "(" + criterion.getName() + "," + stopCriterion.getName() + "," + startIndexFeature.getName() + "," + endIndexFeature.getName() + ")");
	}
	
	@Override
	public FeatureResult<PosTaggedTokenWrapper> checkInternal(PosTaggerContext context, RuntimeEnvironment env) {
		FeatureResult<PosTaggedTokenWrapper> featureResult = null;
		
		int startIndex = context.getToken().getIndex()-1;
		int endIndex = 0;
		
		if (startIndexFeature!=null) {
			FeatureResult<Integer> startIndexResult = startIndexFeature.check(context, env);
			if (startIndexResult!=null) {
				startIndex = startIndexResult.getOutcome();
			} else {
				return featureResult;
			}
		}
		
		if (endIndexFeature!=null) {
			FeatureResult<Integer> endIndexResult = endIndexFeature.check(context, env);
			if (endIndexResult!=null) {
				endIndex = endIndexResult.getOutcome();
			} else {
				return featureResult;
			}
		}
		
		if (startIndex<0) 
			startIndex=0;
		
		if (endIndex<0) 
			endIndex=0;
		
		if (startIndex>=context.getHistory().size())
			startIndex = context.getHistory().size()-1;
		if (endIndex>=context.getHistory().size())
			endIndex = context.getHistory().size()-1;
		
		int step = -1;
		if (endIndex>startIndex)
			step = 1;
		
		PosTaggedToken matchingToken = null;
		
		for (int i=startIndex; (step<0 && i>=0 && i>=endIndex) || (step>0 && i<context.getHistory().size() && i<=endIndex); i+=step) {
			PosTaggedToken oneToken = context.getHistory().get(i);

			FeatureResult<Boolean> criterionResult = this.criterion.check(oneToken, env);
			if (criterionResult!=null && criterionResult.getOutcome()) {
				matchingToken = oneToken;
				break;
			}
			if (stopCriterion!=null) {
				FeatureResult<Boolean> stopCriterionResult = this.stopCriterion.check(oneToken, env);
				if (stopCriterionResult!=null && stopCriterionResult.getOutcome()) {
					break;
				}
			}
		}
		if (matchingToken!=null) {
			featureResult = this.generateResult(matchingToken);
		}

		return featureResult;
	}
	

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return PosTaggedTokenAddressFunction.class;
	}
}
