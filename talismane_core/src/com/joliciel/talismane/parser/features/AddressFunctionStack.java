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

import java.util.Iterator;

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * Retrieves the nth item from the stack.
 * @author Assaf Urieli
 *
 */
public class AddressFunctionStack extends AbstractAddressFunction {
	private IntegerFeature<ParseConfiguration> indexFeature;
	
	public AddressFunctionStack(IntegerFeature<ParseConfiguration> indexFeature) {
		super();
		this.indexFeature = indexFeature;
		this.setName("Stack[" + indexFeature.getName() + "]");
	}
	
	@Override
	public FeatureResult<PosTaggedToken> checkInternal(ParseConfigurationWrapper wrapper) {
		ParseConfiguration configuration = wrapper.getParseConfiguration();
		PosTaggedToken resultToken = null;
		FeatureResult<Integer> indexResult = indexFeature.check(configuration);
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
		FeatureResult<PosTaggedToken> featureResult = null;
		if (resultToken!=null)
			featureResult = this.generateResult(resultToken);
		return featureResult;
	}
}
