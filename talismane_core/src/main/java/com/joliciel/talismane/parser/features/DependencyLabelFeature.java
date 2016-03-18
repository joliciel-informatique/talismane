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
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane. If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.parser.features;

import com.joliciel.talismane.machineLearning.features.DynamicSourceCodeBuilder;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenAddressFunction;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;

/**
 * The dependency label of a given token's governing dependency,
 * where the token is referenced by address.
 * @author Assaf Urieli
 *
 */
public final class DependencyLabelFeature extends AbstractParseConfigurationAddressFeature<String>
implements StringFeature<ParseConfigurationWrapper> {

	public DependencyLabelFeature(PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction) {
		super(addressFunction);
		String name = this.getName() + "(" + addressFunction.getName() + ")";
		this.setName(name);
	}

	@Override
	public FeatureResult<String> checkInternal(ParseConfigurationWrapper wrapper, RuntimeEnvironment env) {
		PosTaggedTokenWrapper innerWrapper = this.getToken(wrapper, env);
		if (innerWrapper==null)
			return null;
		PosTaggedToken posTaggedToken = innerWrapper.getPosTaggedToken();
		if (posTaggedToken==null)
			return null;

		FeatureResult<String> featureResult = null;

		ParseConfiguration configuration = wrapper.getParseConfiguration();
		DependencyArc arc = configuration.getGoverningDependency(posTaggedToken);
		if (arc!=null) {
			String label = arc.getLabel();
			if (label==null)
				label = "null";
			featureResult = this.generateResult(label);
		}
		
		return featureResult;
	}
	
	@Override
	public boolean addDynamicSourceCode(
			DynamicSourceCodeBuilder<ParseConfigurationWrapper> builder,
			String variableName) {
		String address = builder.addFeatureVariable(addressFunction, "address");
		builder.append("if (" + address + "!=null) {" );
		builder.indent();
		String arc = builder.getVarName("arc");
		builder.addImport(DependencyArc.class);
		builder.append(		"DependencyArc " + arc + " = context.getParseConfiguration().getGoverningDependency(" + address + ".getPosTaggedToken());");
		builder.append("if (" + arc + "!=null)");
		builder.indent();
		builder.append(		variableName + " = " + arc + ".getLabel()==null ? \"null\" : " + arc + ".getLabel();");
		builder.outdent();
		builder.outdent();
		builder.append("}");
		return true;
	}
}