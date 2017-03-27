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

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.tokeniser.Token;

/**
 * The "best" lemma of a given token and postag (or set of postags), as supplied
 * by the lexicon.
 * 
 * @author Assaf Urieli
 *
 */
public final class LemmaForPosTagFeature extends AbstractTokenFeature<String>implements StringFeature<TokenWrapper>, NeedsTalismaneSession {
  StringFeature<TokenWrapper>[] posTagCodeFeatures;

  TalismaneSession talismaneSession;

  @SafeVarargs
  public LemmaForPosTagFeature(StringFeature<TokenWrapper>... posTagCodeFeatures) {
    super();
    this.posTagCodeFeatures = posTagCodeFeatures;
    String name = super.getName() + "(";
    boolean firstFeature = true;
    for (StringFeature<TokenWrapper> posTagCodeFeature : posTagCodeFeatures) {
      if (!firstFeature)
        name += ",";
      name += posTagCodeFeature.getName();
      firstFeature = false;
    }
    name += ")";
    this.setName(name);
  }

  @SafeVarargs
  public LemmaForPosTagFeature(TokenAddressFunction<TokenWrapper> addressFunction, StringFeature<TokenWrapper>... posTagCodeFeatures) {
    this(posTagCodeFeatures);
    this.setAddressFunction(addressFunction);
  }

  @Override
  public FeatureResult<String> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) throws TalismaneException {
    TokenWrapper innerWrapper = this.getToken(tokenWrapper, env);
    if (innerWrapper == null)
      return null;
    Token token = innerWrapper.getToken();
    FeatureResult<String> featureResult = null;

    List<String> posTagCodes = new ArrayList<String>();
    for (StringFeature<TokenWrapper> posTagCodeFeature : posTagCodeFeatures) {
      FeatureResult<String> posTagCodeResult = posTagCodeFeature.check(innerWrapper, env);
      if (posTagCodeResult != null)
        posTagCodes.add(posTagCodeResult.getOutcome());
    }

    for (String posTagCode : posTagCodes) {
      PosTag posTag = talismaneSession.getPosTagSet().getPosTag(posTagCode);

      LexicalEntry lexicalEntry = token.getLexicalEntry(posTag);
      if (lexicalEntry != null) {
        featureResult = this.generateResult(lexicalEntry.getLemma());
        break;
      }
    }

    return featureResult;
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
