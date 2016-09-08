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
package com.joliciel.talismane.posTagger.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.posTagger.PosTagSequence;

/**
 * Goes through a pos-tag sequence and removes any pos-tagged tokens which 
 * are empty (startIndex==endIndex) and have a null pos-tag.
 * @author Assaf Urieli
 *
 */
public class RemoveNullEmptyTokensFilter implements PosTagSequenceFilter {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(RemoveNullEmptyTokensFilter.class);

	@Override
	public void apply(PosTagSequence posTagSequence) {
		posTagSequence.removeEmptyPosTaggedTokens();
	}

}
