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
package com.joliciel.talismane.machineLearning.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.joliciel.talismane.utils.JolicielException;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * A top-level wrapper for a feature which refers
 * to one or more StringCollectionFeatures internally.<br/>
 * It is responsible for analysing the collection feature
 * and passing the resulting values one at a time to the wrapped feature.<br/>
 * The collection of results is converted to a List of WeightedOutcome of String, and then returned.<br/>
 * @author Assaf Urieli
 *
 * @param <T>
 * @param <Y>
 */
class StringCollectionFeatureWrapper<T> extends AbstractFeature<T,List<WeightedOutcome<String>>> implements
		 StringCollectionFeature<T> {
	private Feature<T, ?> wrappedFeature;
	private Set<StringCollectionFeature<T>> collectionFeatures;
	
	public StringCollectionFeatureWrapper(Feature<T, ?> wrappedFeature, Set<StringCollectionFeature<T>> collectionFeatures) {
		super();
		this.wrappedFeature = wrappedFeature;
		this.collectionFeatures = collectionFeatures;
		this.setName(this.wrappedFeature.getName());
	}

	public Feature<T, ?> getWrappedFeature() {
		return wrappedFeature;
	}

	@Override
	public FeatureResult<List<WeightedOutcome<String>>> check(T context, RuntimeEnvironment env) {
		List<WeightedOutcome<String>> finalList = new ArrayList<WeightedOutcome<String>>();
		FeatureResult<List<WeightedOutcome<String>>> finalResult = null;
		
		// get the collection results for each enclosed collection
		List<FeatureResult<List<WeightedOutcome<String>>>> collectionResultList = new ArrayList<FeatureResult<List<WeightedOutcome<String>>>>();
		for (StringCollectionFeature<T> collectionFeature : collectionFeatures) {
			FeatureResult<List<WeightedOutcome<String>>> collectionResults = collectionFeature.check(context, env);
			if (collectionResults!=null)
				collectionResultList.add(collectionResults);
		}
		
		if (collectionResultList.size()>0) {
			// we do a cross product of all of the results from all of the enclosed collections
			List<List<CollectionFeatureResult>> crossProduct = new ArrayList<List<CollectionFeatureResult>>();
			crossProduct.add(new ArrayList<CollectionFeatureResult>());
			for (FeatureResult<List<WeightedOutcome<String>>> collectionResults : collectionResultList) {
				String featureName = collectionResults.getFeature().getName();
				List<List<CollectionFeatureResult>> newCrossProduct = new ArrayList<List<CollectionFeatureResult>>();
				for (WeightedOutcome<String> collectionResult : collectionResults.getOutcome()) {
					for (List<CollectionFeatureResult> oneList : crossProduct) {
						List<CollectionFeatureResult> newList = new ArrayList<CollectionFeatureResult>(oneList);
						CollectionFeatureResult result = new CollectionFeatureResult();
						result.featureName = featureName;
						result.outcome = collectionResult.getOutcome();
						result.weight = collectionResult.getWeight();
						newList.add(result);
						newCrossProduct.add(newList);
					}
				}
				crossProduct = newCrossProduct;
			}
			
			// Test the wrapped feature for each set of collection results in the cross-product
			for (List<CollectionFeatureResult> oneCollectionResultSet : crossProduct) {
				String prefix = "";
				double weight = 1.0;
				for (CollectionFeatureResult result : oneCollectionResultSet) {
					env.setValue(result.featureName, result.outcome);
					prefix += result.outcome + "|";
					weight *= result.weight;
				}
				FeatureResult<?> featureResult = wrappedFeature.check(context, env);
				if (featureResult!=null) {
					if (wrappedFeature.getFeatureType().equals(StringFeature.class)) {
						String outcome = (String) featureResult.getOutcome();
						finalList.add(new WeightedOutcome<String>(prefix + outcome, weight));
					} else if (wrappedFeature.getFeatureType().equals(BooleanFeature.class)) {
						Boolean outcome = (Boolean) featureResult.getOutcome();
						finalList.add(new WeightedOutcome<String>(prefix + outcome.toString(), weight));
					} else if (wrappedFeature.getFeatureType().equals(DoubleFeature.class)) {
						Double outcome = (Double) featureResult.getOutcome();
						finalList.add(new WeightedOutcome<String>(prefix, weight * outcome.doubleValue()));
					} else if (wrappedFeature.getFeatureType().equals(IntegerFeature.class)) {
						Integer outcome = (Integer) featureResult.getOutcome();
						finalList.add(new WeightedOutcome<String>(prefix, weight * outcome.doubleValue()));
					} else {
						throw new JolicielException("Cannot include collections in a top-level feature or type: " + wrappedFeature.getFeatureType().getSimpleName());
					}
				}
			}
			if (finalList.size()>0)
				finalResult = this.generateResult(finalList);
		} // have any collection results
		
		return finalResult;
	}

	private static final class CollectionFeatureResult {
		String featureName;
		String outcome;
		double weight;
	}
}
