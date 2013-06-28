package com.joliciel.talismane.machineLearning.features;

public class StringFeatureDynamiser extends
		AbstractDynamiser<String> {

	public StringFeatureDynamiser(Class<String> clazz) {
		super(clazz);
	}

	@Override
	protected Class<?> getOutcomeTypeExtended(Feature<String, ?> feature) {
		return null;
	}

}
