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
 * Reverses a feature using a boolean NOT.
 * If it is null, will return null.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class NotFeature<T> extends AbstractCachableFeature<T, Boolean> implements BooleanFeature<T> {
	BooleanFeature<T> operand;
	
	public NotFeature(BooleanFeature<T> operand) {
		super();
		this.operand = operand;
		this.setName("Not(" + operand.getName() + ")");
	}

	@Override
	public FeatureResult<Boolean> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<Boolean> featureResult = null;
		
		FeatureResult<Boolean> result1 = operand.check(context, env);
		
		if (result1!=null) {
			featureResult = this.generateResult(!result1.getOutcome());
		}
		return featureResult;
	}
	
	@Override
	public boolean addDynamicSourceCode(DynamicSourceCodeBuilder<T> builder,
			String variableName) {
		String op = builder.addFeatureVariable(operand, "operand");
		
		builder.append("if (" + op + "!=null) {");
		builder.indent();
		builder.append(		variableName + " = !(" + op + ");");
		builder.outdent();
		builder.append("}");
		return true;
	}

	public BooleanFeature<T> getOperand() {
		return operand;
	}

	public void setOperand(BooleanFeature<T> operand) {
		this.operand = operand;
	}
	
	
}
