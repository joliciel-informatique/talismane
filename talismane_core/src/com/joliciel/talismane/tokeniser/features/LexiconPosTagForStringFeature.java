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


import java.util.Set;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.posTagger.PosTag;

/**
 * Returns true if the string provided has a lexicon entry for the PosTag provided.
 * @author Assaf Urieli
 *
 */
public final class LexiconPosTagForStringFeature extends AbstractTokenFeature<Boolean> implements BooleanFeature<TokenWrapper> {
	private StringFeature<TokenWrapper> posTagFeature;
	private StringFeature<TokenWrapper> wordToCheckFeature;
	
	/**
	 * 
	 * @param posTag the PosTag we're testing for
	 */
	public LexiconPosTagForStringFeature(StringFeature<TokenWrapper> wordToCheckFeature, StringFeature<TokenWrapper> posTagFeature) {
		this.posTagFeature = posTagFeature;
		this.wordToCheckFeature = wordToCheckFeature;
		this.setName(super.getName() + "(" + this.wordToCheckFeature.getName() + "," + this.posTagFeature.getName() + ")");
	}
	
	public LexiconPosTagForStringFeature(TokenAddressFunction<TokenWrapper> addressFunction, StringFeature<TokenWrapper> wordToCheckFeature, StringFeature<TokenWrapper> posTagFeature) {
		this(wordToCheckFeature, posTagFeature);
		this.setAddressFunction(addressFunction);
	}
	
	@Override
	public FeatureResult<Boolean> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) {
		TokenWrapper innerWrapper = this.getToken(tokenWrapper, env);
		if (innerWrapper==null)
			return null;

		FeatureResult<Boolean> result = null;

		FeatureResult<String> wordToCheckResult = wordToCheckFeature.check(innerWrapper, env);
		if (wordToCheckResult!=null) {
			FeatureResult<String> posTagResult = posTagFeature.check(innerWrapper, env);
			if (posTagResult!=null) {
				PosTag posTag = TalismaneSession.getPosTagSet().getPosTag(posTagResult.getOutcome());
				String wordToCheck = wordToCheckResult.getOutcome();
				PosTaggerLexicon lexicon = TalismaneSession.getLexicon();
				Set<PosTag> posTags = lexicon.findPossiblePosTags(wordToCheck);
				boolean hasPosTag = (posTags.contains(posTag));
				result = this.generateResult(hasPosTag);
			}
		}
		
		return result;
	}

}
