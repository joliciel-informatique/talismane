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
 * If the wrapped boolean feature returns false, will convert it to a null.
 * Useful to keep the feature sparse, so that only true values return a result.
 * @author Assaf Urieli
 *
 */
public class OnlyTrueFeature<T> extends AbstractCachableFeature<T, Boolean> implements BooleanFeature<T> {
	BooleanFeature<T> wrappedFeature;
	
	public OnlyTrueFeature(BooleanFeature<T> wrappedFeature) {
		super();
		this.wrappedFeature = wrappedFeature;
		this.setName(wrappedFeature.getName() + "*");
	}

	@Override
	public FeatureResult<Boolean> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<Boolean> featureResult = null;
		
		FeatureResult<Boolean> result1 = wrappedFeature.check(context, env);
		
		if (result1!=null && result1.getOutcome().booleanValue()==true) {
			featureResult = this.generateResult(true);
		}
		return featureResult;
	}

	@Override
	public boolean addDynamicSourceCode(DynamicSourceCodeBuilder<T> builder, String variableName) {
		String result = builder.addFeatureVariable(wrappedFeature, "result");
		
		builder.append("if (" + result + "!=null && " + result + ".booleanValue()) {");
		builder.indent();
		builder.append(		variableName + " = true;");
		builder.outdent();
		builder.append("}");

		return true;
	}
	public BooleanFeature<T> getWrappedFeature() {
		return wrappedFeature;
	}

	public void setWrappedFeature(BooleanFeature<T> wrappedFeature) {
		this.wrappedFeature = wrappedFeature;
	}
	
	
}
