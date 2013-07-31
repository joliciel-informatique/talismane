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
package com.joliciel.talismane.tokeniser.patterns;

import java.util.List;
import java.util.regex.Pattern;

import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * A pattern for analysing whether a particular token is likely to have a TokeniserDecision different from the default for this token.<br/>
 * When matching a TokeniserPattern, we check if any set of n tokens in a TokenSequence matches it.<br/>
 * If so, all of the separators inside the set are to be tested further, unless they have been marked with {} as not for further testing.<br/>
 * The TokeniserPattern will always match an exact number of tokens in a TokenSequence.<br/>
 * The number of tokens "n" to be matched is calculated from the number of separators explicitly included in the TokeniserPattern.<br/>
 * This regexp on which this is built similar to a standard Pattern, but with some limits:<br/>
 * The only permissible regular expression symbols for now are: . + ( ) | [ ] { } ^ \d \D \s \p \z and any of these escaped with a backslash.<br/>
 * The \p symbol has a special meaning: any separator (punctuation or whitespace).<br/>
 * The repeated wildcard .+ is always assumed to represent a single non-separating token.<br/>
 * Groups separated by the | operator must be surrounded by (). They should not contain any separators, so that the number of tokens
 * to be tested is always constant (create separate TokeniserPattern if absolutely required).<br/>
 * Groups in the [] operator must either contain only separators, or only non-separators.<br/>
 * The { } symbols have a special meaning: around a separator, they are taken to mean that this separator should not be tested further
 * (it is only there to test for pattern matching).<br/>
 * Note that if a pattern ends with a wildcard separator (\s or \p), the wildcard at the end WILL be used for matching, but the matching
 * separator will NOT be included officially in the pattern.<br/>
 * @author Assaf Urieli
 *
 */
public interface TokenPattern {
	/**
	 * The regular expression on which this TokeniserPattern was built.
	 * @return
	 */
	public String getRegExp();
	
	/**
	 * The original pattern, broken up into chunks where each chunk should match exactly one token in the token sequence.
	 * Each chunk is a standard Pattern, but may or may not be interpreted as such (e.g. .+ will only match non-separating tokens).
	 * @return
	 */
	public List<Pattern> getParsedPattern();
	
	/**
	 * The number of tokens that will be matched by this pattern in the TokenSequence.
	 * @return
	 */
	public int getTokenCount();
	
	/**
	 * The indexes in getParsedPattern corresponding to tokens that need to be examined further,
	 * to decide if they represent a token break or not.
	 * These will typically correspond to all separators.
	 * @return
	 */
	public List<Integer> getIndexesToTest();
	
	/**
	 * For each sequence of "n" tokens in a TokenSequence which match this pattern,
	 * add a List of tokens in the matching sequence which should be tested further.
	 * Will also add any matches to Token.getMatches() for the matched tokens.
	 * @param tokenSequence
	 * @return
	 */
	public List<TokenPatternMatchSequence> match(TokenSequence tokenSequence);

	/**
	 * This tokeniser pattern's user-friendly name.
	 * @param name
	 */
	public void setName(String name);
	public String getName();
}
