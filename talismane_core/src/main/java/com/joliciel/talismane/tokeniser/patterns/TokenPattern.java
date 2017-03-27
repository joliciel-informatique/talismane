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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * A pattern for analysing whether a particular token is likely to have a
 * TokeniserDecision different from the default for this token.<br/>
 * When matching a TokeniserPattern, we check if any set of n tokens in a
 * TokenSequence matches it.<br/>
 * If so, all of the tokens inside the set are to be tested further, unless they
 * have been marked with {} as not for further testing.<br/>
 * The TokeniserPattern will always match an exact number of tokens in a
 * TokenSequence.<br/>
 * The number of tokens <i>n</i> to be matched is calculated from the number of
 * separators explicitly included in the TokeniserPattern.<br/>
 * For example, the pattern "parce que" and "parce qu'" need to be listed
 * separately, as the second one has an additional separator. They can be given
 * the same name, to ensure they are handled as one statistical group.<br/>
 * This regexp on which this is built similar to a standard Pattern, but with
 * some limits:<br/>
 * The only permissible regular expression symbols for now are: . + ( ) | [ ] ^
 * \d \D \s \p \b \z and any of these escaped with a backslash.<br/>
 * The \p symbol has a special meaning: any separator (punctuation or
 * whitespace).<br/>
 * The \b symbol has a special meaning: whitespace, sentence start or sentence
 * end.<br/>
 * The repeated wildcard .+ is always assumed to represent a single
 * non-separating token.<br/>
 * Groups separated by the | operator must be surrounded by (). They should not
 * contain any separators, so that the number of tokens to be tested is always
 * constant (create separate TokeniserPattern if absolutely required).<br/>
 * Groups in the [] operator must either contain only separators, or only
 * non-separators.<br/>
 * The { } symbols have a special meaning: around set of tokens, they are taken
 * to mean that these tokens are only there to give context, and should not be
 * tested further to override the default.<br/>
 * The \p, \s and \b symbols are always assumed to be inside curly brackets (no
 * further testing)
 * 
 * @author Assaf Urieli
 *
 */
public class TokenPattern {
  private static final Logger LOG = LoggerFactory.getLogger(TokenPattern.class);
  private static final Pattern whitespacePattern = Pattern.compile("\\s", Pattern.UNICODE_CHARACTER_CLASS);

  private final String regexp;
  private final Pattern separatorPattern;
  private final List<Pattern> parsedPattern;
  private final List<Integer> indexesToTest = new ArrayList<>();
  private final List<Boolean> isSeparatorClassList = new ArrayList<>();
  private String name;
  private String groupName;
  private boolean startsWithSeparatorClass = false;

  public TokenPattern(String regexp, Pattern separatorPattern) throws TalismaneException {
    this.regexp = regexp;
    this.separatorPattern = separatorPattern;
    this.parsedPattern = this.parsePattern(this.regexp);
  }

  /**
   * The regular expression on which this TokeniserPattern was built.
   */
  public String getRegExp() {
    return this.regexp;
  }

  /**
   * The original pattern, broken up into chunks where each chunk should match
   * exactly one token in the token sequence. Each chunk is a standard Pattern,
   * but may or may not be interpreted as such (e.g. .+ will only match
   * non-separating tokens).
   */
  public List<Pattern> getParsedPattern() {
    return this.parsedPattern;
  }

  /**
   * The number of tokens that will be matched by this pattern in the
   * TokenSequence.
   */
  public int getTokenCount() {
    return this.parsedPattern.size();
  }

  /**
   * The indexes in getParsedPattern corresponding to tokens that need to be
   * examined further, to decide if they represent a token break or not. These
   * will typically correspond to all separators.
   */
  public List<Integer> getIndexesToTest() {
    return this.indexesToTest;
  }

