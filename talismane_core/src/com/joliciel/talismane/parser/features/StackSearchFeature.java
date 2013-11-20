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

import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;

/**
 * Looks at all items on the stack above a certain index,
 * and returns the first one meeting a criterion.
 * If no index is provided, will start at Stack[1] (just one behind the top-of-stack)
 * @author Assaf Urieli
 *
 */
public final class StackSearchFeature extends AbstractAddressFunction {
	private IntegerFeature<ParseConfigurationWrapper> indexFeature;
	private BooleanFeature<PosTaggedTokenWrapper> criterionFeature;
	
	public StackSearchFeature(BooleanFeature<PosTaggedTokenWrapper> criterionFeature) {
		super();
		this.criterionFeature = criterionFeature;
		
		this.setName(super.getName() +"(" + criterionFeature.getName() + ")");
	}

	public StackSearchFeature(IntegerFeature<ParseConfigurationWrapper> indexFeature, BooleanFeature<PosTaggedTokenWrapper> criterionFeature) {
		super();
		this.indexFeature = indexFeature;
		this.criterionFeature = criterionFeature;
		
		this.setName(super.getName() +"(" + indexFeature.getName() + "," + criterionFeature.getName() + ")");
	}

	@Override
	public FeatureResult<PosTaggedTokenWrapper> checkInternal(ParseConfigurationWrapper wrapper, RuntimeEnvironment env) {
		ParseConfiguration configuration = wrapper.getParseConfiguration();

		int index=1;
		if (indexFeature!=null) {
			FeatureResult<Integer> indexResult = indexFeature.check(wrapper, env);
			if (indexResult==null)
				return null;
			index = indexResult.getOutcome();
		}
		
		Iterator<PosTaggedToken> stackIterator = configuration.getStack().iterator();
		
		ParseConfigurationAddress parseConfigurationAddress = new ParseConfigurationAddress(env);
		parseConfigurationAddress.setParseConfiguration(configuration);

		int i=-1;
		PosTaggedToken resultToken = null;
		while (stackIterator.hasNext()) {
			PosTaggedToken token = stackIterator.next();
			i++;
			if (i<index)
				continue;
			parseConfigurationAddress.setPosTaggedToken(token);
			FeatureResult<Boolean> criterionResult = criterionFeature.check(parseConfigurationAddress, env);
			if (criterionResult!=null) {
				boolean criterion = criterionResult.getOutcome();
				if (criterion) {
					resultToken = token;
					break;
				}
			}
		}

		FeatureResult<PosTaggedTokenWrapper> featureResult = null;
		if (resultToken!=null)
			featureResult = this.generateResult(resultToken);
		return featureResult;
	}
}
