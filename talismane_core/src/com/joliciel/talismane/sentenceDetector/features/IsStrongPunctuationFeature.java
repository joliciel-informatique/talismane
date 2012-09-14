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
package com.joliciel.talismane.sentenceDetector.features;

import com.joliciel.talismane.sentenceDetector.PossibleSentenceBoundary;
import com.joliciel.talismane.utils.features.BooleanFeature;
import com.joliciel.talismane.utils.features.FeatureResult;

/**
 * Returns true if the current boundar is ".", "?" or "!". Returns false otherwise.
 * @author Assaf
 *
 */
public class IsStrongPunctuationFeature extends AbstractSentenceDetectorFeature<Boolean> implements BooleanFeature<PossibleSentenceBoundary> {
	@Override
	public FeatureResult<Boolean> checkInternal(PossibleSentenceBoundary context) {
		char boundary = context.getText().charAt(context.getIndex());
		boolean isStrong = (boundary=='.'||boundary=='!'||boundary=='?');
		return this.generateResult(isStrong);
	}

}
