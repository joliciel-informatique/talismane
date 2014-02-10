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
package com.joliciel.talismane.parser.features;

import java.util.Iterator;

import com.joliciel.talismane.machineLearning.features.DynamicSourceCodeBuilder;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;

/**
 * Retrieves the nth item from the stack.
 * @author Assaf Urieli
 *
 */
public final class AddressFunctionStack extends AbstractAddressFunction {
	private IntegerFeature<ParseConfigurationWrapper> indexFeature;
	
	public AddressFunctionStack(IntegerFeature<ParseConfigurationWrapper> indexFeature) {
		super();
		this.indexFeature = indexFeature;
		this.setName("Stack[" + indexFeature.getName() + "]");
	}
	
	@Override
	public FeatureResult<PosTaggedTokenWrapper> checkInternal(ParseConfigurationWrapper wrapper, RuntimeEnvironment env) {
		ParseConfiguration configuration = wrapper.getParseConfiguration();
		PosTaggedToken resultToken = null;
		FeatureResult<Integer> indexResult = indexFeature.check(wrapper, env);
		if (indexResult!=null) {
			int index = indexResult.getOutcome();
			Iterator<PosTaggedToken> stackIterator = configuration.getStack().iterator();
			
			for (int i=0; i<=index; i++) {
				if (!stackIterator.hasNext()) {
					resultToken = null;
					break;
				}
				resultToken = stackIterator.next();
			}
		}
		FeatureResult<PosTaggedTokenWrapper> featureResult = null;
		if (resultToken!=null)
			featureResult = this.generateResult(resultToken);
		return featureResult;
	}

	@Override
	public boolean addDynamicSourceCode(
			DynamicSourceCodeBuilder<ParseConfigurationWrapper> builder,
			String variableName) {
		
		String indexName = builder.addFeatureVariable(indexFeature, "index");
		
		builder.append("if (" + indexName + "!=null) {");
		builder.indent();
		String stackIterator = builder.getVarName("stackIterator");
		builder.addImport(Iterator.class);
		builder.addImport(PosTaggedToken.class);
		
		builder.append(	"Iterator<PosTaggedToken> " + stackIterator + " = context.getParseConfiguration().getStack().iterator();");
		
		builder.append(	"for (int i=0; i<=" + indexName + "; i++) {");
		builder.indent();
		builder.append(		"if (!" + stackIterator + ".hasNext()) {");
		builder.indent();
		builder.append(			variableName + " = null;");
		builder.append(			"break;");
	    builder.outdent();
		builder.append(		"}");
		builder.append(	variableName + " = " + stackIterator + ".next();");
		builder.outdent();
		builder.append("}");
		
		builder.outdent();
		builder.append("}");
		return true;
	}
	
	
}
