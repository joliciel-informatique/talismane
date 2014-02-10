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
 * Mimics an in-then-else structure - if condition is true return thenFeature result, else return elseFeature result.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class IfThenElseIntegerFeature<T> extends AbstractCachableFeature<T,Integer> implements
		IntegerFeature<T> {
	private BooleanFeature<T> condition;
	private IntegerFeature<T> thenFeature;
	private IntegerFeature<T> elseFeature;
	
	public IfThenElseIntegerFeature(BooleanFeature<T> condition, IntegerFeature<T> thenFeature, IntegerFeature<T> elseFeature) {
		super();
		this.condition = condition;
		this.thenFeature = thenFeature;
		this.elseFeature = elseFeature;
		this.setName("IfThenElse(" + condition.getName() + "," + thenFeature.getName() + "," + elseFeature.getName() + ")");
	}

	@Override
	protected FeatureResult<Integer> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<Integer> featureResult = null;
		
		FeatureResult<Boolean> conditionResult = condition.check(context, env);
		if (conditionResult!=null) {
			boolean conditionOutcome = conditionResult.getOutcome();
			if (conditionOutcome) {
				FeatureResult<Integer> thenFeatureResult = thenFeature.check(context, env);
				if (thenFeatureResult!=null) {
					int result = thenFeatureResult.getOutcome();
					featureResult = this.generateResult(result);
				}
			} else {
				FeatureResult<Integer> elseFeatureResult = elseFeature.check(context, env);
				if (elseFeatureResult!=null) {
					int result = elseFeatureResult.getOutcome();
					featureResult = this.generateResult(result);
				}
			}
		}
		
		
		return featureResult;
		
	}

	
	@Override
	public boolean addDynamicSourceCode(DynamicSourceCodeBuilder<T> builder, String variableName) {
		String condition1 = builder.addFeatureVariable(condition, "condition");
		
		builder.append("if (" + condition1 + "!=null) {");
		builder.indent();
		builder.append(		"if (" + condition1 +") {");
		builder.indent();
		String thenResult = 	builder.addFeatureVariable(thenFeature, "then");
		builder.append(			"if (" + thenResult + "!=null) " + variableName + " = " + thenResult + ";");
		builder.outdent();
		builder.append(		"} else {");
		builder.indent();
		String elseResult = 	builder.addFeatureVariable(elseFeature, "else");
		builder.append(			"if (" + elseResult + "!=null) " + variableName + " = " + elseResult + ";");
		builder.outdent();
		builder.append(		"}");
		builder.outdent();
		builder.append("}");
		
		return true;
	}
	
	public BooleanFeature<T> getCondition() {
		return condition;
	}

	public IntegerFeature<T> getThenFeature() {
		return thenFeature;
	}

	public IntegerFeature<T> getElseFeature() {
		return elseFeature;
	}

	public void setCondition(BooleanFeature<T> condition) {
		this.condition = condition;
	}

	public void setThenFeature(IntegerFeature<T> thenFeature) {
		this.thenFeature = thenFeature;
	}

	public void setElseFeature(IntegerFeature<T> elseFeature) {
		this.elseFeature = elseFeature;
	}

}
