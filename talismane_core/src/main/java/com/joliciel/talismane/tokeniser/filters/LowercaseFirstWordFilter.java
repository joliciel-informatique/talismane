package com.joliciel.talismane.tokeniser.filters;

import java.util.Set;
import java.util.regex.Pattern;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * Systematically lowercase the first alphabetic word of a sentence, unless it
 * is in all caps.
 */
public class LowercaseFirstWordFilter implements TokenFilter {
  private final TalismaneSession session;

  private static final Pattern alphaPattern = Pattern.compile("\\p{L}.*", Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern punctuation = Pattern.compile("[:;]", Pattern.UNICODE_CHARACTER_CLASS);

  public LowercaseFirstWordFilter(TalismaneSession session) {
    this.session = session;
  }

  @Override
  public void apply(TokenSequence tokenSequence) {
    boolean lowerNextWord = true;
    for (Token token : tokenSequence) {
      String word = token.getText();
      if (lowerNextWord && alphaPattern.matcher(word).matches()) {
        if (Character.isUpperCase(word.charAt(0)) && (word.length() == 1 || !word.equals(word.toUpperCase()))) {
          Set<String> possibleWords = session.getDiacriticizer().diacriticize(word);
          if (possibleWords.size() > 0)
            token.setText(possibleWords.iterator().next());
          else
            token.setText(word.toLowerCase());
        }
        lowerNextWord = false;
      } else if (punctuation.matcher(word).matches()) {
        lowerNextWord = true;
      }
    } // next token
  }
}
