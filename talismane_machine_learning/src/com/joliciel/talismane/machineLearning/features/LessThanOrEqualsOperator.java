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
 * Returns operand1 <= operand2.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class LessThanOrEqualsOperator<T> extends AbstractCachableFeature<T,Boolean> implements
		BooleanFeature<T> {
	private DoubleFeature<T> operand1;
	private DoubleFeature<T> operand2;
	
	public LessThanOrEqualsOperator(DoubleFeature<T> operand1, DoubleFeature<T> operand2) {
		super();
		this.operand1 = operand1;
		this.operand2 = operand2;
		this.setName("(" + operand1.getName() + "<=" + operand2.getName() + ")");
	}

	@Override
	protected FeatureResult<Boolean> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<Boolean> featureResult = null;
		
		FeatureResult<Double> operand1Result = operand1.check(context, env);
		FeatureResult<Double> operand2Result = operand2.check(context, env);
		
		if (operand1Result!=null && operand2Result!=null) {
			boolean result = operand1Result.getOutcome() <= operand2Result.getOutcome();
			featureResult = this.generateResult(result);
		}
		
		return featureResult;	
	}

	@Override
	public boolean addDynamicSourceCode(DynamicSourceCodeBuilder<T> builder, String variableName) {
		String operand1Name = builder.addFeatureVariable(operand1, "operand");
		String operand2Name = builder.addFeatureVariable(operand2, "operand");
		
		builder.append("if (" + operand1Name + "!=null && " + operand2Name + "!=null) {");
		builder.indent();
		builder.append(variableName + "=" + operand1Name + "<=" + operand2Name + ";");
		builder.outdent();
		builder.append("}");
		
		return true;
	}
	
	public DoubleFeature<T> getOperand1() {
		return operand1;
	}

	public DoubleFeature<T> getOperand2() {
		return operand2;
	}

	public void setOperand1(DoubleFeature<T> operand1) {
		this.operand1 = operand1;
	}

	public void setOperand2(DoubleFeature<T> operand2) {
		this.operand2 = operand2;
	}


}
