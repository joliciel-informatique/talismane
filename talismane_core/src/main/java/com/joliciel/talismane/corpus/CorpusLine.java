///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2017 Joliciel Informatique
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
package com.joliciel.talismane.corpus;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalEntry;

/**
 * Represents one line in an annotated corpus, corresponding to a single token.
 * 
 * @author Assaf Urieli
 *
 */
public class CorpusLine {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(CorpusLine.class);
  private final String line;
  private final int lineNumber;
  private final Map<CorpusElement, String> elements = new HashMap<>();
  private LexicalEntry lexicalEntry;

  public enum CorpusElement {
    /**
     * A unique index for a given token, starting at 1.
     */
    INDEX("(\\d+)"),
    /**
     * The token - note that we assume CoNLL formatting based on
     * {@link TalismaneSession#getCoNLLFormatter()}
     */
    TOKEN("(.*?)"),
    /**
     * The lemma - note that we assume CoNLL formatting based on
     * {@link TalismaneSession#getCoNLLFormatter()}
     */
    LEMMA("(.*?)"),
    /**
     * The token's pos-tag
     */
    POSTAG("(.+?)"),
    /**
     * The token's category
     */
    CATEGORY("(.*?)"),
    /**
     * The token's morphological traits.
     */
    MORPHOLOGY("(.*?)"),
    /**
     * The dependency label governing this token
     */
    NON_PROJ_LABEL("(.*?)"),
    /**
     * The index of the token governing this token. A value of 0 indicates an
     * invisible "root" token as a governor
     */
    NON_PROJ_GOVERNOR("(\\d+)"),
    /**
     * The dependency label governing this token when the full tree has been
     * made projective.
     */
    LABEL("(.*?)"),
    /**
     * This index of the token governing this token when the full tree has been
     * made projective. A value of 0 indicates an invisible "root" token as a
     * governor.
     */
    GOVERNOR("(\\d+)"),
    /**
     * The file containing the original token
     */
    FILENAME("(.*?)"),
    /**
     * The row containing the token
     */
    ROW("(\\d+)"),
    /**
     * The column on which the token starts
     */
    COLUMN("(\\d+)"),
    /**
     * The row containing the token's end
     */
    END_ROW("(\\d+)"),
    /**
     * The column just after the token end
     */
    END_COLUMN("(\\d+)"),
    /**
     * An arbitrary comment added to the pos-tag.
     */
    POSTAG_COMMENT("(.*?)"),
    /**
     * An arbitrary comment added to the dependency arc.
     */
    DEP_COMMENT("(.*?)"),
    /**
     * Output that comes before this token.
     */
    PRECEDING_RAW_OUTPUT("(.*?)"),
    /**
     * Output that follows this token.
     */
    TRAILING_RAW_OUTPUT("(.*?)"),
    /**
     * The probability for this tokenisation decision.
     */
    TOKEN_PROB("(.*?)"),
    /**
     * The probability for this pos-tagging decision.
     */
    POSTAG_PROB("(.*?)"),
    /**
     * The probability for this parsing decision.
     */
    PARSE_PROB("(.*?)");

    private final String replacement;

    private CorpusElement(String replacement) {
      this.replacement = replacement;
    }

    public String getReplacement() {
      return replacement;
    }
  }

  public CorpusLine(String line, int lineNumber) {
    this.line = line;
    this.lineNumber = lineNumber;
  }

  private CorpusLine(CorpusLine from) {
    this.line = from.line;
    this.lineNumber = from.lineNumber;
    this.lexicalEntry = from.lexicalEntry;
    for (CorpusElement key : from.elements.keySet()) {
      this.elements.put(key, from.elements.get(key));
    }
  }

  /**
   * Get a particular element from this corpus line.
   */
  public String getElement(CorpusElement type) {
    return this.elements.get(type);
  }

  public void setElement(CorpusElement type, String value) {
    this.elements.put(type, value);
  }

  public boolean hasElement(CorpusElement type) {
    return this.elements.containsKey(type);
  }

  public Map<CorpusElement, String> getElements() {
    return elements;
  }

  /**
   * Get the lexical entry extracted from this line's elements, including the
   * morphology.
   */
  public LexicalEntry getLexicalEntry() {
    return lexicalEntry;
  }

  public void setLexicalEntry(LexicalEntry lexicalEntry) {
    this.lexicalEntry = lexicalEntry;
  }

  @Override
  public String toString() {
    return "CorpusLine [line=" + line + ", lineNumber=" + lineNumber + "]";
  }

  /**
   * The original line out of which the corpus line was extracted.
   * 
   * @return
   */
  public String getLine() {
    return line;
  }

