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

import java.util.List;

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
	 * The "best" lexical entry for this token/postag combination if one exists, or null otherwise.
	 */
	public LexicalEntry getLexicalEntry();
	
	/**
	 * All lexical entries for this token/postag combination.
	 * @return
	 */
	public List<LexicalEntry> getLexicalEntries();
	public void setLexicalEntries(List<LexicalEntry> lexicalEntries);
	
	public PosTaggedToken clonePosTaggedToken();
	
	/**
	 * The lemma of the "best" lexical entry as encoded for the CoNLL output format.
	 * @return
	 */
	public String getLemmaForCoNLL();
}
