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
package com.joliciel.talismane.posTagger;

import java.util.Set;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.machineLearning.features.HasFeatureCache;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;
import com.joliciel.talismane.tokeniser.TaggedToken;

/**
 * A token with a postag tagged onto it.
 * @author Assaf Urieli
 *
 */
public interface PosTaggedToken extends TaggedToken<PosTag>, PosTaggedTokenWrapper, HasFeatureCache {
	/**
	 * The "best" lexical entry if one exists, or null otherwise.
	 */
	public LexicalEntry getLexicalEntry();
	public Set<LexicalEntry> getLexicalEntries();
}
