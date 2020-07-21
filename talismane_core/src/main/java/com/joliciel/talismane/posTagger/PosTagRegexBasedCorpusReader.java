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
package com.joliciel.talismane.posTagger;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.corpus.CorpusLine;
import com.joliciel.talismane.corpus.CorpusLine.CorpusElement;
import com.joliciel.talismane.corpus.CorpusLineReader;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenRegexBasedCorpusReader;
import com.typesafe.config.Config;

/**
 * A corpus reader that expects one pos-tagged token per line, and analyses the
 * line content based on a regex supplied during construction, via a
 * {@link CorpusLineReader}.<br/>
 * 
 * The following placeholders are required:<br/>
 * {@link CorpusElement#TOKEN}, {@link CorpusElement#POSTAG}. <br/>
 * These are included surrounded by % signs on both sides, and without the
 * prefix "CorpusElement."<br/>
 * 
 * Example (note that the regex is applied to one line, so no endline is
 * necessary):
 * 
 * <pre>
 * .*\t%TOKEN%\t.*\t%POSTAG%\t.*\t.*\t
 * </pre>
 * 
 * @author Assaf Urieli
 *
 */
public class PosTagRegexBasedCorpusReader extends TokenRegexBasedCorpusReader implements PosTagAnnotatedCorpusReader {
  private static final Logger LOG = LoggerFactory.getLogger(PosTagRegexBasedCorpusReader.class);

  protected PosTagSequence posTagSequence = null;
  protected Map<Integer, PosTaggedToken> idTokenMap = new HashMap<>();

  /**
   * Reads the values described in
   * {@link TokenRegexBasedCorpusReader#TokenRegexBasedCorpusReader(Reader, Config, String)}
   */
  public PosTagRegexBasedCorpusReader(Reader reader, Config config, String sessionId)
      throws IOException, TalismaneException, ReflectiveOperationException {
    super(reader, config, sessionId);
  }

  @Override
  protected CorpusElement[] getRequiredElements() {
    return new CorpusElement[] { CorpusElement.TOKEN, CorpusElement.POSTAG };
  }

  @Override
  protected void processSentence(Sentence sentence, List<CorpusLine> corpusLines) throws TalismaneException, IOException {
    try {
      super.processSentence(sentence, corpusLines);
      posTagSequence = new PosTagSequence(tokenSequence);
      int i = 0;
      for (CorpusLine corpusLine : corpusLines) {
        PosTaggedToken posTaggedToken = this.convertToPosTaggedToken(corpusLine, posTagSequence, i++, this.getCurrentFile());
        this.idTokenMap.put(corpusLine.getIndex(), posTaggedToken);
      }
    } catch (TalismaneException e) {
      this.clearSentence();
      throw e;
    }
  }

  @Override
  public PosTagSequence nextPosTagSequence() throws TalismaneException, IOException {
    PosTagSequence nextSentence = null;
    if (this.hasNextSentence()) {
      nextSentence = posTagSequence;
      this.clearSentence();
    }
    return nextSentence;
  }

  @Override
  protected void clearSentence() {
    super.clearSentence();
    this.posTagSequence = null;
    this.idTokenMap = new HashMap<>();
  }

  @Override
  public Map<String, String> getCharacteristics() {
    Map<String, String> attributes = super.getCharacteristics();
    attributes.put("tagset", TalismaneSession.get(sessionId).getPosTagSet().getName());
    return attributes;
  }

  protected PosTaggedToken convertToPosTaggedToken(CorpusLine corpusLine, PosTagSequence posTagSequence, int index, File currentFile)
      throws TalismaneException {
    Token token = posTagSequence.getTokenSequence().get(index);

    PosTagSet posTagSet = TalismaneSession.get(sessionId).getPosTagSet();
    PosTag posTag = null;
    try {
      posTag = posTagSet.getPosTag(corpusLine.getElement(CorpusElement.POSTAG));
    } catch (UnknownPosTagException upte) {
      String fileName = "";
      if (currentFile != null)
        fileName = currentFile.getPath();

      throw new TalismaneException(
          "Unknown posTag, " + fileName + ", on line " + corpusLine.getLineNumber() + ": " + corpusLine.getElement(CorpusElement.POSTAG));
    }
    Decision posTagDecision = new Decision(posTag.getCode());
    PosTaggedToken posTaggedToken = new PosTaggedToken(token, posTagDecision, sessionId);
    if (LOG.isTraceEnabled()) {
      LOG.trace(posTaggedToken.toString());
    }

    if (corpusLine.hasElement(CorpusElement.POSTAG_COMMENT))
      posTaggedToken.setComment(corpusLine.getElement(CorpusElement.POSTAG_COMMENT));

    // set the lexical entry if we have one
    if (corpusLine.getLexicalEntry() != null) {
      List<LexicalEntry> lexicalEntrySet = new ArrayList<>(1);
      lexicalEntrySet.add(corpusLine.getLexicalEntry());
      posTaggedToken.setLexicalEntries(lexicalEntrySet);
    }
    posTagSequence.addPosTaggedToken(posTaggedToken);
    return posTaggedToken;
  }
}
