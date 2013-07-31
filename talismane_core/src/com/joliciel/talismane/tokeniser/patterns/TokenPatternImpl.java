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
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

class TokenPatternImpl implements TokenPattern {
	private static final Log LOG = LogFactory.getLog(TokenPatternImpl.class);
	private String regexp;
	private Pattern separatorPattern = null;
	private List<Pattern> parsedPattern = null;
	private List<Integer> indexesToTest = null;
	private String name;
	private String groupName;
	
	private TokeniserPatternServiceInternal tokeniserPatternServiceInternal;

	public TokenPatternImpl(String regexp, Pattern separatorPattern) {
		this.regexp = regexp;
		this.separatorPattern = separatorPattern;
	}
	
	@Override
	public String getRegExp() {
		return this.regexp;
	}

	@Override
	public List<Pattern> getParsedPattern() {
		if (this.parsedPattern==null) {
			this.parsedPattern = this.parsePattern(this.regexp);
		}
		return this.parsedPattern;
	}

	@Override
	public int getTokenCount() {
		List<Pattern> parsedPattern = this.getParsedPattern();
		return parsedPattern.size();
	}

	@Override
	public List<Integer> getIndexesToTest() {
		this.getParsedPattern();
		return this.indexesToTest;
	}

	@Override
	public List<TokenPatternMatchSequence> match(TokenSequence tokenSequence) {
		List<TokenPatternMatchSequence> matchingSequences = new ArrayList<TokenPatternMatchSequence>();
		int t0 = 0;
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			boolean haveMatch = false;
			List<Token> matchingSequence = new ArrayList<Token>();
			// does the current token match the beginning of the pattern?
			if (checkTokenForMatch(this.getParsedPattern().get(0), token)) {
				// potential match, let's follow it through
				haveMatch = true;

				// we match so far, add it to the temp list
				matchingSequence.add(token);

				int p = 1;
				int t1 = t0+1;
				while (p<this.getParsedPattern().size() && t1<tokenSequence.listWithWhiteSpace().size()) {
					Token aToken = tokenSequence.listWithWhiteSpace().get(t1);
					if (checkTokenForMatch(this.getParsedPattern().get(p), aToken)) {
						// we match so far, add it to the temp list
						matchingSequence.add(aToken);

						p++;
					} else {
						// pattern doesn't match
						haveMatch = false;
						break;
					}
					t1++;
				} // next token and parsed pattern
				// Did we get a full match (or did we hit the end of the sentence first)
				if (matchingSequence.size()!=this.getParsedPattern().size())
					haveMatch = false;
			} // 2nd token had a match
			if (haveMatch) {
				Pattern lastPattern = this.getParsedPattern().get(this.getParsedPattern().size()-1);
				if (lastPattern.pattern().equals("\\s")||lastPattern.pattern().equals(this.separatorPattern.pattern())) {
					matchingSequence.remove(matchingSequence.size()-1);
				}
				TokenPatternMatchSequence tokenPatternMatchSequence = this.tokeniserPatternServiceInternal.getTokenPatternMatchSequence(this, matchingSequence);
				matchingSequences.add(tokenPatternMatchSequence);
				for (Token aToken : matchingSequence) {
					tokenPatternMatchSequence.addMatch(aToken);
				}
			}
			t0++;
		} // next token
		
