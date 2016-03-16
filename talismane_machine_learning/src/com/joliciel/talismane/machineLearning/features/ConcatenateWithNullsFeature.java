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

/**
 * Merges two or more string features by concatenating their results and adding a | in between.
 * Includes the string "null" if any of the results is null.
 * @author Assaf Urieli
 *
 */
public class ConcatenateWithNullsFeature<T> extends AbstractCachableFeature<T, String> implements
		StringFeature<T> {
	private static final String NULL_RESULT = "null";
	
	StringFeature<T>[] stringFeatures;
	
	@SafeVarargs
	public ConcatenateWithNullsFeature(StringFeature<T>... stringFeatures) {
		super();
		this.stringFeatures = stringFeatures;
		String name = "Concat(";
		boolean firstFeature = true;
		for (StringFeature<T> stringFeature : stringFeatures) {
			if (!firstFeature)
				name += ",";
			name += stringFeature.getName();
			firstFeature = false;
		}
		name += ")";
		this.setName(name);
	}

	@Override
	public FeatureResult<String> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<String> featureResult = null;
		
		StringBuilder sb = new StringBuilder();
		boolean firstFeature = true;
		for (StringFeature<T> stringFeature : stringFeatures) {
			if (!firstFeature)
				sb.append("|");
			FeatureResult<String> result = stringFeature.check(context, env);
			if (result==null)
				sb.append(NULL_RESULT);
			else
				sb.append(result.getOutcome());
			firstFeature = false;
		}
		
		featureResult = this.generateResult(sb.toString());
		return featureResult;
	}
	
	@Override
	public boolean addDynamicSourceCode(DynamicSourceCodeBuilder<T> builder,
			String variableName) {
		
		String sb = builder.getVarName("sb");
		builder.append("StringBuilder " + sb + " = new StringBuilder();");
		boolean firstFeature = true;
		for (StringFeature<T> stringFeature : stringFeatures) {
			if (!firstFeature) {
				builder.append(sb + ".append(\"|\");");
			}
			String stringFeatureName = builder.addFeatureVariable(stringFeature, "string");
			
			builder.append("if (" + stringFeatureName + "==null)");
			builder.indent();
			builder.append(	sb + ".append(\"" + NULL_RESULT + "\");");
			builder.outdent();
			builder.append("else");
			builder.indent();
			builder.append(	sb + ".append(" + stringFeatureName + ");");
			builder.outdent();
			firstFeature = false;
		}
		
		builder.append(variableName + " = " + sb + ".toString();");
		return true;
	}

	public StringFeature<T>[] getStringFeatures() {
		return stringFeatures;
	}

}
