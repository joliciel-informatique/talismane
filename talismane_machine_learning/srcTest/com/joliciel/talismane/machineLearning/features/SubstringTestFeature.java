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
 * A feature for testing on string contexts.
 * @author Assaf Urieli
 *
 */
public class SubstringTestFeature extends AbstractFeature<String,String> implements StringFeature<String> {
	private IntegerFeature<String> startFeature;
	private IntegerFeature<String> endFeature;
	
	public SubstringTestFeature(IntegerFeature<String> startFeature,
			IntegerFeature<String> endFeature) {
		super();
		this.startFeature = startFeature;
		this.endFeature = endFeature;
		
		this.setName(this.getName() + "(" + this.startFeature.getName() + "," + this.endFeature.getName() + ")");
	}

	@Override
	public FeatureResult<String> check(String context, RuntimeEnvironment env) {
		FeatureResult<String> result = null;
		FeatureResult<Integer> startResult = startFeature.check(context, env);
		FeatureResult<Integer> endResult = endFeature.check(context, env);
		
		if (startResult!=null && endResult!=null) {
			int start = startResult.getOutcome();
			int end = endResult.getOutcome();
			
			if (start >= 0 && end <= context.length()
					&& start <= end) {
				String subString = context.substring(start, end);
				result = this.generateResult(subString);
			}
		}
		return result;
	}
	
	@Override
	public boolean addDynamicSourceCode(
			DynamicSourceCodeBuilder<String> builder, String variableName) {
		String start = builder.addFeatureVariable(startFeature, "start");
		String end = builder.addFeatureVariable(endFeature, "end");
		
		builder.append("if (" + start + "!=null && " + end + "!=null) {");
		builder.indent();
		builder.append("if (" + start + ">=0 && " + end + "<= context.length() && " + start + "<=" + end + ") {" );
		builder.indent();
		builder.append(variableName + "=context.substring(" + start + ", " + end + ");");
		builder.outdent();
		builder.append("}");
		builder.outdent();
		builder.append("}");
		return true;
	}

	public IntegerFeature<String> getStartFeature() {
		return startFeature;
	}

	public void setStartFeature(IntegerFeature<String> startFeature) {
		this.startFeature = startFeature;
	}

	public IntegerFeature<String> getEndFeature() {
		return endFeature;
	}

	public void setEndFeature(IntegerFeature<String> endFeature) {
		this.endFeature = endFeature;
	}

}
