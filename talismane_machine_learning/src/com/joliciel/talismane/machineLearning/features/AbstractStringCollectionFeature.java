package com.joliciel.talismane.machineLearning.features;

import java.util.List;

import com.joliciel.talismane.utils.WeightedOutcome;

public abstract class AbstractStringCollectionFeature<T> extends AbstractCachableFeature<T, List<WeightedOutcome<String>>>
		implements StringCollectionFeature<T> {

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return StringCollectionFeature.class;
	}

}
