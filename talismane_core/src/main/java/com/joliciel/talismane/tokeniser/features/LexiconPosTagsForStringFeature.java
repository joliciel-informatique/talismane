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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringCollectionFeature;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * A StringCollectionFeature returning all of the postags in the lexicon for a
 * word specified by the wordToCheckFeature.
 * 
 * @author Assaf Urieli
 *
 */
public final class LexiconPosTagsForStringFeature extends AbstractTokenFeature<List<WeightedOutcome<String>>>
    implements StringCollectionFeature<TokenWrapper>, NeedsTalismaneSession {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(LexiconPosTagsForStringFeature.class);
  private StringFeature<TokenWrapper> wordToCheckFeature;

  TalismaneSession talismaneSession;

  public LexiconPosTagsForStringFeature(StringFeature<TokenWrapper> wordToCheckFeature) {
    this.wordToCheckFeature = wordToCheckFeature;
    this.setName(super.getName() + "(" + this.wordToCheckFeature.getName() + ")");
  }

  public LexiconPosTagsForStringFeature(TokenAddressFunction<TokenWrapper> addressFunction, StringFeature<TokenWrapper> wordToCheckFeature) {
    this(wordToCheckFeature);
    this.setAddressFunction(addressFunction);
  }

  @Override
  public FeatureResult<List<WeightedOutcome<String>>> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) throws TalismaneException {
    TokenWrapper innerWrapper = this.getToken(tokenWrapper, env);
    if (innerWrapper == null)
      return null;

    FeatureResult<List<WeightedOutcome<String>>> result = null;
    FeatureResult<String> wordToCheckResult = wordToCheckFeature.check(innerWrapper, env);
    if (wordToCheckResult != null) {
      String wordToCheck = wordToCheckResult.getOutcome();
      List<WeightedOutcome<String>> resultList = new ArrayList<WeightedOutcome<String>>();
      PosTaggerLexicon lexicon = talismaneSession.getMergedLexicon();
      Set<PosTag> posTags = lexicon.findPossiblePosTags(wordToCheck);

      for (PosTag posTag : posTags) {
        resultList.add(new WeightedOutcome<String>(posTag.getCode(), 1.0));
      }

      if (resultList.size() > 0)
        result = this.generateResult(resultList);
    }

    return result;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Class<? extends Feature> getFeatureType() {
    return StringCollectionFeature.class;
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
