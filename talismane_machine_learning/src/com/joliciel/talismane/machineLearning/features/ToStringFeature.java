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
package com.joliciel.talismane.machineLearning.features;

/**
 * Converts a non-string feature to a string feature.
 * If the feature result is null, will return the string "null".
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class ToStringFeature<T> extends AbstractCachableFeature<T, String> implements StringFeature<T> {
	private static final String NULL_STRING = "null";

	Feature<T,?> featureToString;
	
	public ToStringFeature(Feature<T,?> feature1) {
		super();
		this.featureToString = feature1;
		this.setName("ToString(" + feature1.getName() + ")");
	}

	@Override
	public FeatureResult<String> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<String> featureResult = null;
		
		FeatureResult<?> result1 = featureToString.check(context, env);
		
		if (result1!=null) {
			featureResult = this.generateResult(result1.getOutcome().toString());
		} else {
			featureResult = this.generateResult(NULL_STRING);
		}
		return featureResult;
	}


	@Override
	public boolean addDynamicSourceCode(DynamicSourceCodeBuilder<T> builder,
			String variableName) {
		String op = builder.addFeatureVariable(featureToString, "operand");
		
		builder.append("if (" + op + "!=null) {");
		builder.indent();
		builder.append(		variableName + " = " + op + ".toString();");
		builder.outdent();
		builder.append("} else {");
		builder.indent();
		builder.append(		variableName + " = \"" + NULL_STRING + "\";");
		builder.outdent();
		builder.append("}");
		return true;
	}
	
	public Feature<T, ?> getFeatureToString() {
		return featureToString;
	}

	public void setFeatureToString(Feature<T, ?> featureToString) {
		this.featureToString = featureToString;
	}
	
	
}
