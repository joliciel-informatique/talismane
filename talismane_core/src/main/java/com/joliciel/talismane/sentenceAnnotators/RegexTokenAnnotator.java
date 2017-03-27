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
package com.joliciel.talismane.sentenceAnnotators;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.utils.RegexUtils;

/**
 * A regex annotator which tokenises the matched group as a single separate
 * token, and offers the possibility of a replacement string to replace the
 * token text.<br/>
 * <br/>
 * In addition to the parameters recognised by {@link AbstractRegexAnnotator},
 * recognises:
 * <ul>
 * <li>replacement: see {@link #getReplacement()}.</li>
 * <li>analysisText: see {@link #getAnalysisText()}.</li>
 * </ul>
 * <br/>
 * 
 * @author Assaf Urieli
 *
 */
public class RegexTokenAnnotator extends AbstractRegexAnnotator {
  private final String replacement;
  private final String analysisText;
  private static final Set<String> handledParameters = new HashSet<>(Arrays.asList("replacement", "analysisText"));

  /**
   * Assigns default groupIndex=0, caseSensitive=true, diacricticSensitive=true,
   * autoWordBoundaries=false.
   */
  public RegexTokenAnnotator(String regex, String replacement, String analysisText, TalismaneSession talismaneSession) throws SentenceAnnotatorLoadException {
    this(regex, replacement, analysisText, 0, true, true, false, talismaneSession);
  }

  public RegexTokenAnnotator(String regex, String replacement, String analysisText, int groupIndex, boolean caseSensitive, boolean diacricticSensitive,
      boolean autoWordBoundaries, TalismaneSession talismaneSession) throws SentenceAnnotatorLoadException {
    super(regex, groupIndex, caseSensitive, diacricticSensitive, autoWordBoundaries, true, talismaneSession);
    this.replacement = replacement;
    this.analysisText = analysisText;
  }

  public RegexTokenAnnotator(String descriptor, Map<String, String> defaultParams, TalismaneSession talismaneSession) throws SentenceAnnotatorLoadException {
    super(descriptor, defaultParams, handledParameters, true, talismaneSession);
    Map<String, String> parameters = this.getParameters();

    String replacement = null;
    String analysisText = null;

    for (String paramName : parameters.keySet()) {
      String paramValue = parameters.get(paramName);

      if (paramName.equals("replacement")) {
        replacement = paramValue;
      } else if (paramName.equals("analysisText")) {
        analysisText = paramValue;
      }
    }

    this.replacement = replacement;
    this.analysisText = analysisText;
  }

  /**
   * The replacement string to use for this token. See
   * {@link RegexUtils#getReplacement(String, CharSequence, Matcher)} for
   * handling of the replacement string.
   * 
   * @return
   */
  public String getReplacement() {
    return replacement;
  }

  /**
   * The analysis text to use for this token. See {@link Token#getAnalyisText()}
   * .
   * 
   * @return
   */
  public String getAnalysisText() {
    return analysisText;
  }

  @Override
  protected String findReplacement(CharSequence text, Matcher matcher) {
    String newText = RegexUtils.getReplacement(replacement, text, matcher);
    return newText;
  }

}
