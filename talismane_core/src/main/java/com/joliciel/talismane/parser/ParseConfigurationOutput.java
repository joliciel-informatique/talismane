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
package com.joliciel.talismane.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.tokeniser.Token;

public final class ParseConfigurationOutput extends ArrayList<ParseConfigurationTokenOutput> {

  /**
   * 
   */
  private static final long serialVersionUID = 8874423911960774024L;

  public ParseConfigurationOutput(ParseConfiguration parseConfiguration) {
    Map<Token, DependencyArc> tokenDependencyMap = new HashMap<Token, DependencyArc>();
    for (DependencyArc dependencyArc : parseConfiguration.getDependencies()) {
      tokenDependencyMap.put(dependencyArc.getDependent().getToken(), dependencyArc);
    }

    Map<Token, DependencyArc> tokenDependencyMapNonProj = new HashMap<Token, DependencyArc>();
    for (DependencyArc dependencyArc : parseConfiguration.getNonProjectiveDependencies()) {
      tokenDependencyMapNonProj.put(dependencyArc.getDependent().getToken(), dependencyArc);
    }

    Map<Token, ParseConfigurationTokenOutput> tokenOutputMap = new HashMap<Token, ParseConfigurationTokenOutput>();

    for (PosTaggedToken posTaggedToken : parseConfiguration.getPosTagSequence()) {
      ParseConfigurationTokenOutput unit = new ParseConfigurationTokenOutput(posTaggedToken);
      tokenOutputMap.put(posTaggedToken.getToken(), unit);
      this.add(unit);
    }

    for (ParseConfigurationTokenOutput unit : this) {
      DependencyArc arc = tokenDependencyMap.get(unit.getToken());
      if (arc != null) {
        ParseConfigurationTokenOutput governorOutput = tokenOutputMap.get(arc.getHead().getToken());
        unit.setArc(arc);
        unit.setGovernor(governorOutput);
        unit.setLabel(arc.getLabel());
      }

      DependencyArc nonProjectiveArc = tokenDependencyMapNonProj.get(unit.getToken());
      if (nonProjectiveArc != null) {
        ParseConfigurationTokenOutput governorOutput = tokenOutputMap.get(nonProjectiveArc.getHead().getToken());
        unit.setNonProjectiveArc(nonProjectiveArc);
        unit.setNonProjectiveGovernor(governorOutput);
        unit.setNonProjectiveLabel(nonProjectiveArc.getLabel());
      }
    }
  }
}
