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
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * The "best" lemma of a given token as supplied by the lexicon, referenced by address.
 * @author Assaf Urieli
 *
 */
public class LemmaFeature extends AbstractPosTaggedTokenFeature<String> implements StringFeature<PosTaggedTokenWrapper> {
	
	public LemmaFeature() {
		super();
		this.setName(super.getName());
	}

	@Override
	public FeatureResult<String> checkInternal(PosTaggedTokenWrapper wrapper) {
		PosTaggedToken posTaggedToken = wrapper.getPosTaggedToken();
		if (posTaggedToken==null)
			return null;
		FeatureResult<String> featureResult = null;
		LexicalEntry lexicalEntry = null;
		if (posTaggedToken.getLexicalEntries().size()>0)
			lexicalEntry = posTaggedToken.getLexicalEntries().iterator().next();
		if (lexicalEntry!=null)
			featureResult = this.generateResult(lexicalEntry.getLemma());
		
		return featureResult;
	}
	
}