		if (LOG.isTraceEnabled()) {
			if (matchingSequences.size()>0)
				LOG.trace(this.getName() + ": matchingSequences = " + matchingSequences);
		}
		return matchingSequences;
	}
	
	/**
	 * Does this particular token match this particular pattern.
	 * @param pattern
	 * @param token
	 * @return
	 */
	boolean checkTokenForMatch(Pattern pattern, Token token) {
		String regex = pattern.pattern();
		if (regex.contains(".+")||regex.contains("\\D")) {
			// this pattern is only allowed for non-separators
			if (!token.isSeparator())
				return pattern.matcher(token.getText()).matches();
			else
				return false;
		} else if (!this.separatorPattern.matcher(regex).find()) {
			// no separators, we simply check for string equality
			return (regex.equals(token.getText()));
		} else if (regex.startsWith("\\")
				&& !(regex.startsWith("\\d"))
				&& !(regex.startsWith("\\s"))
				&& !(regex.startsWith("\\p"))
				) {
			// an escaped separator
			return (regex.substring(1).equals(token.getText()));
		} else if (regex.length()==1) {
			// an unescaped separator
			return (regex.equals(token.getText()));
		} else {
			// a true pattern
			return (pattern.matcher(token.getText()).matches());
		}
	}

	/**
	 * Break the regexp up into chunks, where each chunk will match one token.
	 * @param regexp
	 * @return
	 */
	List<Pattern> parsePattern(String regexp) {
		boolean inLiteral = false;
		boolean inException = false;
		boolean inGrouping = false;
		boolean separatorGrouping = false;
		int groupingStart = 0;
		List<Pattern> parsedPattern = new ArrayList<Pattern>();
		indexesToTest = new ArrayList<Integer>();
		int currentStart = 0;
		int currentEnd = 0;
		for (int i = 0; i < regexp.length(); i++) {
			char c = regexp.charAt(i);
			if (!inLiteral&&c=='\\') {
				inLiteral = true;
			} else if (inLiteral) {
				if (c=='d'||c=='D'||c=='z') {
					// digit or non-digit = not a separator
					// \z is included here because we're only expecting it inside negative lookahead
					currentEnd = i+1;
				} else if (inGrouping) {
					separatorGrouping = true;
					currentEnd = i+1;
				} else {
					// always a separator
					// either an actual separator, or the patterns \p (all separators) or \s (whitespace)
					this.addPattern(regexp, currentStart, currentEnd, parsedPattern, inException);

					this.addPattern(regexp, i-1, i+1, parsedPattern, inException);
					currentStart = i+1;
					currentEnd = i+1;
				}
				inLiteral = false;
			} else if (c=='[') {
				inGrouping = true;
				separatorGrouping = false;
				groupingStart = i;
				currentEnd = i+1;
			} else if (c==']') {
				if (separatorGrouping) {
					if (groupingStart>0) {
						this.addPattern(regexp, currentStart, groupingStart, parsedPattern, inException);
					}
					this.addPattern(regexp, groupingStart, i+1, parsedPattern, inException);
					currentStart = i+1;
					currentEnd = i+1;
				} else {
					currentEnd = i+1;
				}
				inGrouping = false;
			} else if (c=='{') {
				this.addPattern(regexp, currentStart, currentEnd, parsedPattern, inException);
				inException = true;
				currentStart = i+1;
				currentEnd = i+1;
			} else if (c=='}') {
				this.addPattern(regexp, currentStart, currentEnd, parsedPattern, inException);
				inException = false;
				currentStart = i+1;
				currentEnd = i+1;
			} else if (c=='.' || c=='+' || c=='(' || c=='|' || c==')' || c=='^' || c=='?' || c=='!') {
				// special meaning characters, not separators
				currentEnd = i+1;
			} else if (separatorPattern.matcher("" + c).find()) {
				if (inGrouping) {
					separatorGrouping = true;
				} else {
					// a separator
					this.addPattern(regexp, currentStart, currentEnd, parsedPattern, inException);
	
					this.addPattern(regexp, i, i+1, parsedPattern, inException);
					currentStart = i+1;
					currentEnd = i+1;
				}
			} else {
				// any other non-separating character
				currentEnd = i+1;
			}
		}
		this.addPattern(regexp, currentStart, currentEnd, parsedPattern, inException);

		return parsedPattern;
	}
	
	private void addPattern(String testPattern, int start, int end, List<Pattern> parsedPattern, boolean inException) {
		if (start==end)
			return;
		
		String regex = testPattern.substring(start, end);
		
		if (regex.equals("\\p")) {
			// all separators
			parsedPattern.add(this.separatorPattern);
		} else {
			if (start==0) {
				// automatically add upper-case characters
				char c = testPattern.charAt(0);
				if (c=='(') {
					String patternOpening = "(";
					String patternToSplit = regex.substring(1,regex.indexOf(')'));
					if (patternToSplit.startsWith("?!")) {
						patternToSplit = patternToSplit.substring(2);
						patternOpening += "?!";
					}
					String[] patternParts = patternToSplit.split("\\|");
					String patternClosing = regex.substring(regex.indexOf(')'));
					regex = patternOpening;
					boolean firstPart = true;
					for (String patternPart : patternParts) {
						if (patternPart.length()>0) {
							if (!firstPart)
								regex += "|";
							char c2 = patternPart.charAt(0);
							if (c2!=Character.toUpperCase(c2)) {
								regex += "[" + this.getCharacters(c2) + "]" + patternPart.substring(1);
							} else {
								regex += patternPart;
							}
							firstPart = false;
						}
					}
					regex += patternClosing;
				}
				if (c!=Character.toUpperCase(c)) {
					regex = "[" + this.getCharacters(c) + "]" + regex.substring(1);
				}
			}
			
			// We never add the first pattern to the indexesToTest
			// since the interval concerns the interval between a token and the one preceeding it.
			if (parsedPattern.size()!=0 && !inException) {
				indexesToTest.add(parsedPattern.size());
			}
			
			parsedPattern.add(Pattern.compile(regex));
		}
	}
	
	private String getCharacters(char c) {
		// add uppercase equivalents automatically
		String characters = "";
		characters += c;
		characters += Character.toUpperCase(c);
		if (c=='à'||c=='â')
			characters += 'A';
		else if (c=='é'||c=='ê')
			characters += 'E';
		else if (c=='ô')
			characters += 'O';
		else if (c=='ç')
			characters += 'C';
		return characters;
	}
	
	public String getName() {
		if (name==null)
			name=regexp.replace(' ','_');
		return name;
	}

	public void setName(String name) {
		this.name = name.replace(' ','_');
	}
	
	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	@Override
	public String toString() {
		return this.getName();
	}

	@Override
	public int hashCode() {
		return regexp.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TokenPatternImpl other = (TokenPatternImpl) obj;
		if (regexp == null) {
			if (other.regexp != null)
				return false;
		} else if (!regexp.equals(other.regexp))
			return false;
		return true;
	}

	public TokeniserPatternServiceInternal getTokeniserPatternServiceInternal() {
		return tokeniserPatternServiceInternal;
	}

	public void setTokeniserPatternServiceInternal(
			TokeniserPatternServiceInternal tokeniserPatternServiceInternal) {
		this.tokeniserPatternServiceInternal = tokeniserPatternServiceInternal;
	}
	
}
