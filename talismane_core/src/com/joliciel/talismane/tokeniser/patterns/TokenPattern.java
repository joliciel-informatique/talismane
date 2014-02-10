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
import java.util.regex.Pattern;

import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * A pattern for analysing whether a particular token is likely to have a TokeniserDecision different from the default for this token.<br/>
 * When matching a TokeniserPattern, we check if any set of n tokens in a TokenSequence matches it.<br/>
 * If so, all of the tokens inside the set are to be tested further, unless they have been marked with {} as not for further testing.<br/>
 * The TokeniserPattern will always match an exact number of tokens in a TokenSequence.<br/>
 * The number of tokens <i>n</i> to be matched is calculated from the number of separators explicitly included in the TokeniserPattern.<br/>
 * For example, the pattern "parce que" and "parce qu'" need to be listed separately, as the second one has an additional separator.
 * They can be given the same name, to ensure they are handled as one statistical group.<br/>
 * This regexp on which this is built similar to a standard Pattern, but with some limits:<br/>
 * The only permissible regular expression symbols for now are: . + ( ) | [ ] ^ \d \D \s \p \b \z and any of these escaped with a backslash.<br/>
 * The \p symbol has a special meaning: any separator (punctuation or whitespace).<br/>
 * The \b symbol has a special meaning: whitespace, sentence start or sentence end.<br/>
 * The repeated wildcard .+ is always assumed to represent a single non-separating token.<br/>
 * Groups separated by the | operator must be surrounded by (). They should not contain any separators, so that the number of tokens
 * to be tested is always constant (create separate TokeniserPattern if absolutely required).<br/>
 * Groups in the [] operator must either contain only separators, or only non-separators.<br/>
 * The { } symbols have a special meaning: around set of tokens, they are taken to mean that these tokens are only there to give context, and should not be tested further to override the default.<br/>
 * The \p, \s and \b symbols are always assumed to be inside curly brackets (no further testing)
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
	 * Return a TokenPatternMatchSequence for each sequence of <i>n</i> tokens in a TokenSequence which match this pattern.
	 * Will also add any matches to Token.getMatches() for the matched tokens.
	 * @param tokenSequence
	 * @return
	 */
	public List<TokenPatternMatchSequence> match(TokenSequence tokenSequence);

	/**
	 * This token pattern's user-friendly name.
	 * Can also be used to group together two patterns whose statistical distribtuion should be identical,
	 * e.g. "parce que" and "parce qu'".
	 * @param name
	 */
	public void setName(String name);
	public String getName();
	
	/**
	 * A name for grouping together several patterns due to take advantage
	 * of their distributional similarity in features.
	 * @return
	 */
	public String getGroupName();
	public void setGroupName(String groupName);
	
	/**
	 * Is the pattern at the provided index a separator class pattern.
	 * @param index
	 * @return
	 */
	public boolean isSeparatorClass(int index);
}
