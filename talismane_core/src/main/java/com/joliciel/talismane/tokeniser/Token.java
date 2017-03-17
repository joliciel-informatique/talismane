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
package com.joliciel.talismane.tokeniser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.tokeniser.features.TokenWrapper;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;
import com.joliciel.talismane.tokeniser.patterns.TokenPatternMatch;

/**
 * A token is a single parsing unit, which can have dependents and a governor.
 * <br/>
 * A token is more-or-less equivalent to a word, although punctuation (and in
 * some case white space) count as tokens as well.<br/>
 * Some languages may consider compound words (e.g. "of course") as a single
 * token, and some may insert empty tokens into sentences (e.g. when a single
 * token represents two POS tags downstream, such as "auquel" in French).<br/>
 * Some languages may break a single word up into multiple tokens (e.g. biblical
 * or modern Hebrew for the coordinating conjunction "ve").<br/>
 * 
 * @author Assaf Urieli
 *
 */
public class Token implements TokenWrapper {
	/**
	 * An imaginary word that can be placed at the start of the sentence to
	 * simplify algorithms.
	 */
	public static final String START_TOKEN = "[[START]]";

	private static final Logger LOG = LoggerFactory.getLogger(Token.class);
	private static Pattern whiteSpacePattern = Pattern.compile("[\\s\ufeff]+", Pattern.UNICODE_CHARACTER_CLASS);

	private String analysisText;
	private String text;
	private final String originalText;
	private String conllText;
	private int index;
	private int indexWithWhiteSpace;
	private TokenSequence tokenSequence;
	private Set<PosTag> possiblePosTags;
	private Map<PosTag, Integer> frequencies;
	private Map<String, FeatureResult<?>> featureResults = new HashMap<String, FeatureResult<?>>();
	private boolean separator;
	private final boolean whiteSpace;
	private List<TokenPatternMatch> matches = null;
	private Map<String, List<TokenPatternMatch>> matchesPerPattern = null;
	private List<TaggedToken<TokeniserOutcome>> atomicParts = new ArrayList<TaggedToken<TokeniserOutcome>>();

	private final int startIndex;
	private final int endIndex;

	private String fileName = null;
	private Integer lineNumber = null;
	private Integer columnNumber = null;
	private Integer lineNumberEnd = null;
	private Integer columnNumberEnd = null;

	private Map<PosTag, List<LexicalEntry>> lexicalEntryMap;
	private double probability = -1;
	private Map<String, TokenAttribute<?>> attributes = new HashMap<String, TokenAttribute<?>>();

	private final PosTaggerLexicon lexicon;
	private final TalismaneSession talismaneSession;

	Token(Token tokenToClone) {
		this.talismaneSession = tokenToClone.talismaneSession;
		this.analysisText = tokenToClone.analysisText;
		this.text = tokenToClone.text;
		this.originalText = tokenToClone.originalText;
		this.index = tokenToClone.index;
		this.indexWithWhiteSpace = tokenToClone.indexWithWhiteSpace;
		this.tokenSequence = tokenToClone.tokenSequence;
		this.possiblePosTags = tokenToClone.possiblePosTags;
		this.frequencies = tokenToClone.frequencies;
		this.featureResults = tokenToClone.featureResults;
		this.separator = tokenToClone.separator;
		this.whiteSpace = tokenToClone.whiteSpace;
		this.matches = tokenToClone.matches;
		this.atomicParts = tokenToClone.atomicParts;
		this.startIndex = tokenToClone.startIndex;
		this.endIndex = tokenToClone.endIndex;
		this.fileName = tokenToClone.fileName;
		this.lineNumber = tokenToClone.lineNumber;
		this.columnNumber = tokenToClone.columnNumber;
		this.attributes = tokenToClone.attributes;

		this.lexicon = tokenToClone.lexicon;
	}