  /**
   * Return a TokenPatternMatchSequence for each sequence of <i>n</i> tokens in
   * a TokenSequence which match this pattern. Will also add any matches to
   * Token.getMatches() for the matched tokens.
   */
  public List<TokenPatternMatchSequence> match(TokenSequence tokenSequence) {
    List<TokenPatternMatchSequence> matchingSequences = new ArrayList<TokenPatternMatchSequence>();
    boolean matchSentenceStart = false;
    if (this.getParsedPattern().get(0).pattern().equals("\\b")) {
      matchSentenceStart = true;
    }
    boolean matchSentenceEnd = false;
    if (this.getParsedPattern().get(this.getParsedPattern().size() - 1).pattern().equals("\\b")) {
      matchSentenceEnd = true;
    }

    for (int t0 = -1; t0 < tokenSequence.listWithWhiteSpace().size(); t0++) {
      boolean haveMatch = false;
      List<Token> matchingSequence = new ArrayList<Token>();
      if (t0 >= 0) {
        // does the current token match the beginning of the pattern?
        Token token = tokenSequence.listWithWhiteSpace().get(t0);
        if (checkTokenForMatch(this.getParsedPattern().get(0), token)) {
          // potential match, let's follow it through
          haveMatch = true;
          // we match so far, add it to the temp list
          matchingSequence.add(token);
        }
      } else if (matchSentenceStart) {
        // automatically match start of sentence
        haveMatch = true;
        // add null token to the temp list
        matchingSequence.add(null);
      }
      if (haveMatch) {
        int p = 1;
        int t1 = t0 + 1;
        while (p < this.getParsedPattern().size() && t1 < tokenSequence.listWithWhiteSpace().size()) {
          Token aToken = tokenSequence.listWithWhiteSpace().get(t1);
          Pattern pattern = this.getParsedPattern().get(p);
          if (checkTokenForMatch(pattern, aToken)) {
            // we match so far, add it to the temp list
            matchingSequence.add(aToken);
          } else {
            // pattern doesn't match
            haveMatch = false;
            break;
          }
          p++;
          t1++;
        } // next token and parsed pattern

        if (t1 == tokenSequence.listWithWhiteSpace().size() && p == this.getParsedPattern().size() - 1 && matchSentenceEnd) {
          // add a null token representing the sentence end
          matchingSequence.add(null);
        }

        // Did we get a full match (or did we hit the end of the
        // sentence first)
        if (matchingSequence.size() != this.getParsedPattern().size()) {
          haveMatch = false;
        }
      } // Current token matched start of pattern, try to match the rest
        // of the pattern

      if (haveMatch) {
        TokenPatternMatchSequence tokenPatternMatchSequence = new TokenPatternMatchSequence(this, matchingSequence);
        matchingSequences.add(tokenPatternMatchSequence);
        for (Token aToken : matchingSequence) {
          tokenPatternMatchSequence.addMatch(aToken);
        }
      }
    } // next token

    if (LOG.isTraceEnabled()) {
      if (matchingSequences.size() > 0)
        LOG.trace(this.getName() + ": matchingSequences = " + matchingSequences);
    }
    return matchingSequences;
  }

  /**
   * Does this particular token match this particular pattern.
   */
  boolean checkTokenForMatch(Pattern pattern, Token token) {
    String regex = pattern.pattern();
    if (regex.contains(".+") || regex.contains("\\D")) {
      // this pattern is only allowed for non-separators
      if (!token.isSeparator())
        return pattern.matcher(token.getAnalyisText()).matches();
      else
        return false;
    } else if (!this.separatorPattern.matcher(regex).find()) {
      // no separators, we simply check for string equality
      return (regex.equals(token.getAnalyisText()));
    } else if (regex.startsWith("\\") && !(regex.startsWith("\\d")) && !(regex.startsWith("\\s")) && !(regex.startsWith("\\p")) && !(regex.startsWith("\\b"))) {
      // an escaped separator
      return (regex.substring(1).equals(token.getAnalyisText()));
    } else if (regex.length() == 1) {
      // an unescaped separator
      return (regex.equals(token.getAnalyisText()));
    } else if (regex.equals("\\b")) {
      // \b matches whitespace, sentence start and sentence end
      return (whitespacePattern.matcher(token.getAnalyisText()).matches());
    } else {
      // a true pattern
      return (pattern.matcher(token.getAnalyisText()).matches());
    }
  }

