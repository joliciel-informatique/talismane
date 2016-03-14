///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
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
package com.joliciel.talismane.lexicon;

import java.util.Set;

import com.joliciel.talismane.NeedsTalismaneSession;

/**
 * An interface for retrieving, for a given original word, assumed to be uppercase without diacritics, the various lowercase
 * possibilities which can contain diacritics. Useful for converting text in ALL CAPS to possible words.
 * @author Assaf Urieli
 *
 */
public interface Diacriticizer extends NeedsTalismaneSession {
	/**
	 * Given a word, will try to find equivalent lowercase words with diacritics.
	 * By equivalent we mean: for each letter in the original word, if the letter is undecorated uppercase, the equivalent
	 * letter must be a decorated or undecorated lowercase or uppercase. If the original letter is in the lowercase, it must remain identical.
	 * If the original letter is a decorated uppercase, the equivalent letter must be the decorated lowercase or uppercase.<br/>
	 * Thus, for a french glossary, "MANGE" will return "mangé" and "mange", "A" will return "à" and "a", "À" will return only "à", and "a" will return only "a".
	 * @param originalWord
	 * @return
	 */
	public Set<String> diacriticize(String originalWord);
}
