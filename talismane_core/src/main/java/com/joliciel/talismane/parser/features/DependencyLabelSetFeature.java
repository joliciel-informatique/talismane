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

import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.features.AbstractStringCollectionFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * A StringCollectionFeature returning all of the dependency labels in the
 * current transition system.
 * 
 * @author Assaf Urieli
 *
 */
public final class DependencyLabelSetFeature extends AbstractStringCollectionFeature<ParseConfigurationWrapper>implements NeedsTalismaneSession {
  private TalismaneSession talismaneSession;

  @Override
  public FeatureResult<List<WeightedOutcome<String>>> checkInternal(ParseConfigurationWrapper context, RuntimeEnvironment env) {
    TransitionSystem transitionSystem = talismaneSession.getTransitionSystem();
    List<WeightedOutcome<String>> resultList = new ArrayList<WeightedOutcome<String>>();
    for (String label : transitionSystem.getDependencyLabelSet().getDependencyLabels()) {
      resultList.add(new WeightedOutcome<String>(label, 1.0));
    }
    return this.generateResult(resultList);
  }

  @Override
  public TalismaneSession getTalismaneSession() {
    return talismaneSession;
  }

  @Override
  public void setTalismaneSession(TalismaneSession talismaneSession) {
    this.talismaneSession = talismaneSession;
  }

}
