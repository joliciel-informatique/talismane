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
 * Rounds a double to the nearest integer.
 * @author Assaf Urieli
 *
 */
class RoundFeature<T> extends AbstractFeature<T, Integer> implements
		IntegerFeature<T> {
	private DoubleFeature<T> featureToRound;
	
	public RoundFeature(DoubleFeature<T> featureToRound) {
		super();
		this.featureToRound = featureToRound;
		this.setName(this.featureToRound.getName());
	}

	@Override
	public FeatureResult<Integer> check(T context, RuntimeEnvironment env) {
		FeatureResult<Integer> featureResult = null;
		
		FeatureResult<Double> doubleResult = featureToRound.check(context, env);
		if (doubleResult!=null) {
			int intResult = (int) Math.round(doubleResult.getOutcome());
			featureResult = this.generateResult(intResult);
		}
		return featureResult;
	}


	@Override
	public boolean addDynamicSourceCode(DynamicSourceCodeBuilder<T> builder,
			String variableName) {
		String op = builder.addFeatureVariable(featureToRound, "operand");
		
		builder.append("if (" + op + "!=null) {");
		builder.indent();
		builder.append(		variableName + " = (int) Math.round(" + op + ");");
		builder.outdent();
		builder.append("}");
		return true;
	}
	
	public DoubleFeature<T> getFeatureToRound() {
		return featureToRound;
	}

	public void setFeatureToRound(DoubleFeature<T> featureToRound) {
		this.featureToRound = featureToRound;
	}

	
}
