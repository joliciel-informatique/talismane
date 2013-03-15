///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * The grammatical person of a given token as supplied by the lexicon.
 * @author Assaf Urieli
 *
 */
public class GrammaticalPersonFeature extends AbstractPosTaggedTokenFeature<String> implements StringFeature<PosTaggedTokenWrapper> {
	public GrammaticalPersonFeature() {
		super();
		this.setName(super.getName());
	}

	@Override
	public FeatureResult<String> checkInternal(PosTaggedTokenWrapper wrapper, RuntimeEnvironment env) {
		PosTaggedToken posTaggedToken = wrapper.getPosTaggedToken();
		if (posTaggedToken==null)
			return null;
		FeatureResult<String> featureResult = null;
		LexicalEntry lexicalEntry = null;
		if (posTaggedToken.getLexicalEntries().size()>0)
			lexicalEntry = posTaggedToken.getLexicalEntries().iterator().next();
		if (lexicalEntry!=null) {
			String person = "";
			for (String onePerson : lexicalEntry.getPerson()) {
				person += onePerson;
			}
			if (person.length()>0)
				featureResult = this.generateResult(person);
		}
		return featureResult;
	}
	
}
