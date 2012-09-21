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
package com.joliciel.talismane.tokeniser.features;

import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.tokeniser.TokenWrapper;

/**
 * A feature which tests a Token, ignoring any context (e.g. the history of current tagging decisions by a learner).
 * Because token features may be re-used during tokenising and later steps,
 * they should make sure to continue to function if the token sequence has been altered by the tokeniser.
 * This implies, if the feature refers to a token other than the current token, that we get the result directly
 * from that token rather than storing it against the current token.
 * @author Assaf Urieli
 *
 */
public interface TokenFeature<Y> extends Feature<TokenWrapper,Y> {
	/**
	 * Returns true if the feature in question needs to be rechecked each time
	 * in case the other tokens in the sequence have potentially changed.
	 * Returns false if the feature in question only depends on the current token.
	 * @return
	 */
	public boolean isDynamic();
}
