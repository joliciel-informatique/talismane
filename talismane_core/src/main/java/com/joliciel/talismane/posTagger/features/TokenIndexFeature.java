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

import com.joliciel.talismane.machineLearning.features.DynamicSourceCodeBuilder;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.tokeniser.Token;

/**
 * The index of a given token in the token sequence.
 * @author Assaf Urieli
 *
 */
public final class TokenIndexFeature<T> extends AbstractPosTaggedTokenFeature<T,Integer> implements IntegerFeature<T> {
	public TokenIndexFeature(PosTaggedTokenAddressFunction<T> addressFunction) {
		super(addressFunction);
		this.setAddressFunction(addressFunction);
	}

	public TokenIndexFeature() {
		super(new ItsMeAddressFunction<T>());
	}
	
	@Override
	public FeatureResult<Integer> checkInternal(T context, RuntimeEnvironment env) {
		PosTaggedTokenWrapper innerWrapper = this.getToken(context, env);
		if (innerWrapper==null)
			return null;
		PosTaggedToken posTaggedToken = innerWrapper.getPosTaggedToken();
		if (posTaggedToken==null)
			return null;
		
		FeatureResult<Integer> featureResult = null;
		Token token = posTaggedToken.getToken();
		int index = token.getIndex();
		featureResult = this.generateResult(index);

		return featureResult;
	}
	

	@Override
	public boolean addDynamicSourceCode(
			DynamicSourceCodeBuilder<T> builder,
			String variableName) {
		String address = builder.addFeatureVariable(addressFunction, "address");
		builder.append("if (" + address + "!=null) {" );
		builder.indent();
		builder.append(	variableName + " = " + address + ".getPosTaggedToken().getToken().getIndex();");
		builder.outdent();
		builder.append("}");
		return true;
	}
}