  /**
   * Break the regexp up into chunks, where each chunk will match one token.
   * 
   * @throws TalismaneException
   */
  List<Pattern> parsePattern(String regexp) throws TalismaneException {
    boolean inLiteral = false;
    boolean inException = false;
    boolean inGrouping = false;
    boolean groupingHasLetters = false;
    int groupingStart = 0;
    List<Pattern> parsedPattern = new ArrayList<Pattern>();

    int currentStart = 0;
    int currentEnd = 0;
    for (int i = 0; i < regexp.length(); i++) {
      char c = regexp.charAt(i);
      if (!inLiteral && c == '\\') {
        inLiteral = true;
      } else if (inLiteral) {
        if (c == 'd' || c == 'D' || c == 'z') {
          // digit or non-digit = not a separator
          // \z is included here because we're only expecting it
          // inside negative lookahead
          currentEnd = i + 1;
        } else if (inGrouping) {
          currentEnd = i + 1;
        } else {
          // always a separator
          // either an actual separator, or the patterns \p (all
          // separators) or \s (whitespace)
          // or \b (whitespace/sentence start/sentence end)
          this.addPattern(regexp, currentStart, currentEnd, parsedPattern, inException);

          this.addPattern(regexp, i - 1, i + 1, parsedPattern, inException);
          currentStart = i + 1;
          currentEnd = i + 1;
        }
        inLiteral = false;
      } else if (c == '[') {
        inGrouping = true;
        groupingHasLetters = false;
        groupingStart = i;
        currentEnd = i + 1;
      } else if (c == ']') {
        if (!groupingHasLetters) {
          if (groupingStart > 0) {
            this.addPattern(regexp, currentStart, groupingStart, parsedPattern, inException);
          }
          this.addPattern(regexp, groupingStart, i + 1, parsedPattern, inException);
          currentStart = i + 1;
          currentEnd = i + 1;
        } else {
          currentEnd = i + 1;
        }
        inGrouping = false;
      } else if (c == '{') {
        this.addPattern(regexp, currentStart, currentEnd, parsedPattern, inException);
        inException = true;
        currentStart = i + 1;
        currentEnd = i + 1;
      } else if (c == '}') {
        this.addPattern(regexp, currentStart, currentEnd, parsedPattern, inException);
        inException = false;
        currentStart = i + 1;
        currentEnd = i + 1;
      } else if (c == '.' || c == '+' || c == '(' || c == '|' || c == ')' || c == '^' || c == '?' || c == '!') {
        // special meaning characters, not separators
        currentEnd = i + 1;
      } else if (c == '-') {
        // either the dash separator, or a character range (e.g. A-Z)
        if (inGrouping) {
          // do nothing
          // we don't know if it's a separator grouping or a character
          // range
        } else {
          // a separator
          this.addPattern(regexp, currentStart, currentEnd, parsedPattern, inException);

          this.addPattern(regexp, i, i + 1, parsedPattern, inException);
          currentStart = i + 1;
          currentEnd = i + 1;
        }
      } else if (separatorPattern.matcher("" + c).find()) {
        if (inGrouping) {
          if (groupingHasLetters) {
            throw new TalismaneException("Cannot mix separators and non-separators in same grouping");
          }
        } else {
          // a separator
          this.addPattern(regexp, currentStart, currentEnd, parsedPattern, inException);

          this.addPattern(regexp, i, i + 1, parsedPattern, inException);
          currentStart = i + 1;
          currentEnd = i + 1;
        }
      } else {
        // any other non-separating character
        if (inGrouping) {
          groupingHasLetters = true;
        }
        currentEnd = i + 1;
      }
    }
    this.addPattern(regexp, currentStart, currentEnd, parsedPattern, inException);

    if (LOG.isTraceEnabled()) {
      int i = 0;
      LOG.trace("Parsed " + regexp);
      for (Pattern pattern : parsedPattern) {
        boolean test = indexesToTest.contains(i);
        LOG.trace("Added " + pattern.pattern() + " Test? " + test);
        i++;
      }
    }

    if (indexesToTest.size() == 0) {
      throw new InvalidTokenPatternException("No indexes to test in pattern: " + this.getName());
    }

    return parsedPattern;
  }

