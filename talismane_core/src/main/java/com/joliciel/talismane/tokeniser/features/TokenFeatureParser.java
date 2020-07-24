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
package com.joliciel.talismane.tokeniser.features;

import com.joliciel.talismane.NeedsSessionId;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureClassContainer;

/**
 * A helper class for adding generic token features to any parser requiring
 * them. See {@link #addFeatureClasses(FeatureClassContainer)}.
 * 
 * @author Assaf Urieli
 *
 */
public class TokenFeatureParser {

  /**
   * Add token feature classes to the container provided, including:<br/>
   * - AndRange: {@link AndRangeFeature}<br/>
   * - BackwardSearch: {@link BackwardSearchFeature}<br/>
   * - CountIf: {@link CountIfFeature}<br/>
   * - FirstWordInCompound: {@link FirstWordInCompoundFeature}<br/>
   * - FirstWordInSentence: {@link FirstWordInSentenceFeature}<br/>
   * - ForwardSearch: {@link ForwardSearchFeature}<br/>
   * - Has: {@link HasFeature}<br/>
   * - HasClosedClassesOnly: {@link HasClosedClassesOnlyFeature}<br/>
   * - IfThenElse: {@link IfThenElseTokenAddressFeature}<br/>
   * - LastWordInCompound: {@link LastWordInCompoundFeature}<br/>
   * - LastWordInSentence: {@link LastWordInSentenceFeature}<br/>
   * - LemmaForPosTag: {@link LemmaForPosTagFeature}<br/>
   * - LexiconAllPosTags: {@link LexiconAllPosTagsFeature}<br/>
   * - LexiconPosTag: {@link LexiconPosTagFeature}<br/>
   * - LexiconPosTagForString: {@link LexiconPosTagForStringFeature}<br/>
   * - LexiconPosTags: {@link LexiconPosTagsFeature}<br/>
   * - LexiconPosTagsForString: {@link LexiconPosTagsForStringFeature}<br/>
   * - NLetterPrefix: {@link NLetterPrefixFeature}<br/>
   * - NLetterSuffix: {@link NLetterSuffixFeature}<br/>
   * - Offset: {@link TokenOffsetAddressFunction}<br/>
   * - OrRange: {@link OrRangeFeature}<br/>
   * - PosTagSet: {@link PosTagSetFeature}<br/>
   * - Regex: {@link RegexFeature}<br/>
   * - TokenAt: {@link TokenAtAddressFunction}<br/>
   * - TokenIndex: {@link TokenIndexFeature}<br/>
   * - UnknownWord: {@link UnknownWordFeature}<br/>
   * - Word: {@link WordFeature}<br/>
   * - WordForm: {@link TokenWordFormFeature}<br/>
   */
  public static void addFeatureClasses(FeatureClassContainer container) {
    container.addFeatureClass("AndRange", AndRangeFeature.class);
    container.addFeatureClass("BackwardSearch", BackwardSearchFeature.class);
    container.addFeatureClass("CountIf", CountIfFeature.class);
    container.addFeatureClass("FirstWordInCompound", FirstWordInCompoundFeature.class);
    container.addFeatureClass("FirstWordInSentence", FirstWordInSentenceFeature.class);
    container.addFeatureClass("ForwardSearch", ForwardSearchFeature.class);
    container.addFeatureClass("Has", HasFeature.class);
    container.addFeatureClass("HasClosedClassesOnly", HasClosedClassesOnlyFeature.class);
    container.addFeatureClass("IfThenElse", IfThenElseTokenAddressFeature.class);
    container.addFeatureClass("LastWordInCompound", LastWordInCompoundFeature.class);
    container.addFeatureClass("LastWordInSentence", LastWordInSentenceFeature.class);
    container.addFeatureClass("LemmaForPosTag", LemmaForPosTagFeature.class);
    container.addFeatureClass("LexiconAllPosTags", LexiconAllPosTagsFeature.class);
    container.addFeatureClass("LexiconPosTag", LexiconPosTagFeature.class);
    container.addFeatureClass("LexiconPosTagForString", LexiconPosTagForStringFeature.class);
    container.addFeatureClass("LexiconPosTags", LexiconPosTagsFeature.class);
    container.addFeatureClass("LexiconPosTagsForString", LexiconPosTagsForStringFeature.class);
    container.addFeatureClass("NLetterPrefix", NLetterPrefixFeature.class);
    container.addFeatureClass("NLetterSuffix", NLetterSuffixFeature.class);
    container.addFeatureClass("Offset", TokenOffsetAddressFunction.class);
    container.addFeatureClass("OrRange", OrRangeFeature.class);
    container.addFeatureClass("PosTagSet", PosTagSetFeature.class);
    container.addFeatureClass("Regex", RegexFeature.class);
    container.addFeatureClass("TokenAt", TokenAtAddressFunction.class);
    container.addFeatureClass("TokenIndex", TokenIndexFeature.class);
    container.addFeatureClass("UnknownWord", UnknownWordFeature.class);
    container.addFeatureClass("Word", WordFeature.class);
    container.addFeatureClass("WordForm", TokenWordFormFeature.class);
  }

  public static void injectDependencies(@SuppressWarnings("rawtypes") Feature feature, String sessionId) {
    if (feature instanceof NeedsSessionId) {
      ((NeedsSessionId) feature).setSessionId(sessionId);
    }
  }

}
