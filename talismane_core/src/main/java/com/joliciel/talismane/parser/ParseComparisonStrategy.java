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

import com.joliciel.talismane.parser.Parser.ParseComparisonStrategyType;

/**
 * The strategy for deciding which parses need to be compared with each other.
 * 
 * @author Assaf Urieli
 *
 */
public interface ParseComparisonStrategy {
  public static ParseComparisonStrategy forType(ParseComparisonStrategyType type) {
    switch (type) {
    case transitionCount:
      return new TransitionCountComparisonStrategy();
    case bufferSize:
      return new BufferSizeComparisonStrategy();
    case stackAndBufferSize:
      return new StackAndBufferSizeComparsionStrategy();
    case dependencyCount:
      return new DependencyCountComparisonStrategy();
    }
    throw new RuntimeException("Unknown parse comparison strategy: " + type);
  }

  public int getComparisonIndex(ParseConfiguration configuration);
}