  private void addPattern(String testPattern, int start, int end, List<Pattern> parsedPattern, boolean inException) {
    if (start == end)
      return;

    String regex = testPattern.substring(start, end);

    if (regex.equals("\\p")) {
      // all separators
      parsedPattern.add(this.separatorPattern);
    } else {
      if (parsedPattern.size() == 0 || (parsedPattern.size() == 1 && parsedPattern.get(0).pattern().equals("\\b"))) {
        // automatically add upper-case characters
        char c = testPattern.charAt(start);
        if (c == '(') {
          String patternOpening = "(";
          String patternToSplit = regex.substring(1, regex.indexOf(')'));
          if (patternToSplit.startsWith("?!")) {
            patternToSplit = patternToSplit.substring(2);
            patternOpening += "?!";
          }
          String[] patternParts = patternToSplit.split("\\|");
          String patternClosing = regex.substring(regex.indexOf(')'));
          regex = patternOpening;
          boolean firstPart = true;
          for (String patternPart : patternParts) {
            if (patternPart.length() > 0) {
              if (!firstPart)
                regex += "|";
              char c2 = patternPart.charAt(0);
              if (c2 != Character.toUpperCase(c2)) {
                regex += "[" + this.getCharacters(c2) + "]" + patternPart.substring(1);
              } else {
                regex += patternPart;
              }
              firstPart = false;
            }
          }
          regex += patternClosing;
        }
        if (c != Character.toUpperCase(c)) {
          regex = "[" + this.getCharacters(c) + "]" + regex.substring(1);
        }
      }

      // We never add the first pattern to the indexesToTest
      // since the interval concerns the interval between a token and the
      // one preceeding it.

      boolean isSeparatorClass = regex.equals("\\p") || regex.equals("\\s") || regex.equals("\\b");
      isSeparatorClassList.add(isSeparatorClass);
      if (isSeparatorClass && parsedPattern.size() == 0)
        startsWithSeparatorClass = true;

      if (!(parsedPattern.size() == 0 || (parsedPattern.size() == 1 && startsWithSeparatorClass) || inException || isSeparatorClass)) {
        indexesToTest.add(parsedPattern.size());
      }

      parsedPattern.add(Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS));
    }
  }

  private String getCharacters(char c) {
    // add uppercase equivalents automatically
    String characters = "";
    characters += c;
    characters += Character.toUpperCase(c);
    if (c == 'à' || c == 'â')
      characters += 'A';
    else if (c == 'é' || c == 'ê')
      characters += 'E';
    else if (c == 'ô')
      characters += 'O';
    else if (c == 'ç')
      characters += 'C';
    return characters;
  }

  /**
   * This token pattern's user-friendly name. Can also be used to group together
   * two patterns whose statistical distribtuion should be identical, e.g.
   * "parce que" and "parce qu'".
   */
  public String getName() {
    if (name == null) {
      name = regexp;
    }
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * A name for grouping together several patterns due to take advantage of
   * their distributional similarity in features.
   */
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
    TokenPattern other = (TokenPattern) obj;
    if (regexp == null) {
      if (other.regexp != null)
        return false;
    } else if (!regexp.equals(other.regexp))
      return false;
    return true;
  }

  /**
   * Is the pattern at the provided index a separator class pattern.
   */
  public boolean isSeparatorClass(int index) {
    getParsedPattern();
    return isSeparatorClassList.get(index);
  }

}