  /**
   * The line number of the original line in the corpus.
   */
  public int getLineNumber() {
    return lineNumber;
  }

  public int getIndex() {
    if (this.hasElement(CorpusElement.INDEX)) {
      return Integer.parseInt(this.getElement(CorpusElement.INDEX));
    }
    return -1;
  }

  public void setIndex(int index) {
    this.setElement(CorpusElement.INDEX, "" + index);
  }

  public String getToken() {
    return this.getElement(CorpusElement.TOKEN);
  }

  public void setToken(String token) {
    this.setElement(CorpusElement.TOKEN, token);
  }

  public String getLemma() {
    return this.getElement(CorpusElement.LEMMA);
  }

  public void setLemma(String lemma) {
    this.setElement(CorpusElement.LEMMA, lemma);
  }

  public String getPosTag() {
    return this.getElement(CorpusElement.POSTAG);
  }

  public void setPosTag(String posTag) {
    this.setElement(CorpusElement.POSTAG, posTag);
  }

  public String getCategory() {
    return this.getElement(CorpusElement.CATEGORY);
  }

  public void setCategory(String category) {
    this.setElement(CorpusElement.CATEGORY, category);
  }

  public String getMorphology() {
    return this.getElement(CorpusElement.MORPHOLOGY);
  }

  public void setMorphology(String morphology) {
    this.setElement(CorpusElement.MORPHOLOGY, morphology);
  }

  public int getGovernorIndex() {
    if (this.hasElement(CorpusElement.GOVERNOR)) {
      return Integer.parseInt(this.getElement(CorpusElement.GOVERNOR));
    }
    return -1;
  }

  public void setGovernorIndex(int governorIndex) {
    this.setElement(CorpusElement.GOVERNOR, "" + governorIndex);
  }

  public String getLabel() {
    return this.getElement(CorpusElement.LABEL);
  }

  public void setLabel(String label) {
    this.setElement(CorpusElement.LABEL, label);
  }

  public int getNonProjGovernorIndex() {
    if (this.hasElement(CorpusElement.NON_PROJ_GOVERNOR)) {
      return Integer.parseInt(this.getElement(CorpusElement.NON_PROJ_GOVERNOR));
    }
    return -1;
  }

  public void setNonProjGovernorIndex(int governorIndex) {
    this.setElement(CorpusElement.NON_PROJ_GOVERNOR, "" + governorIndex);
  }

  public String getNonProjLabel() {
    return this.getElement(CorpusElement.NON_PROJ_LABEL);
  }

  public void setNonProjLabel(String label) {
    this.setElement(CorpusElement.NON_PROJ_LABEL, label);
  }

  public String getPrecedingRawOutput() {
    return this.getElement(CorpusElement.PRECEDING_RAW_OUTPUT);
  }

  public String getTrailingRawOutput() {
    return this.getElement(CorpusElement.TRAILING_RAW_OUTPUT);
  }

  public void setTokenProbability(double tokenProb) {
    this.setElement(CorpusElement.TOKEN_PROB, String.format("%.2f", tokenProb));
  }

  public double getTokenProbability() {
    if (this.hasElement(CorpusElement.TOKEN_PROB)) {
      return Double.parseDouble(this.getElement(CorpusElement.TOKEN_PROB));
    }
    return -1;
  }

  public void setPosTagProbability(double posTagProb) {
    this.setElement(CorpusElement.POSTAG_PROB, String.format("%.2f", posTagProb));
  }

  public double getPosTagProbability() {
    if (this.hasElement(CorpusElement.POSTAG_PROB)) {
      return Double.parseDouble(this.getElement(CorpusElement.POSTAG_PROB));
    }
    return -1;
  }

  public void setParseProbability(double parseProb) {
    this.setElement(CorpusElement.PARSE_PROB, String.format("%.2f", parseProb));
  }

  public double getParseProbability() {
    if (this.hasElement(CorpusElement.PARSE_PROB)) {
      return Double.parseDouble(this.getElement(CorpusElement.PARSE_PROB));
    }
    return -1;
  }

  public CorpusLine cloneCorpusLine() {
    return new CorpusLine(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((elements == null) ? 0 : elements.hashCode());
    result = prime * result + ((line == null) ? 0 : line.hashCode());
    result = prime * result + lineNumber;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    CorpusLine other = (CorpusLine) obj;
    if (elements == null) {
      if (other.elements != null)
        return false;
    } else if (!elements.equals(other.elements))
      return false;
    if (line == null) {
      if (other.line != null)
        return false;
    } else if (!line.equals(other.line))
      return false;
    if (lineNumber != other.lineNumber)
      return false;
    return true;
  }

}