	public Token(String text, TokenSequence tokenSequence, int index, int startIndex, int endIndex, PosTaggerLexicon lexicon,
			TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
		this.analysisText = null;
		this.text = null;
		this.originalText = text;
		this.tokenSequence = tokenSequence;
		this.index = index;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		if (text.length() == 0)
			this.whiteSpace = false;
		else if (whiteSpacePattern.matcher(text).matches())
			this.whiteSpace = true;
		else
			this.whiteSpace = false;

		this.lexicon = lexicon;
	}

	/**
	 * This token's text for analysis purposes. In some cases, the analysis text
	 * might group together different tokens into a single equivalence class,
	 * for example, you may wish to analyse all numbers identically by assigning
	 * the analysis text to "999".<br/>
	 * If the token's analysis text has been set, it will be returned.<br/>
	 * Else, if the token's text has been set, it will be returned.<br/>
	 * Else, the original text is returned.
	 */

	public String getAnalyisText() {
		if (analysisText != null)
			return analysisText;
		if (text != null)
			return text;
		return originalText;
	}

	public void setAnalysisText(String analysisText) {
		this.analysisText = analysisText;
	}

	/**
	 * The token's processed text, after corrections for encoding/spelling etc.
	 * <br/>
	 * If the token's text has been set, it will be returned.<br/>
	 * Else, the original text is returned.
	 * 
	 * @return
	 */
	public String getText() {
		if (text != null)
			return text;
		return originalText;
	}

	public void setText(String text) {
		this.text = text;
	}

	/**
	 * The original raw text represented by this token, i.e. before it was fixed
	 * for encoding/spelling or whatever other fixes.
	 */

	public String getOriginalText() {
		return originalText;
	}

	/**
	 * The index of this token in the containing sequence.
	 */

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * The index of this token in the containing sequence when whitespace is
	 * taken into account.
	 */

	public int getIndexWithWhiteSpace() {
		return indexWithWhiteSpace;
	}

	public void setIndexWithWhiteSpace(int indexWithWhiteSpace) {
		this.indexWithWhiteSpace = indexWithWhiteSpace;
	}

	public TokenSequence getTokenSequence() {
		return tokenSequence;
	}

	/**
	 * The token sequence containing this token.
	 */
	void setTokenSequence(TokenSequence tokenSequence) {
		this.tokenSequence = tokenSequence;
	}

	@Override
	public String toString() {
		return this.getAnalyisText();
	}

	/**
	 * A set of possible postags (assigned externally by a lexicon).
	 * 
	 * @throws TalismaneException
	 */
	public Set<PosTag> getPossiblePosTags() throws TalismaneException {
		if (possiblePosTags == null) {
			possiblePosTags = lexicon.findPossiblePosTags(this.getAnalyisText());
		}

		return possiblePosTags;
	}

	/**
	 * A list of postags and counts for this token in a training corpus
	 * (assigned externally by a statistics service).
	 */
	public Map<PosTag, Integer> getFrequencies() {
		return frequencies;
	}

	public void setFrequencies(Map<PosTag, Integer> frequencies) {
		this.frequencies = frequencies;
	}

	@SuppressWarnings("unchecked")

	public <T, Y> FeatureResult<Y> getResultFromCache(Feature<T, Y> feature, RuntimeEnvironment env) {
		FeatureResult<Y> result = null;

		String key = feature.getName() + env.getKey();
		if (this.featureResults.containsKey(key)) {
			result = (FeatureResult<Y>) this.featureResults.get(key);
		}
		return result;
	}

	public <T, Y> void putResultInCache(Feature<T, Y> feature, FeatureResult<Y> featureResult, RuntimeEnvironment env) {
		String key = feature.getName() + env.getKey();
		this.featureResults.put(key, featureResult);
	}

	/**
	 * The start index of this token within the enclosing sentence.
	 */
	public int getStartIndex() {
		return startIndex;
	}

	/**
	 * The end index of this token within the enclosing sentence.
	 */
	public int getEndIndex() {
		return endIndex;
	}

