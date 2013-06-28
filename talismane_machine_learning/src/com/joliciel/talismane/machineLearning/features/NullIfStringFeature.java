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
 * If the condition returns true, return null, else return the result of the feature provided.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class NullIfStringFeature<T> extends AbstractCachableFeature<T,String> implements
		StringFeature<T> {
	private BooleanFeature<T> condition;
	private StringFeature<T> resultFeature;
	
	public NullIfStringFeature(BooleanFeature<T> condition, StringFeature<T> resultFeature) {
		super();
		this.condition = condition;
		this.resultFeature = resultFeature;
		this.setName("NullIf(" + condition.getName() + "," + resultFeature.getName() + ")");
	}

	@Override
	protected FeatureResult<String> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<String> featureResult = null;
		
		FeatureResult<Boolean> conditionResult = condition.check(context, env);
		if (conditionResult!=null) {
			boolean conditionOutcome = conditionResult.getOutcome();
			if (!conditionOutcome) {
				FeatureResult<String> thenFeatureResult = resultFeature.check(context, env);
				if (thenFeatureResult!=null) {
					String result = thenFeatureResult.getOutcome();
					featureResult = this.generateResult(result);
				}
			}
		}
		
		return featureResult;
		
	}
	
	@Override
	public boolean addDynamicSourceCode(DynamicSourceCodeBuilder<T> builder, String variableName) {
		String cond = builder.addFeatureVariable(condition, "condition");
		
		builder.append("if (" + cond + "!=null && !" + cond + ") {");
		builder.indent();
		String result = 	builder.addFeatureVariable(resultFeature, "result");
		
		builder.append(		variableName + " = " + result + ";");
		builder.outdent();
		builder.append("}");

		return true;
	}
	
	public BooleanFeature<T> getCondition() {
		return condition;
	}

	public StringFeature<T> getResultFeature() {
		return resultFeature;
	}

	public void setCondition(BooleanFeature<T> condition) {
		this.condition = condition;
	}

	public void setResultFeature(StringFeature<T> resultFeature) {
		this.resultFeature = resultFeature;
	}
	
}
