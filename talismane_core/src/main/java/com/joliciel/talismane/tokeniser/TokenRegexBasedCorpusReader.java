package com.joliciel.talismane.tokeniser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.corpus.CorpusLine;
import com.joliciel.talismane.corpus.CorpusLine.CorpusElement;
import com.joliciel.talismane.corpus.CorpusLineReader;
import com.joliciel.talismane.corpus.TokenPerLineCorpusReader;
import com.joliciel.talismane.lexicon.RegexLexicalEntryReader;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.utils.io.CurrentFileObserver;
import com.typesafe.config.Config;

/**
 * A corpus reader that expects one token per line, and analyses the line
 * content based on a regex supplied during construction, via a
 * {@link CorpusLineReader}.<br/>
 * 
 * The following placeholders are required:<br/>
 * {@link CorpusElement#TOKEN} <br/>
 * These are included surrounded by % signs on both sides, and without the
 * prefix "CorpusElement."<br/>
 * 
 * Example (note that the regex is applied to one line, so no endline is
 * necessary):
 * 
 * <pre>
 * .+\t%TOKEN%
 * </pre>
 * 
 * @author Assaf Urieli
 *
 */
public class TokenRegexBasedCorpusReader extends TokenPerLineCorpusReader implements TokeniserAnnotatedCorpusReader, CurrentFileObserver {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(TokenRegexBasedCorpusReader.class);

  protected PretokenisedSequence tokenSequence = null;
  private final List<TokenFilter> filters;

  /**
   * Add attributes as specified in the config to the corpus reader. Recognises
   * the attributes:<br/>
   * - input-pattern: the pattern to match corpus line elements, see class
   * description.<br/>
   * - sentence-file: where to read the correctly formatted sentences<br/>
   * - corpus-lexical-entry-regex: how to read the lexical entries, see
   * {@link RegexLexicalEntryReader}<br/>
   * 
   * @param config
   *          the local config for this corpus reader (local namespace)
   * @throws ReflectiveOperationException
   *           if a TokenFilter cannot be built
   */
  public TokenRegexBasedCorpusReader(Reader reader, Config config, TalismaneSession session)
      throws IOException, TalismaneException, ReflectiveOperationException {
    super(reader, config, session);

    Config topLevelConfig = session.getConfig();

    this.filters = new ArrayList<>();

    String configPath = "talismane.core." + session.getId() + ".tokeniser.filters";
    List<String> filterDescriptors = topLevelConfig.getStringList(configPath);
    for (String descriptor : filterDescriptors) {
      TokenFilter filter = TokenFilter.loadFilter(descriptor, session);
      this.filters.add(filter);
    }
  }

  @Override
  protected CorpusElement[] getRequiredElements() {
    return new CorpusElement[] { CorpusElement.TOKEN };
  }

  @Override
  protected void processSentence(Sentence sentence, List<CorpusLine> corpusLines) throws TalismaneException, IOException {
    try {
      super.processSentence(sentence, corpusLines);
      tokenSequence = new PretokenisedSequence(sentence, session);
      for (CorpusLine corpusLine : corpusLines) {
        this.convertToToken(tokenSequence, corpusLine);
      }

      for (TokenFilter filter : filters)
        filter.apply(tokenSequence);

      tokenSequence.cleanSlate();
    } catch (TalismaneException e) {
      this.clearSentence();
      throw e;
    }
  }

  @Override
  public TokenSequence nextTokenSequence() throws TalismaneException, IOException {
    TokenSequence nextSentence = null;
    if (this.hasNextSentence()) {
      nextSentence = tokenSequence;
      this.clearSentence();
    }
    return nextSentence;
  }

  @Override
  protected void clearSentence() {
    super.clearSentence();
    this.tokenSequence = null;
  }

  @Override
  public Map<String, String> getCharacteristics() {
    return super.getCharacteristics();
  }

  @Override
  public Sentence nextSentence() throws TalismaneException, IOException {
    return this.nextTokenSequence().getSentence();
  }

  @Override
  public boolean isNewParagraph() {
    return false;
  }

  /**
   * Convert a data line into a token, and add it to the provided token
   * sequence.
   * 
   * @throws TalismaneException
   */
  protected Token convertToToken(PretokenisedSequence tokenSequence, CorpusLine corpusLine) throws TalismaneException {
    Token token = tokenSequence.addToken(corpusLine.getElement(CorpusElement.TOKEN));
    if (corpusLine.hasElement(CorpusElement.FILENAME))
      token.setFileName(corpusLine.getElement(CorpusElement.FILENAME));
    if (corpusLine.hasElement(CorpusElement.ROW))
      token.setLineNumber(Integer.parseInt(corpusLine.getElement(CorpusElement.ROW)));
    if (corpusLine.hasElement(CorpusElement.COLUMN))
      token.setColumnNumber(Integer.parseInt(corpusLine.getElement(CorpusElement.COLUMN)));
    if (corpusLine.hasElement(CorpusElement.END_ROW))
      token.setLineNumberEnd(Integer.parseInt(corpusLine.getElement(CorpusElement.END_ROW)));
    if (corpusLine.hasElement(CorpusElement.END_COLUMN))
      token.setColumnNumberEnd(Integer.parseInt(corpusLine.getElement(CorpusElement.END_COLUMN)));
    if (corpusLine.hasElement(CorpusElement.LEMMA))
      token.setOriginalLemma(corpusLine.getElement(CorpusElement.LEMMA));
    if (corpusLine.hasElement(CorpusElement.MORPHOLOGY))
      token.setOriginalMorphology(corpusLine.getElement(CorpusElement.MORPHOLOGY));
    if (corpusLine.hasElement(CorpusElement.CATEGORY))
      token.setOriginalCategory(corpusLine.getElement(CorpusElement.CATEGORY));

    return token;
  }
}
