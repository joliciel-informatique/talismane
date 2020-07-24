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
package com.joliciel.talismane.sentenceAnnotators;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.resources.WordList;
import com.joliciel.talismane.tokeniser.StringAttribute;
import com.joliciel.talismane.tokeniser.TokenAttribute;
import com.joliciel.talismane.utils.StringUtils;

/**
 * A parent class for RegexAnnotator implementations, which knows how to
 * construct the Pattern using the various attributes set and the initial regex,
 * and how to apply the regex to get the matching placeholders.<br/>
 * <br/>
 * The following parameters are recognised:
 * <ul>
 * <li>group: see {@link RegexAnnotator#getGroupIndex()}</li>
 * <li>caseSensitive: see {@link RegexAnnotator#isCaseSensitive()}</li>
 * <li>diacriticSensitive: see {@link RegexAnnotator#isDiacriticSensitive()}
 * </li>
 * <li>autoWordBoundaries: see {@link RegexAnnotator#isAutoWordBoundaries()}
 * </li>
 * </ul>
 * <br/>
 * <br/>
 * Any additional unrecognised parameters are assumed to be token attributes.
 * 
 * @author Assaf Urieli
 *
 */
public abstract class AbstractRegexAnnotator implements RegexAnnotator {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractRegexAnnotator.class);
  private static Pattern wordListPattern = Pattern.compile("\\\\p\\{WordList\\((.*?)\\)\\}", Pattern.UNICODE_CHARACTER_CLASS);
  private static Pattern diacriticPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
  private final String regex;
  private final Pattern pattern;
  private final int groupIndex;
  private final Map<String, TokenAttribute<?>> attributes = new HashMap<String, TokenAttribute<?>>();
  private final boolean caseSensitive;
  private final boolean diacriticSensitive;
  private final boolean autoWordBoundaries;
  private boolean excluded = false;
  private final boolean singleToken;
  private final Map<String, String> parameters = new HashMap<>();
  private final String sessionId;

  /**
   * A constructor with the minimum required data. This is the constructor to be
   * used when creating annotators directly in code.
   */
  public AbstractRegexAnnotator(String regex, int groupIndex, boolean caseSensitive, boolean diacricticSensitive, boolean autoWordBoundaries,
      boolean singleToken, String sessionId) throws SentenceAnnotatorLoadException {
    this.sessionId = sessionId;
    this.regex = regex;
    this.groupIndex = groupIndex;
    this.caseSensitive = caseSensitive;
    this.diacriticSensitive = diacricticSensitive;
    this.autoWordBoundaries = autoWordBoundaries;
    this.singleToken = singleToken;
    this.pattern = this.constructPattern();
  }

  /**
   * A constructor used when automatically generating annotator from
   * descriptors.
   * 
   * @param descriptor
   *          the full descriptor used to construct the annotator
   * @param handledParameters
   *          any parameters handled by the sub-class - any additional
   *          parameters are either handled here, or added as token attributes
   * @param singleToken
   *          whether this annotator identifies a single token's boundaries
   * 
   * @throws SentenceAnnotatorLoadException
   */
  public AbstractRegexAnnotator(String descriptor, Map<String, String> defaultParams, Set<String> handledParameters, boolean singleToken,
      String sessionId) throws SentenceAnnotatorLoadException {
    this.sessionId = sessionId;
    this.singleToken = singleToken;

    String[] tabs = descriptor.split("\t");

    if (tabs.length < 2)
      throw new SentenceAnnotatorLoadException(
          "Wrong number of tabs for " + RegexAnnotator.class.getSimpleName() + ". Expected at least 2, but was " + tabs.length + " in: " + descriptor);
    this.regex = tabs[1];

    parameters.putAll(defaultParams);
    for (int i = 2; i < tabs.length; i++) {
      String tab = tabs[i];
      // allow empty tabs
      if (tab.length() > 0) {
        int equalsPos = tab.indexOf('=');
        if (equalsPos < 0)
          throw new SentenceAnnotatorLoadException("No equals sign in parameter " + (i + 1) + " [" + tab + "] in: " + descriptor);
        String paramName = tab.substring(0, tab.indexOf('='));
        String paramValue = tab.substring(tab.indexOf('=') + 1);
        parameters.put(paramName, paramValue);
      }
    }

    int groupIndex = 0;
    boolean caseSensitive = true;
    boolean diacriticSensitive = true;
    boolean autoWordBoundaries = false;

    for (String paramName : parameters.keySet()) {
      String paramValue = parameters.get(paramName);
      if (handledParameters.contains(paramName))
        continue;

      if (paramName.equals("group")) {
        groupIndex = Integer.parseInt(paramValue);
      } else if (paramName.equals("caseSensitive")) {
        caseSensitive = Boolean.valueOf(paramValue);
      } else if (paramName.equals("diacriticSensitive")) {
        diacriticSensitive = Boolean.valueOf(paramValue);
      } else if (paramName.equals("autoWordBoundaries")) {
        autoWordBoundaries = Boolean.valueOf(paramValue);
      } else {
        this.addAttribute(paramName, new StringAttribute(paramName, paramValue));
      }
    }

    this.groupIndex = groupIndex;
    this.caseSensitive = caseSensitive;
    this.diacriticSensitive = diacriticSensitive;
    this.autoWordBoundaries = autoWordBoundaries;

    if (regex == null || regex.length() == 0)
      throw new SentenceAnnotatorLoadException("Cannot use an empty regex for a filter");

    this.pattern = this.constructPattern();
    Matcher matcher = pattern.matcher("");
    if (this.groupIndex > matcher.groupCount()) {
      throw new SentenceAnnotatorLoadException("No group " + this.groupIndex + " in pattern: " + this.regex);
    }
  }

  /**
   * Parameters extracted from the descriptor.
   */
  Map<String, String> getParameters() {
    return this.parameters;
  }

  private Pattern constructPattern() throws SentenceAnnotatorLoadException {
    // we may need to replace WordLists by the list contents
    String myRegex = this.regex;

    if (LOG.isTraceEnabled()) {
      LOG.trace("Regex: " + myRegex);
    }

    if (this.autoWordBoundaries) {
      Boolean startsWithLetter = null;
      for (int i = 0; i < myRegex.length() && startsWithLetter == null; i++) {
        char c = myRegex.charAt(i);
        if (c == '\\') {
          i++;
          c = myRegex.charAt(i);
          if (c == 'd' || c == 'w') {
            startsWithLetter = true;
          } else if (c == 's' || c == 'W' || c == 'b' || c == 'B') {
            startsWithLetter = false;
          } else if (c == 'p') {
            i += 2; // skip the open curly brackets
            int closeCurlyBrackets = myRegex.indexOf('}', i);
            int openParentheses = myRegex.indexOf('(', i);
            int endIndex = closeCurlyBrackets;
            if (openParentheses > 0 && openParentheses < closeCurlyBrackets)
              endIndex = openParentheses;
            if (endIndex > 0) {
              String specialClass = myRegex.substring(i, endIndex);
              if (specialClass.equals("WordList")) {
                startsWithLetter = true;
              }
            }
            if (startsWithLetter == null)
              startsWithLetter = false;
          }
          break;
        } else if (c == '[' || c == '(') {
          // do nothing
        } else if (Character.isLetter(c) || Character.isDigit(c)) {
          startsWithLetter = true;
        } else {
          startsWithLetter = false;
        }
      }

      Boolean endsWithLetter = null;
      for (int i = myRegex.length() - 1; i >= 0 && endsWithLetter == null; i--) {
        char c = myRegex.charAt(i);
        char prevC = ' ';
        char prevPrevC = ' ';
        if (i >= 1)
          prevC = myRegex.charAt(i - 1);
        if (i >= 2)
          prevPrevC = myRegex.charAt(i - 2);
        if (prevC == '\\' && StringUtils.countChar(myRegex, prevC, i - 1, false) % 2 == 1) {
          // the previous character was an escaping backslash
          if (c == 'd' || c == 'w') {
            endsWithLetter = true;
          } else if (c == 's' || c == 'W' || c == 'b' || c == 'B') {
            endsWithLetter = false;
          } else {
            endsWithLetter = false;
          }
          break;
        } else if (c == ']' || c == ')' || c == '+') {
          // do nothing
        } else if (c == '}') {
          int startIndex = myRegex.lastIndexOf('{') + 1;
          int closeCurlyBrackets = myRegex.indexOf('}', startIndex);
          int openParentheses = myRegex.indexOf('(', startIndex);
          int endIndex = closeCurlyBrackets;
          if (openParentheses > 0 && openParentheses < closeCurlyBrackets)
            endIndex = openParentheses;
          if (endIndex > 0) {
            String specialClass = myRegex.substring(startIndex, endIndex);
            if (specialClass.equals("WordList") || specialClass.equals("Alpha") || specialClass.equals("Lower") || specialClass.equals("Upper")
                || specialClass.equals("ASCII") || specialClass.equals("Digit")) {
              endsWithLetter = true;
            }
          }
          break;
        } else if (c == '?' || c == '*') {
          if (Character.isLetterOrDigit(prevC)) {
            if (prevPrevC == '\\' && StringUtils.countChar(myRegex, prevPrevC, i - 2, false) % 2 == 1) {
              // the preceding character was an escaping
              // backslash...
              if (prevC == 'd' || prevC == 'w') {
                // skip this construct
                i -= 2;
              } else {
                endsWithLetter = false;
              }
            } else {
              // since the matched text may or may not match
              // prevC
              // we skip this letter and continue, to find out
              // if prior to this letter
              // there's another letter
              i--;
            }
          } else {
            endsWithLetter = false;
          }
        } else if (Character.isLetterOrDigit(c)) {
          endsWithLetter = true;
        } else {
          endsWithLetter = false;
        }
      }

      if (startsWithLetter != null && startsWithLetter) {
        myRegex = "\\b" + myRegex;
      }
      if (endsWithLetter != null && endsWithLetter) {
        myRegex = myRegex + "\\b";
      }
      if (LOG.isTraceEnabled()) {
        LOG.trace("After autoWordBoundaries: " + myRegex);
      }
    }

    if (!this.caseSensitive || !this.diacriticSensitive) {
      StringBuilder regexBuilder = new StringBuilder();
      for (int i = 0; i < myRegex.length(); i++) {
        char c = myRegex.charAt(i);
        if (c == '\\') {
          // escape - skip next
          regexBuilder.append(c);
          i++;
          c = myRegex.charAt(i);
          regexBuilder.append(c);
        } else if (c == '[') {
          // character group, don't change it
          regexBuilder.append(c);
          while (c != ']' && i < myRegex.length()) {
            i++;
            c = myRegex.charAt(i);
            regexBuilder.append(c);
          }
        } else if (c == '{') {
          // command, don't change it
          regexBuilder.append(c);
          while (c != '}' && i < myRegex.length()) {
            i++;
            c = myRegex.charAt(i);
            regexBuilder.append(c);
          }
        } else if (Character.isLetter(c)) {
          Set<String> chars = new TreeSet<String>();
          chars.add("" + c);
          char noAccent = diacriticPattern.matcher(Normalizer.normalize("" + c, Form.NFD)).replaceAll("").charAt(0);

          if (!this.caseSensitive) {
            chars.add("" + Character.toUpperCase(c));
            chars.add("" + Character.toLowerCase(c));
            chars.add("" + Character.toUpperCase(noAccent));
          }
          if (!this.diacriticSensitive) {
            chars.add("" + noAccent);
            if (!this.caseSensitive) {
              chars.add("" + Character.toLowerCase(noAccent));
            }
          }
          if (chars.size() == 1) {
            regexBuilder.append(c);
          } else {
            regexBuilder.append('[');
            for (String oneChar : chars) {
              regexBuilder.append(oneChar);
            }
            regexBuilder.append(']');
          }
        } else {
          regexBuilder.append(c);
        }
      }
      myRegex = regexBuilder.toString();
      if (LOG.isTraceEnabled()) {
        LOG.trace("After caseSensitive: " + myRegex);
      }
    }

    Matcher matcher = wordListPattern.matcher(myRegex);
    StringBuilder regexBuilder = new StringBuilder();

    int lastIndex = 0;
    while (matcher.find()) {
      String[] params = matcher.group(1).split(",");
      int start = matcher.start();
      int end = matcher.end();
      regexBuilder.append(myRegex.substring(lastIndex, start));

      String wordListName = params[0];
      boolean uppercaseOptional = false;
      boolean diacriticsOptional = false;
      boolean lowercaseOptional = false;
      boolean firstParam = true;
      for (String param : params) {
        if (firstParam) {
          /* word list name */ } else if (param.equals("diacriticsOptional"))
          diacriticsOptional = true;
        else if (param.equals("uppercaseOptional"))
          uppercaseOptional = true;
        else if (param.equals("lowercaseOptional"))
          lowercaseOptional = true;
        else
          throw new SentenceAnnotatorLoadException("Unknown parameter in word list " + matcher.group(1) + ": " + param);
        firstParam = false;
      }

      WordList wordList = TalismaneSession.get(sessionId).getWordListFinder().getWordList(wordListName);
      if (wordList == null)
        throw new SentenceAnnotatorLoadException("Unknown word list: " + wordListName);

      StringBuilder sb = new StringBuilder();

      boolean firstWord = true;
      for (String word : wordList.getWordList()) {
        if (!firstWord)
          sb.append("|");
        word = Normalizer.normalize(word, Form.NFC);
        if (uppercaseOptional || diacriticsOptional) {
          String wordNoDiacritics = Normalizer.normalize(word, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
          String wordLowercase = word.toLowerCase(Locale.ENGLISH);
          String wordLowercaseNoDiacritics = Normalizer.normalize(wordLowercase, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
          String wordUppercase = wordNoDiacritics.toUpperCase(Locale.ENGLISH);

          boolean needsGrouping = false;
          if (uppercaseOptional && !word.equals(wordLowercase))
            needsGrouping = true;
          if (diacriticsOptional && !word.equals(wordNoDiacritics))
            needsGrouping = true;
          if (lowercaseOptional && !word.equals(wordUppercase))
            needsGrouping = true;
          if (needsGrouping) {
            for (int i = 0; i < word.length(); i++) {
              char c = word.charAt(i);

              boolean grouped = false;
              if (uppercaseOptional && c != wordLowercase.charAt(i))
                grouped = true;
              if (diacriticsOptional && c != wordNoDiacritics.charAt(i))
                grouped = true;
              if (lowercaseOptional && c != wordUppercase.charAt(i))
                grouped = true;

              if (!grouped)
                sb.append(c);
              else {
                sb.append("[");
                String group = "" + c;
                if (uppercaseOptional && group.indexOf(wordLowercase.charAt(i)) < 0)
                  group += (wordLowercase.charAt(i));
                if (lowercaseOptional && group.indexOf(wordUppercase.charAt(i)) < 0)
                  group += (wordUppercase.charAt(i));
                if (diacriticsOptional && group.indexOf(wordNoDiacritics.charAt(i)) < 0)
                  group += (wordNoDiacritics.charAt(i));
                if (uppercaseOptional && diacriticsOptional && group.indexOf(wordLowercaseNoDiacritics.charAt(i)) < 0)
                  group += (wordLowercaseNoDiacritics.charAt(i));

                sb.append(group);
                sb.append("]");
              } // does this letter need grouping?
            } // next letter
          } else {
            sb.append(word);
          } // any options activated?
        } else {
          sb.append(word);
        }
        firstWord = false;
      } // next word in list

      regexBuilder.append(sb.toString());
      lastIndex = end;
    } // next match
    regexBuilder.append(myRegex.substring(lastIndex));
    myRegex = regexBuilder.toString();
    if (LOG.isTraceEnabled()) {
      LOG.trace("Replaced regex " + this.regex + " with: ");
      LOG.trace(myRegex);
    }
    Pattern pattern = Pattern.compile(myRegex, Pattern.UNICODE_CHARACTER_CLASS);
    return pattern;
  }

  @Override
  public void annotate(Sentence annotatedText, String... labels) {
    List<Annotation<TokenPlaceholder>> placeholders = new ArrayList<>();
    List<Annotation<TokenAttribute<?>>> annotations = new ArrayList<>();

    Matcher matcher = this.getPattern().matcher(annotatedText.getText());
    int lastStart = -1;
    while (matcher.find()) {
      int start = matcher.start(groupIndex);
      if (start > lastStart) {
        int end = matcher.end(groupIndex);

        if (LOG.isTraceEnabled()) {
          LOG.trace("Regex: " + this.regex);
          LOG.trace("Next match: " + annotatedText.getText().subSequence(matcher.start(), matcher.end()).toString().replace('\n', '¶').replace('\r', '¶'));
          if (matcher.start() != start || matcher.end() != end) {
            LOG.trace("But matching group: " + annotatedText.getText().subSequence(start, end).toString().replace('\n', '¶').replace('\r', '¶'));
          }
        }

        if (this.singleToken) {
          String replacement = this.findReplacement(annotatedText.getText(), matcher);
          TokenPlaceholder placeholder = new TokenPlaceholder(replacement, regex);
          Annotation<TokenPlaceholder> placeholderAnnotation = new Annotation<>(start, end, placeholder, labels);
          placeholders.add(placeholderAnnotation);

          if (LOG.isTraceEnabled())
            LOG.trace("Added placeholder: " + placeholder.toString());
        }

        for (String key : attributes.keySet()) {
          TokenAttribute<?> attribute = attributes.get(key);
          Annotation<TokenAttribute<?>> annotation = new Annotation<>(start, end, attribute, labels);
          annotations.add(annotation);
          if (LOG.isTraceEnabled())
            LOG.trace("Added attribute: " + attribute.toString());
        }

      }
      lastStart = start;
    }
    annotatedText.addAnnotations(placeholders);
    annotatedText.addAnnotations(annotations);
  }

  /**
   * If the token text should be replaced, return something, otherwise return
   * null.
   */
  protected String findReplacement(CharSequence text, Matcher matcher) {
    return null;
  }

  @Override
  public String getRegex() {
    return regex;
  }

  @Override
  public String toString() {
    return "AbstractRegexFilter [regex=" + regex + "]";
  }

  @Override
  public int getGroupIndex() {
    return groupIndex;
  }

  @Override
  public Map<String, TokenAttribute<?>> getAttributes() {
    return attributes;
  }

  @Override
  public void addAttribute(String key, TokenAttribute<?> value) {
    attributes.put(key, value);
  }

  @Override
  public Pattern getPattern() {
    return pattern;
  }

  @Override
  public boolean isDiacriticSensitive() {
    return diacriticSensitive;
  }

  @Override
  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  @Override
  public boolean isAutoWordBoundaries() {
    return autoWordBoundaries;
  }

  @Override
  public boolean isExcluded() {
    return excluded;
  }

  public void setExcluded(boolean excluded) {
    this.excluded = excluded;
  }
}
