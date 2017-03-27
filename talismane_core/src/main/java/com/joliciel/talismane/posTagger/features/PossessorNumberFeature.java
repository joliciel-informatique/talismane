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
package com.joliciel.talismane.posTagger.features;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.lexicon.LexicalAttribute;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringCollectionFeature;

/**
 * The grammatical number of the possessor of a given token as supplied by the
 * lexicon.
 * 
 * @author Assaf Urieli
 *
 */
public final class PossessorNumberFeature<T> extends AbstractLexicalAttributeFeature<T>implements StringCollectionFeature<T> {
  private final List<String> attributes = new ArrayList<>(1);

  public PossessorNumberFeature(PosTaggedTokenAddressFunction<T> addressFunction) {
    super(addressFunction);
    this.setAddressFunction(addressFunction);
    attributes.add(LexicalAttribute.PossessorNumber.toString());
  }

  @Override
  protected List<String> getAttributes(PosTaggedTokenWrapper innerWrapper, RuntimeEnvironment env) {
    return attributes;
  }
}