	/**
	 * Is this token a separator or not?
	 */

	public boolean isSeparator() {
		return separator;
	}

	public void setSeparator(boolean separator) {
		this.separator = separator;
	}

	/**
	 * Is this token white-space or not?
	 */

	public boolean isWhiteSpace() {
		return whiteSpace;
	}

	/**
	 * A list of TokeniserPatterns for which this token was matched, and the
	 * pattern index at which the match occurred.
	 */

	public List<TokenPatternMatch> getMatches() {
		if (matches == null)
			matches = new ArrayList<TokenPatternMatch>();
		return matches;
	}

	/**
	 * Get all matches for a given pattern.
	 */

	public List<TokenPatternMatch> getMatches(TokenPattern pattern) {
		if (matchesPerPattern == null) {
			matchesPerPattern = new HashMap<String, List<TokenPatternMatch>>();
			for (TokenPatternMatch match : this.getMatches()) {
				List<TokenPatternMatch> matchesForPattern = matchesPerPattern.get(match.getPattern().getName());
				if (matchesForPattern == null) {
					matchesForPattern = new ArrayList<TokenPatternMatch>();
					matchesPerPattern.put(match.getPattern().getName(), matchesForPattern);
				}
				matchesForPattern.add(match);
			}
		}
		return matchesPerPattern.get(pattern.getName());
	}

	public int compareTo(Token o) {
		return this.getStartIndex() - o.getStartIndex();
	}

	/**
	 * A list of atomic decisions which make up this token.
	 */

	public List<TaggedToken<TokeniserOutcome>> getAtomicParts() {
		return atomicParts;
	}

	public void setAtomicParts(List<TaggedToken<TokeniserOutcome>> atomicParts) {
		this.atomicParts = atomicParts;
	}

	/**
	 * The probability assigned to this token by the tokeniser.
	 */

	public double getProbability() {
		if (probability < 0) {
			probability = 1;
			if (this.atomicParts != null) {
				for (TaggedToken<TokeniserOutcome> atomicDecision : atomicParts) {
					probability *= atomicDecision.getDecision().getProbability();
				}
			}
		}
		return probability;
	}

	@Override
	public Token getToken() {
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((originalText == null) ? 0 : originalText.hashCode());
		result = prime * result + startIndex;
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
		Token other = (Token) obj;
		if (originalText == null) {
			if (other.originalText != null)
				return false;
		} else if (!originalText.equals(other.originalText))
			return false;
		if (startIndex != other.startIndex)
			return false;
		return true;
	}

	/**
	 * Is this an empty token (without any textual content?)
	 */

	public boolean isEmpty() {
		return this.getStartIndex() == this.getEndIndex();
	}

	/**
	 * Returns the original index in the original text at the beginning of this
	 * token.
	 */

	public int getOriginalIndex() {
		return this.getTokenSequence().getSentence().getOriginalIndex(this.startIndex);
	}

	/**
	 * Returns the original text line number at the beginning of this token.
	 */

