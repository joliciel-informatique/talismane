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
package com.joliciel.talismane.languageDetector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.joliciel.talismane.TalismaneException;

/**
 * A factory for creating language detector features from descriptors.
 * 
 * @author Assaf Urieli
 *
 */
public class LanguageDetectorFeatureFactory {
  /**
   * Transform a set of descriptors into a set of features.
   * 
   * @param featureDescriptors
   * @return
   * @throws TalismaneException
   *           if an unknown feature descriptor is encountered
   */
  public Set<LanguageDetectorFeature<?>> getFeatureSet(List<String> featureDescriptors) throws TalismaneException {
    Set<LanguageDetectorFeature<?>> features = new HashSet<LanguageDetectorFeature<?>>();
    for (String descriptor : featureDescriptors) {
      if (descriptor.startsWith("CharNgram")) {
        int n = Integer.parseInt(descriptor.substring(descriptor.indexOf('(') + 1, descriptor.lastIndexOf(')')));
        CharacterNgramFeature charNgramFeature = new CharacterNgramFeature(n);
        features.add(charNgramFeature);
      } else {
        throw new TalismaneException("Unknown language feature descriptor: " + descriptor);
      }
    }
    return features;
  }
}
