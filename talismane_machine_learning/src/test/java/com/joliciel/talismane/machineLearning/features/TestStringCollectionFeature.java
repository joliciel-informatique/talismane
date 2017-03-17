package com.joliciel.talismane.machineLearning.features;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.utils.WeightedOutcome;

public class TestStringCollectionFeature extends AbstractStringCollectionFeature<String>implements StringCollectionFeature<String> {

	StringFeature<String>[] stringFeatures;

	@SafeVarargs
	public TestStringCollectionFeature(StringFeature<String>... stringFeatures) {
		this.stringFeatures = stringFeatures;
		String name = super.getName() + "(";
		boolean firstFeature = true;
		for (StringFeature<String> stringFeature : stringFeatures) {
			if (!firstFeature)
				name += ",";
			name += stringFeature.getName();
			firstFeature = false;
		}
		name += ")";
		this.setName(name);
	}

	@Override
	public FeatureResult<List<WeightedOutcome<String>>> checkInternal(String context, RuntimeEnvironment env) throws TalismaneException {
		List<WeightedOutcome<String>> abcList = new ArrayList<WeightedOutcome<String>>();
		double weight = 1.0;
		for (StringFeature<String> stringFeature : stringFeatures) {
			FeatureResult<String> result = stringFeature.check(context, env);

			abcList.add(new WeightedOutcome<String>(result.getOutcome(), weight));
			weight -= 0.1;
		}

		return this.generateResult(abcList);
	}
}
