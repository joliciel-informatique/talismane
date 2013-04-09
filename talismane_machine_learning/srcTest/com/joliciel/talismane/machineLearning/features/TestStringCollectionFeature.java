package com.joliciel.talismane.machineLearning.features;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.machineLearning.features.AbstractStringCollectionFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.StringCollectionFeature;
import com.joliciel.talismane.utils.WeightedOutcome;

public class TestStringCollectionFeature extends AbstractStringCollectionFeature<TestContext> implements StringCollectionFeature<TestContext> {
	
	StringFeature<TestContext>[] stringFeatures;
	
	public TestStringCollectionFeature(StringFeature<TestContext>... stringFeatures) {
		this.stringFeatures = stringFeatures;
		String name = super.getName() + "(";
		boolean firstFeature = true;
		for (StringFeature<TestContext> stringFeature : stringFeatures) {
			if (!firstFeature)
				name += ",";
			name += stringFeature.getName();
			firstFeature = false;
		}
		name += ")";
		this.setName(name);
	}

	@Override
	public FeatureResult<List<WeightedOutcome<String>>> checkInternal(
			TestContext context, RuntimeEnvironment env) {
		List<WeightedOutcome<String>> abcList = new ArrayList<WeightedOutcome<String>>();
		double weight = 1.0;
		for (StringFeature<TestContext> stringFeature : stringFeatures) {
			FeatureResult<String> result = stringFeature.check(context, env);
			
			abcList.add(new WeightedOutcome<String>(result.getOutcome(), weight));
			weight -= 0.1;
		}
		
		return this.generateResult(abcList);
	}
}
