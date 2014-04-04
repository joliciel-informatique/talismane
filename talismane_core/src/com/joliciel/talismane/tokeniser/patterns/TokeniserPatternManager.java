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
package com.joliciel.talismane.tokeniser.patterns;

import java.util.List;
import java.util.Map;

import com.joliciel.talismane.tokeniser.SeparatorDecision;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;

/**
 * The TokeniserPatternManager will read patterns from a list of strings,
 * typically stored in a file.<br/>
 * <br/>
 * The list of strings should contain default decisions for the various separators, in lines as follows:<br/>
 * IS_NOT_SEPARATOR -<br/>
 * IS_SEPARATOR_AFTER '<br/>
 * IS_SEPARATOR_BEFORE<br/>
 * All other separators are assumed to separate tokens on both sides (IS_SEPARATOR)<br/>
 * <br/>
 * Next, it should contain a list of patterns.<br/>
 * Optionally, each pattern can be preceded by a user-friendly name and a tab.<br/>
 * Patterns are used to check if any set of n atomic tokens in a sentence matches it. If so, all of the separators inside the set are tested further.<br/>
 * More information on patterns can be found in the {@link TokenPattern} class.<br/>
 * 
 * @author Assaf Urieli
 *
 */
public interface TokeniserPatternManager {
	/**
	 * For each type of separator decision, a list of separators (e.g. punctuation marks) which will receive
	 * this decision by default.
	 * @return
	 */
	public abstract Map<SeparatorDecision, String> getSeparatorDefaults();
	
	/**
	 * The test patterns - only token sequences matching these patterns will
	 * be submitted to further decision.
	 * @return
	 */
	public abstract List<String> getTestPatterns();
	
	/**
	 * Test patterns after parsing.
	 * @return
	 */
	public List<TokenPattern> getParsedTestPatterns();
	
	/**
	 * Takes a sequence of atomic tokens and applies default decisions for each separator.
	 * @param tokenSequence
	 * @return
	 */
	public List<TokeniserOutcome> getDefaultOutcomes(TokenSequence tokenSequence);
	
	/**
	 * The full list of descriptors used to construct this pattern manager.
	 * @return
	 */
	public List<String> getDescriptors();
}