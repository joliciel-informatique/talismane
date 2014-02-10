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
package com.joliciel.talismane.parser.features;


import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenAddressFunction;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;

/**
 * Returns the first token in a given range which matches a certain criterion, or null if no such token is found.<br/>
 * If a start index is provided as a second argument, will start looking at this index.<br/>
 * If an end index is provided as a third argument, will continue until the end index.<br/>
 * All indexes must be &lt;= the current history size, otherwise only valid indexes will be looked at.<br/>
 * If start index &gt;= end index, will look backwards, otherwise will look forwards.<br/>
 * A stopCriterion can be provided, in which case the search will stop as soon as the stop criterion is hit. In this case, the find criterion is always checked before the stop criterion.<br/>
 * The user can indicate whether to find the first or the last occurrence (default is first).<br/>
 * @author Assaf Urieli
 *
 */
public final class TokenSearchFeature extends AbstractAddressFunction implements PosTaggedTokenAddressFunction<ParseConfigurationWrapper>  {
	private BooleanFeature<PosTaggedTokenWrapper> criterion;
	private BooleanFeature<PosTaggedTokenWrapper> stopCriterion;
	private BooleanFeature<ParseConfigurationWrapper> findFirstFeature;
	private IntegerFeature<ParseConfigurationWrapper> startIndexFeature = null;
	private IntegerFeature<ParseConfigurationWrapper> endIndexFeature = null;

	
	public TokenSearchFeature(BooleanFeature<PosTaggedTokenWrapper> criterion, IntegerFeature<ParseConfigurationWrapper> startIndexFeature, IntegerFeature<ParseConfigurationWrapper> endIndexFeature) {
		this.criterion = criterion;
		this.startIndexFeature = startIndexFeature;
		this.endIndexFeature = endIndexFeature;
		this.setName(super.getName() + "(" + criterion.getName() + "," + startIndexFeature.getName() + "," + endIndexFeature.getName() + ")");
	}

	public TokenSearchFeature(BooleanFeature<PosTaggedTokenWrapper> criterion, BooleanFeature<PosTaggedTokenWrapper> stopCriterion, IntegerFeature<ParseConfigurationWrapper> startIndexFeature, IntegerFeature<ParseConfigurationWrapper> endIndexFeature) {
		this.criterion = criterion;
		this.stopCriterion = stopCriterion;
		this.startIndexFeature = startIndexFeature;
		this.endIndexFeature = endIndexFeature;
		this.setName(super.getName() + "(" + criterion.getName() + "," + stopCriterion.getName() + "," + startIndexFeature.getName() + "," + endIndexFeature.getName() + ")");
	}
	
	public TokenSearchFeature(BooleanFeature<PosTaggedTokenWrapper> criterion, IntegerFeature<ParseConfigurationWrapper> startIndexFeature, IntegerFeature<ParseConfigurationWrapper> endIndexFeature, BooleanFeature<ParseConfigurationWrapper> findFirstFeature) {
		this.criterion = criterion;
		this.startIndexFeature = startIndexFeature;
		this.endIndexFeature = endIndexFeature;
		this.findFirstFeature = findFirstFeature;
		this.setName(super.getName() + "(" + criterion.getName() + "," + startIndexFeature.getName() + "," + endIndexFeature.getName() + "," + findFirstFeature.getName() + ")");
	}

	public TokenSearchFeature(BooleanFeature<PosTaggedTokenWrapper> criterion, BooleanFeature<PosTaggedTokenWrapper> stopCriterion, IntegerFeature<ParseConfigurationWrapper> startIndexFeature, IntegerFeature<ParseConfigurationWrapper> endIndexFeature, BooleanFeature<ParseConfigurationWrapper> findFirstFeature) {
		this.criterion = criterion;
		this.stopCriterion = stopCriterion;
		this.startIndexFeature = startIndexFeature;
		this.endIndexFeature = endIndexFeature;
		this.findFirstFeature = findFirstFeature;
		this.setName(super.getName() + "(" + criterion.getName() + "," + stopCriterion.getName() + "," + startIndexFeature.getName() + "," + endIndexFeature.getName() + "," + findFirstFeature.getName() + ")");
	}

	@Override
	public FeatureResult<PosTaggedTokenWrapper> checkInternal(ParseConfigurationWrapper context, RuntimeEnvironment env) {
		FeatureResult<PosTaggedTokenWrapper> featureResult = null;
		
		PosTagSequence posTagSequence = context.getParseConfiguration().getPosTagSequence();
		int startIndex = 0;
		int endIndex = posTagSequence.size()-1;
		
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
		
		if (startIndex>=posTagSequence.size())
			startIndex = posTagSequence.size()-1;
		if (endIndex>=posTagSequence.size())
			endIndex = posTagSequence.size()-1;
		
		int step = -1;
		if (endIndex>startIndex)
			step = 1;
		
		PosTaggedToken matchingToken = null;
		
		boolean findFirst = true;
		if (findFirstFeature!=null) {
			FeatureResult<Boolean> findFirstResult = this.findFirstFeature.check(context, env);
			if (findFirstResult==null) {
				return null;
			}
			findFirst = findFirstResult.getOutcome();
		}

		ParseConfigurationAddress parseConfigurationAddress = new ParseConfigurationAddress(env);
		parseConfigurationAddress.setParseConfiguration(context.getParseConfiguration());

		for (int i=startIndex; (step<0 && i>=0 && i>=endIndex) || (step>0 && i<posTagSequence.size() && i<=endIndex); i+=step) {
			PosTaggedToken oneToken = posTagSequence.get(i);
			parseConfigurationAddress.setPosTaggedToken(oneToken);

			FeatureResult<Boolean> criterionResult = this.criterion.check(parseConfigurationAddress, env);
			if (criterionResult!=null && criterionResult.getOutcome()) {
				matchingToken = oneToken;
				if (findFirst)
					break;
			}
			if (stopCriterion!=null) {
				FeatureResult<Boolean> stopCriterionResult = this.stopCriterion.check(parseConfigurationAddress, env);
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
