///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Joliciel Informatique
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
package com.joliciel.talismane.sentenceAnnotators;

import java.util.Collections;
import java.util.Map;

import com.joliciel.talismane.TalismaneSession;

/**
 * A Regex annotator which only adds attributes to any tokens entirely contained
 * by the match.
 * 
 * @author Assaf Urieli
 *
 */
public class RegexAttributeAnnotator extends AbstractRegexAnnotator {
  /**
   * Assigns default groupIndex=0, caseSensitive=true, diacricticSensitive=true,
   * autoWordBoundaries=false.
   * 
   * @param regex
   * @throws SentenceAnnotatorLoadException
   */
  public RegexAttributeAnnotator(String regex, String sessionId) throws SentenceAnnotatorLoadException {
    this(regex, 0, true, true, false, sessionId);
  }

  public RegexAttributeAnnotator(String regex, int groupIndex, boolean caseSensitive, boolean diacricticSensitive, boolean autoWordBoundaries,
                                 String sessionId) throws SentenceAnnotatorLoadException {
    super(regex, groupIndex, caseSensitive, diacricticSensitive, autoWordBoundaries, false, sessionId);
  }

  public RegexAttributeAnnotator(String descriptor, Map<String, String> defaultParams, String sessionId)
      throws SentenceAnnotatorLoadException {
    super(descriptor, defaultParams, Collections.emptySet(), false, sessionId);
  }

}
