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

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;

/**
 * Retrieves the nth item from the buffer.
 * 
 * @author Assaf Urieli
 *
 */
public final class AddressFunctionBuffer extends AbstractAddressFunction {
  private IntegerFeature<ParseConfigurationWrapper> indexFeature;

  public AddressFunctionBuffer(IntegerFeature<ParseConfigurationWrapper> indexFeature) {
    super();
    this.indexFeature = indexFeature;
    this.setName("Buffer[" + indexFeature.getName() + "]");
  }

  @Override
  public FeatureResult<PosTaggedTokenWrapper> check(ParseConfigurationWrapper wrapper, RuntimeEnvironment env) throws TalismaneException {
    ParseConfiguration configuration = wrapper.getParseConfiguration();
    PosTaggedToken resultToken = null;
    FeatureResult<Integer> indexResult = indexFeature.check(configuration, env);
    if (indexResult != null) {
      int index = indexResult.getOutcome();

      Iterator<PosTaggedToken> bufferIterator = configuration.getBuffer().iterator();
      for (int i = 0; i <= index; i++) {
        if (!bufferIterator.hasNext()) {
          resultToken = null;
          break;
        }
        resultToken = bufferIterator.next();
      }
    }
    FeatureResult<PosTaggedTokenWrapper> featureResult = null;
    if (resultToken != null)
      featureResult = this.generateResult(resultToken);
    return featureResult;
  }
}
