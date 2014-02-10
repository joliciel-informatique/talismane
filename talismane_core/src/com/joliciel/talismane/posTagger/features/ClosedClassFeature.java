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
package com.joliciel.talismane.posTagger.features;

import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.DynamicSourceCodeBuilder;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.PosTagOpenClassIndicator;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * Whether or not the pos-tag assigned to this token is a closed-class category.
 * @author Assaf Urieli
 *
 */
public final class ClosedClassFeature<T> extends AbstractPosTaggedTokenFeature<T,Boolean> implements BooleanFeature<T> {
	public ClosedClassFeature(PosTaggedTokenAddressFunction<T> addressFunction) {
		super(addressFunction);
		this.setAddressFunction(addressFunction);
	}

	@Override
	public FeatureResult<Boolean> checkInternal(T context, RuntimeEnvironment env) {
		PosTaggedTokenWrapper innerWrapper = this.getToken(context, env);
		if (innerWrapper==null)
			return null;
		PosTaggedToken posTaggedToken = innerWrapper.getPosTaggedToken();
		if (posTaggedToken==null)
			return null;
		
		FeatureResult<Boolean> featureResult = null;
		boolean isClosedClass = posTaggedToken.getTag().getOpenClassIndicator().equals(PosTagOpenClassIndicator.CLOSED);
		featureResult = this.generateResult(isClosedClass);
		
		return featureResult;
	}


	@Override
	public boolean addDynamicSourceCode(
			DynamicSourceCodeBuilder<T> builder,
			String variableName) {
		String addressFunctionName = builder.addFeatureVariable(addressFunction, "address");
		builder.append("if (" + addressFunctionName + "!=null) {" );
		builder.indent();
		builder.addImport(PosTagOpenClassIndicator.class);
		builder.append(	variableName + " = " + addressFunctionName + ".getPosTaggedToken().getTag().getOpenClassIndicator().equals(PosTagOpenClassIndicator.CLOSED);");
		builder.outdent();
		builder.append("}");
		return true;
	}
}
