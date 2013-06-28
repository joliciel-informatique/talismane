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
package com.joliciel.talismane.parser.features;

import java.util.List;

import com.joliciel.talismane.machineLearning.features.DynamicSourceCodeBuilder;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;

/**
 * Retrieves the left-most left-hand dependent of the reference token.
 * @author Assaf Urieli
 *
 */
public final class AddressFunctionLDep extends AbstractAddressFunction {
	private ParserAddressFunction addressFunction;
	
	public AddressFunctionLDep(ParserAddressFunction addressFunction) {
		super();
		this.addressFunction = addressFunction;
		this.setName("LDep(" + addressFunction.getName() + ")");
	}

	@Override
	public FeatureResult<PosTaggedTokenWrapper> checkInternal(ParseConfigurationWrapper wrapper, RuntimeEnvironment env) {
		ParseConfiguration configuration = wrapper.getParseConfiguration();
		PosTaggedToken resultToken = null;
		FeatureResult<PosTaggedTokenWrapper> addressResult = addressFunction.check(configuration, env);
		if (addressResult!=null) {
			PosTaggedToken referenceToken = addressResult.getOutcome().getPosTaggedToken();
			List<PosTaggedToken> leftDependents = configuration.getLeftDependents(referenceToken);
			if (leftDependents.size()>0)
				resultToken = leftDependents.get(0);
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
		String address = builder.addFeatureVariable(addressFunction, "address");
		builder.append("if (" + address + "!=null) {" );
		builder.indent();
		String leftDependents = builder.getVarName("leftDependents");
		builder.addImport(List.class);
		
		builder.append("List<PosTaggedToken> " + leftDependents + " = context.getParseConfiguration().getLeftDependents(" + address + ".getPosTaggedToken());");
		builder.append("if (" + leftDependents + ".size()>0)");
		builder.indent();
		builder.append(		variableName + " = " + leftDependents + ".get(0);");
		builder.outdent();
		builder.outdent();
		builder.append("}");
		return true;
	}
}