	public int getLineNumber() {
		if (this.lineNumber == null) {
			this.lineNumber = this.getTokenSequence().getSentence().getLineNumber(this.getOriginalIndex());
		}
		return this.lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	/**
	 * Returns the original text column number (inside a line) at the beginning
	 * of this token.
	 */

	public int getColumnNumber() {
		if (this.columnNumber == null) {
			this.columnNumber = this.getTokenSequence().getSentence().getColumnNumber(this.getOriginalIndex());
		}
		return this.columnNumber;
	}

	public void setColumnNumber(int columnNumber) {
		this.columnNumber = columnNumber;
	}

	/**
	 * Returns the original text line number at the end of this token.
	 */

	public int getLineNumberEnd() {
		if (this.lineNumberEnd == null)
			this.lineNumberEnd = this.getTokenSequence().getSentence().getLineNumber(this.getOriginalIndexEnd());
		return this.lineNumberEnd;
	}

	public void setLineNumberEnd(int lineNumberEnd) {
		this.lineNumberEnd = lineNumberEnd;
	}

	/**
	 * Returns the original text column number (inside a line) at the end of
	 * this token.
	 */

	public int getColumnNumberEnd() {
		if (this.columnNumberEnd == null)
			this.columnNumberEnd = this.getTokenSequence().getSentence().getColumnNumber(this.getOriginalIndexEnd());
		return this.columnNumberEnd;
	}

	public void setColumnNumberEnd(int columnNumberEnd) {
		this.columnNumberEnd = columnNumberEnd;
	}

	/**
	 * Returns the original index in the original text at the end of this token.
	 */

	public int getOriginalIndexEnd() {
		return this.getTokenSequence().getSentence().getOriginalIndex(this.endIndex);
	}

	public String getFileName() {
		if (this.fileName == null) {
			this.fileName = this.getTokenSequence().getSentence().getFileName();
		}
		return this.fileName;
	}

	/**
	 * Returns the file name of the sentence containing this token.
	 */

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Any text that should be output as "raw" prior to outputting this token,
	 * or null if none available.
	 */

	public String getPrecedingRawOutput() {
		Sentence sentence = this.getTokenSequence().getSentence();
		int prevStart = -1;
		if (this.index > 0) {
			Token previousToken = this.getTokenSequence().get(index - 1);
			prevStart = previousToken.getStartIndex();
		}
		String rawOutput = null;
		if (startIndex > prevStart)
			rawOutput = sentence.getRawInput(prevStart, startIndex);
		if (LOG.isTraceEnabled())
			LOG.trace(rawOutput);
		return rawOutput;
	}

	/**
	 * Any text that should be output as "raw" after outputting this token, or
	 * null if none available. Will only ever return non-null for the final
	 * token in a sequence.
	 */

	public String getTrailingRawOutput() {
		String rawOutput = null;
		if (this.index == this.getTokenSequence().size() - 1) {
			Sentence sentence = this.getTokenSequence().getSentence();
			rawOutput = sentence.getRawInput(startIndex, Integer.MAX_VALUE - 1);
			if (LOG.isTraceEnabled())
				LOG.trace(rawOutput);
		}
		return rawOutput;
	}

	/**
	 * Make a shallow clone of this token.
	 */

	public Token cloneToken() {
		Token token = new Token(this);
		return token;
	}

	/**
	 * The original text as encoded for the CoNLL output format.
	 */

	public String getTextForCoNLL() {
		if (conllText == null) {
			conllText = talismaneSession.getCoNLLFormatter().toCoNLL(originalText);
		}
		return conllText;
	}

	/**
	 * The "best" lexical entry for this token/postag combination if one exists,
	 * or null otherwise.
	 */

	public LexicalEntry getLexicalEntry(PosTag posTag) {
		if (this.lexicalEntryMap == null) {
			this.lexicalEntryMap = new HashMap<PosTag, List<LexicalEntry>>();
		}
		List<LexicalEntry> lexicalEntries = this.lexicalEntryMap.get(posTag);
		if (lexicalEntries == null) {
			lexicalEntries = lexicon.findLexicalEntries(this.getText(), posTag);
			this.lexicalEntryMap.put(posTag, lexicalEntries);
		}
		LexicalEntry bestEntry = null;
		if (lexicalEntries.size() > 0)
			bestEntry = lexicalEntries.get(0);
		return bestEntry;
	}

	/**
	 * Any attributes assigned to this token (e.g. telling downstream systems
	 * not to stem this token in a search index, in the case of a recognised
	 * acronym).
	 */

	public Map<String, TokenAttribute<?>> getAttributes() {
		return attributes;
	}

	/**
	 * Add an attribute of a given type.
	 */

	public <T extends Serializable> void addAttribute(String key, TokenAttribute<T> value) {
		if (!attributes.containsKey(key))
			attributes.put(key, value);
	}

}
