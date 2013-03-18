///////////////////////////////////////////////////////////////////////////////
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
package com.joliciel.talismane.posTagger.features;

import com.joliciel.talismane.machineLearning.features.AbstractCachableFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;

/**
 * An Abstract base class for features applied to a given pos-tagged token.
 * @author Assaf Urieli
 *
 */
public abstract class AbstractPosTaggedTokenFeature<T,Y> extends AbstractCachableFeature<T,Y> implements PosTaggedTokenFeature<T,Y> {
	PosTaggedTokenAddressFunction<T> addressFunction;
	
	public AbstractPosTaggedTokenFeature (PosTaggedTokenAddressFunction<T> addressFunction) {
		this.addressFunction = addressFunction;
	}
	
	public PosTaggedTokenAddressFunction<T> getAddressFunction() {
		return addressFunction;
	}

	public void setAddressFunction(PosTaggedTokenAddressFunction<T> addressFunction) {
		this.addressFunction = addressFunction;
		String name = this.getName();
		if (name.endsWith(")")) {
			name = name.substring(0, name.length()-1) + "," + addressFunction.getName() + ")";
		} else {
			name = name + "(" + addressFunction.getName() + ")";
		}
		this.setName(name);
	}
	
	protected PosTaggedTokenWrapper getToken(T context, RuntimeEnvironment env) {
		FeatureResult<PosTaggedTokenWrapper> tokenResult = addressFunction.check(context, env);
		if (tokenResult==null)
			return null;
		return tokenResult.getOutcome();
	}
}
