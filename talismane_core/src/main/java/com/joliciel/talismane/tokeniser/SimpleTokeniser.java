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
package com.joliciel.talismane.tokeniser;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.rawText.Sentence;

/**
 * A simplistic implementation of a Tokeniser, using only TokenFilters and
 * default decisions.
 * 
 * @author Assaf Urieli
 *
 */
public class SimpleTokeniser extends Tokeniser {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(SimpleTokeniser.class);

  public SimpleTokeniser(TalismaneSession talismaneSession) {
    super(talismaneSession);
  }

  SimpleTokeniser(SimpleTokeniser tokeniser) {
    super(tokeniser);
  }

  @Override
  protected List<TokenisedAtomicTokenSequence> tokeniseInternal(TokenSequence initialSequence, Sentence sentence) {
    List<TokenisedAtomicTokenSequence> sequences = null;

    sequences = new ArrayList<TokenisedAtomicTokenSequence>();
    TokenisedAtomicTokenSequence defaultSequence = new TokenisedAtomicTokenSequence(sentence, 0, this.getTalismaneSession());
    for (Token token : initialSequence.listWithWhiteSpace()) {
      Decision tokeniserDecision = new Decision(TokeniserOutcome.SEPARATE.name());
      TaggedToken<TokeniserOutcome> taggedToken = new TaggedToken<TokeniserOutcome>(token, tokeniserDecision,
          TokeniserOutcome.valueOf(tokeniserDecision.getOutcome()));
      defaultSequence.add(taggedToken);
    }
    sequences.add(defaultSequence);

    return sequences;
  }

  @Override
  public Tokeniser cloneTokeniser() {
    return new SimpleTokeniser(this);
  }

}
