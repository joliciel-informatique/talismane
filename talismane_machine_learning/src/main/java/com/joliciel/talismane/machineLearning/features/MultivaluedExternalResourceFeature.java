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
package com.joliciel.talismane.machineLearning.features;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.ExternalResource;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.utils.JolicielException;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * An external resource feature for an external resource with multiple classes
 * per key.
 * 
 * @author Assaf Urieli
 *
 */
public class MultivaluedExternalResourceFeature<T> extends AbstractStringCollectionFeature<T>implements StringCollectionFeature<T> {
	ExternalResourceFinder externalResourceFinder;

	StringFeature<T> resourceNameFeature;
	StringFeature<T>[] keyElementFeatures;

	@SafeVarargs
	public MultivaluedExternalResourceFeature(StringFeature<T> resourceNameFeature, StringFeature<T>... keyElementFeatures) {
		this.resourceNameFeature = resourceNameFeature;
		this.keyElementFeatures = keyElementFeatures;

		String name = super.getName() + "(" + resourceNameFeature.getName() + ",";

		for (StringFeature<T> stringFeature : keyElementFeatures) {
			name += stringFeature.getName();
		}
		name += ")";
		this.setName(name);
	}

	@Override
	public FeatureResult<List<WeightedOutcome<String>>> checkInternal(T context, RuntimeEnvironment env) throws TalismaneException {
		FeatureResult<List<WeightedOutcome<String>>> result = null;
		FeatureResult<String> resourceNameResult = resourceNameFeature.check(context, env);
		if (resourceNameResult != null) {
			String resourceName = resourceNameResult.getOutcome();

			@SuppressWarnings("unchecked")
			ExternalResource<List<WeightedOutcome<String>>> externalResource = (ExternalResource<List<WeightedOutcome<String>>>) externalResourceFinder
					.getExternalResource(resourceName);
			if (externalResource == null) {
				throw new JolicielException("External resource not found: " + resourceName);
			}

			List<String> keyElements = new ArrayList<String>();
			for (StringFeature<T> stringFeature : keyElementFeatures) {
				FeatureResult<String> keyElementResult = stringFeature.check(context, env);
				if (keyElementResult == null) {
					return null;
				}
				String keyElement = keyElementResult.getOutcome();
				keyElements.add(keyElement);
			}
			List<WeightedOutcome<String>> outcomes = externalResource.getResult(keyElements);
			if (outcomes != null && outcomes.size() > 0)
				result = this.generateResult(outcomes);
		}

		return result;
	}

	public ExternalResourceFinder getExternalResourceFinder() {
		return externalResourceFinder;
	}

	public void setExternalResourceFinder(ExternalResourceFinder externalResourceFinder) {
		this.externalResourceFinder = externalResourceFinder;
	}

	public StringFeature<T> getResourceNameFeature() {
		return resourceNameFeature;
	}

	public void setResourceNameFeature(StringFeature<T> resourceNameFeature) {
		this.resourceNameFeature = resourceNameFeature;
	}

	public StringFeature<T>[] getKeyElementFeatures() {
		return keyElementFeatures;
	}

	public void setKeyElementFeatures(StringFeature<T>[] keyElementFeatures) {
		this.keyElementFeatures = keyElementFeatures;
	}

}
