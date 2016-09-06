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
package com.joliciel.talismane.fr.ftb.export;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.fr.ftb.PhraseUnit;
import com.joliciel.talismane.fr.ftb.Sentence;
import com.joliciel.talismane.fr.ftb.TreebankReader;
import com.joliciel.talismane.fr.ftb.TreebankService;
import com.joliciel.talismane.fr.ftb.util.CSVFormatter;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterWrapper;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * A token reader for the French Treebank corpus.
 * 
 * @author Assaf Urieli
 *
 */
class FrenchTreebankTokenReader implements TokeniserAnnotatedCorpusReader, PosTagAnnotatedCorpusReader {
	private static final Logger LOG = LoggerFactory.getLogger(FrenchTreebankTokenReader.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(FrenchTreebankTokenReader.class);

	private TreebankService treebankService;

	private FtbPosTagMapper ftbPosTagMapper;
	private Writer csvFileErrorWriter = null;
	private boolean ignoreCase = true;
	private List<TokenSequenceFilter> tokenSequenceFilters = new ArrayList<TokenSequenceFilter>();
	private List<PosTagSequenceFilter> posTagSequenceFilters = new ArrayList<PosTagSequenceFilter>();
	private List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();
	private final TreebankReader treebankReader;
	private TokenSequenceFilter tokenFilterWrapper = null;
	private int maxSentenceCount = 0;
	private int startSentence = 0;
	private int sentenceCount = 0;
	private int includeIndex = -1;
	private int excludeIndex = -1;
	private int crossValidationSize = 0;

	private PosTagSequence currentSentence = null;

	private boolean useCompoundPosTags = false;
	private final TalismaneSession talismaneSession;

	public FrenchTreebankTokenReader(TreebankReader treebankReader, TalismaneSession talismaneSession) {
		this.treebankReader = treebankReader;
		this.talismaneSession = talismaneSession;
	}

	@Override
	public boolean hasNextPosTagSequence() {
		if (maxSentenceCount > 0 && sentenceCount >= maxSentenceCount) {
			return false;
		} else {
			while (currentSentence == null) {
				if (!treebankReader.hasNextSentence()) {
					break;
				}

				currentSentence = this.nextSentenceInternal();
				boolean includeMe = true;

				// check cross-validation
				if (crossValidationSize > 0) {
					if (includeIndex >= 0) {
						if (sentenceCount % crossValidationSize != includeIndex) {
							includeMe = false;
						}
					} else if (excludeIndex >= 0) {
						if (sentenceCount % crossValidationSize == excludeIndex) {
							includeMe = false;
						}
					}
				}

				if (startSentence > sentenceCount) {
					includeMe = false;
				}

				sentenceCount++;

				if (!includeMe) {
					currentSentence = null;
					continue;
				}

			}
		}
		return currentSentence != null;
	}

	@Override
	public boolean hasNextTokenSequence() {
		return this.hasNextPosTagSequence();
	}

	@Override
	public PosTagSequence nextPosTagSequence() {
		if (this.ftbPosTagMapper == null) {
			throw new RuntimeException("Cannot get next PosTagSequence without PosTagMapper");
		}
		PosTagSequence posTagSequence = currentSentence;
		currentSentence = null;
		return posTagSequence;
	}

	@Override
	public TokenSequence nextTokenSequence() {
		PosTagSequence posTagSequence = currentSentence;
		currentSentence = null;
		return posTagSequence.getTokenSequence();
	}

	PosTagSequence nextSentenceInternal() {
		MONITOR.startTask("nextSentenceInternal");
		try {
			Sentence sentence = treebankReader.nextSentence();
			LOG.debug("Sentence " + sentence.getSentenceNumber());
			List<Integer> tokenSplits = new ArrayList<Integer>();
			PosTagSet posTagSet = talismaneSession.getPosTagSet();

			String text = sentence.getText();
			// get rid of duplicate white space
			Pattern duplicateWhiteSpace = Pattern.compile("\\s[\\s]+");
			text = duplicateWhiteSpace.matcher(text).replaceAll(" ");

			// there's no guarantee that the phrase units align to the original
			// sentence text
			// given the issues we had for aligning sentences in the first place
			List<PhraseUnit> phraseUnits = sentence.getAllPhraseUnits();
			LOG.trace("Phrase units: " + phraseUnits.size());
			Pattern separators = Tokeniser.SEPARATORS;
			Pattern whitespace = Pattern.compile("\\s+");

			Matcher matcher = separators.matcher(text);
			List<String> allTokens = new ArrayList<String>();
			int currentPos = 0;
			while (matcher.find()) {
				if (matcher.start() > currentPos) {
					String leftoverToken = text.substring(currentPos, matcher.start());
					allTokens.add(leftoverToken);
				}
				String token = text.substring(matcher.start(), matcher.end());
				allTokens.add(token);
				currentPos = matcher.end();
			}
			if (currentPos < text.length())
				allTokens.add(text.substring(currentPos));

			com.joliciel.talismane.filters.Sentence oneSentence = new com.joliciel.talismane.filters.Sentence(text, talismaneSession);
			TokenSequence tokenSequence = new TokenSequence(oneSentence, talismaneSession);
			List<PosTaggedToken> posTaggedTokens = new ArrayList<PosTaggedToken>();

			PhraseUnitReader phraseUnitReader = new ComplexPhraseUnitReaderWithEmptyTokens(phraseUnits);

			phraseUnitReader.setTreebankService(treebankService);
			if (ftbPosTagMapper != null)
				phraseUnitReader.setFtbPosTagMapper(ftbPosTagMapper);

			String phraseUnitText = phraseUnitReader.nextString();

			LOG.trace("phrase unit: " + phraseUnitText);
			currentPos = 0;
			int lastSplit = 0;
			tokenSplits.add(0);

			while (phraseUnitText != null && phraseUnitText.length() == 0) {
				tokenSplits.add(currentPos);
				Token aToken = tokenSequence.addEmptyToken(currentPos);
				PosTag posTag = phraseUnitReader.getPosTag();
				Decision corpusDecision = new Decision(posTag.getCode());

				PosTaggedToken posTaggedToken = new PosTaggedToken(aToken, corpusDecision, talismaneSession);
				posTaggedTokens.add(posTaggedToken);
				phraseUnitText = phraseUnitReader.nextString();
			}
			boolean inPhraseUnit = false;
			boolean addEmptyTokenBeforeNextToken = false;
			PosTag emptyTokenPosTag = null;
			for (String token : allTokens) {
				if (LOG.isTraceEnabled())
					LOG.trace("token: " + token);
				currentPos += token.length();
				if ((!ignoreCase && phraseUnitText.equals(token)) || (ignoreCase && phraseUnitText.equalsIgnoreCase(token))) {
					// exact match

					if (addEmptyTokenBeforeNextToken) {
						if (LOG.isTraceEnabled())
							LOG.trace("Adding empty token at " + (currentPos - token.length()));
						tokenSplits.add((currentPos - token.length()));
						Token emptyToken = tokenSequence.addEmptyToken((currentPos - token.length()));
						Decision emptyTokenDecision = new Decision(emptyTokenPosTag.getCode());
						PosTaggedToken posTaggedToken2 = new PosTaggedToken(emptyToken, emptyTokenDecision, talismaneSession);
						posTaggedTokens.add(posTaggedToken2);
						addEmptyTokenBeforeNextToken = false;
					}

					if (LOG.isTraceEnabled())
						LOG.trace("Adding split " + currentPos);
					tokenSplits.add(currentPos);

					Token aToken = tokenSequence.addToken(lastSplit, currentPos);
					PosTag posTag = phraseUnitReader.getPosTag();
					Decision corpusDecision = new Decision(posTag.getCode());
					PosTaggedToken posTaggedToken = new PosTaggedToken(aToken, corpusDecision, talismaneSession);
					posTaggedTokens.add(posTaggedToken);

					lastSplit = currentPos;
					phraseUnitText = phraseUnitReader.nextString();
					if (LOG.isTraceEnabled())
						LOG.trace("phrase unit: " + phraseUnitText);
					while (phraseUnitText != null && phraseUnitText.length() == 0) {
						Token emptyToken = null;
						emptyTokenPosTag = phraseUnitReader.getPosTag();
						phraseUnitText = phraseUnitReader.nextString();
						if (LOG.isTraceEnabled())
							LOG.trace("phrase unit: " + phraseUnitText);

						// Empty tokens need to be attached either to the right
						// (auquel, duquel)
						// or to the left (du, des)
						if (phraseUnitText.equals("duquel") || phraseUnitText.equals("auquel") || phraseUnitText.equals("desquels")
								|| phraseUnitText.equals("auxquels") || phraseUnitText.equals("desquelles") || phraseUnitText.equals("auxquelles")) {
							// attach empty token to the "duquel" that follows
							// it
							addEmptyTokenBeforeNextToken = true;
						} else {
							if (LOG.isTraceEnabled())
								LOG.trace("Adding empty token at " + currentPos);
							tokenSplits.add(currentPos);
							emptyToken = tokenSequence.addEmptyToken(currentPos);
							Decision emptyTokenDecision = new Decision(emptyTokenPosTag.getCode());
							PosTaggedToken posTaggedToken2 = new PosTaggedToken(emptyToken, emptyTokenDecision, talismaneSession);
							posTaggedTokens.add(posTaggedToken2);
						}
					}
					inPhraseUnit = false;
				} else if (phraseUnitText.length() >= token.length() && ((!ignoreCase && phraseUnitText.substring(0, token.length()).equals(token))
						|| (ignoreCase && phraseUnitText.substring(0, token.length()).equalsIgnoreCase(token)))) {
					// the current phrase unit text starts with this token
					phraseUnitText = phraseUnitText.substring(token.length());
					if (LOG.isTraceEnabled())
						LOG.trace("phrase unit: " + phraseUnitText);
					inPhraseUnit = true;
				} else if (token.length() == 1 && whitespace.matcher(token).matches()) {
					// white space, always add split unless we're already inside
					// white space
					if (!inPhraseUnit) {
						if (LOG.isTraceEnabled())
							LOG.trace("Adding split " + currentPos);
						tokenSplits.add(currentPos);
						tokenSequence.addToken(lastSplit, currentPos);
						lastSplit = currentPos;
					}
				} else {
					// non-white space, what to do? either we skip the token, or
					// we skip the phrase unit!
					// for now let's assume it never happens and see what
					// results!
					int pos = 0;
					StringBuilder sb = new StringBuilder();
					for (int split : tokenSplits) {
						String aToken = text.substring(pos, split);
						sb.append('|');
						sb.append(aToken);
						pos = split;
					}
					LOG.info(sb.toString());
					LOG.info("File: " + sentence.getFile().getFileName());
					LOG.info("Sentence: " + text);
					if (csvFileErrorWriter != null) {
						try {
							csvFileErrorWriter.write(CSVFormatter.format(phraseUnitText) + ",");
							for (String info : phraseUnitReader.getCurrentInfo())
								csvFileErrorWriter.write(CSVFormatter.format(info) + ",");
							csvFileErrorWriter.write(CSVFormatter.format(token) + ",");
							csvFileErrorWriter.write(sentence.getFile().getFileName() + ",");
							csvFileErrorWriter.write(sentence.getSentenceNumber() + ",");
							csvFileErrorWriter.write(CSVFormatter.format(sentence.getText()) + ",");
							csvFileErrorWriter.write("\n");
							csvFileErrorWriter.flush();
						} catch (IOException ioe) {
							throw new RuntimeException(ioe);
						}
						break;
					} else {
						// instead of throwing an error, write these to a file
						// (or do both)
						// so we can catch them all in one fell swoop
						throw new RuntimeException("Unexpected text: " + token);
					}
				}
			}
			if (lastSplit < currentPos) {
				tokenSplits.add(currentPos);
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug(text);

				int pos = 0;
				StringBuilder sb = new StringBuilder();
				for (int split : tokenSplits) {
					String aToken = text.substring(pos, split);
					sb.append('|');
					sb.append(aToken);
					pos = split;
				}
				LOG.debug(sb.toString());
			}

			for (TokenSequenceFilter tokenSequenceFilter : this.tokenSequenceFilters) {
				if (LOG.isTraceEnabled())
					LOG.trace("Applying filter: " + tokenSequenceFilter.getClass().getSimpleName());
				tokenSequenceFilter.apply(tokenSequence);
			}

			if (tokenFilterWrapper == null) {
				tokenFilterWrapper = new TokenFilterWrapper(this.tokenFilters);
			}
			tokenFilterWrapper.apply(tokenSequence);

			tokenSequence.finalise();

			PosTagSequence posTagSequence = new PosTagSequence(tokenSequence);
			int i = 0;
			for (Token token : tokenSequence) {
				if (LOG.isTraceEnabled())
					LOG.trace("Token : \"" + token.getText() + "\" (was \"" + token.getOriginalText() + "\")");
				PosTaggedToken posTaggedToken = posTaggedTokens.get(i);
				if (token.equals(posTaggedToken.getToken())) {
					posTagSequence.addPosTaggedToken(posTaggedToken);
					i++;
				} else if (token.getStartIndex() == token.getEndIndex()) {
					LOG.debug("Adding null pos tag at position " + token.getStartIndex());
					Decision nullPosTagDecision = new Decision(PosTag.NULL_POS_TAG.getCode());
					PosTaggedToken emptyTagToken = new PosTaggedToken(token, nullPosTagDecision, talismaneSession);
					posTagSequence.addPosTaggedToken(emptyTagToken);
				} else {
					throw new RuntimeException("Expected only empty tokens added. Postag Token = " + posTaggedToken.getToken().getText() + ", start: "
							+ token.getStartIndex() + ", end:" + token.getEndIndex());
				}
			}

			if (useCompoundPosTags) {
				PosTagSequence newSequence = new PosTagSequence(tokenSequence);
				PosTaggedToken lastPosTaggedToken = null;
				i = 0;
				for (PosTaggedToken posTaggedToken : posTagSequence) {
					boolean removed = false;
					if (posTaggedToken.getToken().isEmpty()) {
						String lastWord = "";
						if (lastPosTaggedToken != null)
							lastWord = lastPosTaggedToken.getToken().getOriginalText().toLowerCase();
						if (lastWord.equals("des") || lastWord.equals("du") || lastWord.equals("aux") || lastWord.equals("au") || lastWord.endsWith(" des")
								|| lastWord.endsWith(" du") || lastWord.endsWith(" aux") || lastWord.endsWith(" au") || lastWord.endsWith("'aux")
								|| lastWord.endsWith("'au")) {
							if (lastWord.equals("des") || lastWord.equals("du") || lastWord.equals("aux") || lastWord.equals("au")) {
								if (lastPosTaggedToken.getTag().getCode().equals("P")) {
									lastPosTaggedToken.setTag(posTagSet.getPosTag("P+D"));
									lastPosTaggedToken.getToken().setText(lastWord);
								}
							}
							posTaggedToken.setTag(PosTag.NULL_POS_TAG);
							tokenSequence.removeEmptyToken(posTaggedToken.getToken());
							removed = true;
						} else if (i == posTagSequence.size() - 1) {
							// last token in sequence
							// need to remove it now, since it won't get removed
							// in the next iteration
							tokenSequence.removeEmptyToken(posTaggedToken.getToken());
							removed = true;
						}
					} else {
						newSequence.addPosTaggedToken(posTaggedToken);
					}
					if (lastPosTaggedToken != null && lastPosTaggedToken.getToken().isEmpty()) {
						String word = posTaggedToken.getToken().getOriginalText().toLowerCase();
						if (word.equals("duquel") || word.equals("desquels") || word.equals("desquelles") || word.equals("auquel") || word.equals("auxquels")
								|| word.equals("auxquelles")) {
							posTaggedToken.setTag(posTagSet.getPosTag("P+PRO"));
							lastPosTaggedToken.setTag(PosTag.NULL_POS_TAG);
							posTaggedToken.getToken().setText(word);
							tokenSequence.removeEmptyToken(lastPosTaggedToken.getToken());
						} else if (word.equals("dudit")) {
							posTaggedToken.setTag(posTagSet.getPosTag("P+D"));
							lastPosTaggedToken.setTag(PosTag.NULL_POS_TAG);
							posTaggedToken.getToken().setText(word);
							tokenSequence.removeEmptyToken(lastPosTaggedToken.getToken());
						} else {
							LOG.info("Not expecting empty token here (index " + lastPosTaggedToken.getToken().getIndex() + ", next token = " + word + "): "
									+ posTagSequence);
							lastPosTaggedToken.setTag(PosTag.NULL_POS_TAG);
							tokenSequence.removeEmptyToken(lastPosTaggedToken.getToken());
						}
					}
					if (!removed)
						lastPosTaggedToken = posTaggedToken;
					i++;
				}
				posTagSequence = newSequence;
				tokenSequence.finalise();
			}
			for (PosTagSequenceFilter posTagSequenceFilter : this.posTagSequenceFilters) {
				posTagSequenceFilter.apply(posTagSequence);
			}

			return posTagSequence;
		} finally {
			MONITOR.endTask();
		}
	}

	public TreebankService getTreebankService() {
		return treebankService;
	}

	public void setTreebankService(TreebankService treebankService) {
		this.treebankService = treebankService;
	}

	public Writer getCsvFileErrorWriter() {
		return csvFileErrorWriter;
	}

	public void setCsvFileErrorWriter(Writer csvFileErrorWriter) {
		this.csvFileErrorWriter = csvFileErrorWriter;
	}

	public boolean isIgnoreCase() {
		return ignoreCase;
	}

	public void setIgnoreCase(boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
	}

	@Override
	public void addTokenSequenceFilter(TokenSequenceFilter tokenFilter) {
		this.tokenSequenceFilters.add(tokenFilter);
	}

	@Override
	public Map<String, String> getCharacteristics() {
		Map<String, String> characteristics = new HashMap<String, String>();

		characteristics.put("treebankReader", treebankReader.getClass().getSimpleName());
		characteristics.putAll(this.treebankReader.getCharacteristics());
		if (ftbPosTagMapper != null)
			characteristics.put("posTagSet", "" + ftbPosTagMapper.getPosTagSet().getName());
		characteristics.put("ignoreCase", "" + ignoreCase);

		int i = 0;
		for (TokenSequenceFilter tokenSequenceFilter : this.tokenSequenceFilters) {
			characteristics.put("filter" + i, "" + tokenSequenceFilter.getClass().getSimpleName());

			i++;
		}
		return characteristics;
	}

	public FtbPosTagMapper getFtbPosTagMapper() {
		return ftbPosTagMapper;
	}

	public void setFtbPosTagMapper(FtbPosTagMapper ftbPosTagMapper) {
		this.ftbPosTagMapper = ftbPosTagMapper;
	}

	@Override
	public void addTokenFilter(TokenFilter tokenFilter) {
		this.tokenFilters.add(tokenFilter);
	}

	@Override
	public List<TokenSequenceFilter> getTokenSequenceFilters() {
		return this.tokenSequenceFilters;
	}

	@Override
	public List<TokenFilter> getTokenFilters() {
		return this.tokenFilters;
	}

	@Override
	public void addPosTagSequenceFilter(PosTagSequenceFilter posTagSequenceFilter) {
		this.posTagSequenceFilters.add(posTagSequenceFilter);
	}

	@Override
	public int getMaxSentenceCount() {
		return maxSentenceCount;
	}

	@Override
	public void setMaxSentenceCount(int maxSentenceCount) {
		this.maxSentenceCount = maxSentenceCount;
	}

	public boolean isUseCompoundPosTags() {
		return useCompoundPosTags;
	}

	public void setUseCompoundPosTags(boolean useCompoundPosTags) {
		this.useCompoundPosTags = useCompoundPosTags;
	}

	@Override
	public int getIncludeIndex() {
		return includeIndex;
	}

	@Override
	public void setIncludeIndex(int includeIndex) {
		this.includeIndex = includeIndex;
	}

	@Override
	public int getExcludeIndex() {
		return excludeIndex;
	}

	@Override
	public void setExcludeIndex(int excludeIndex) {
		this.excludeIndex = excludeIndex;
	}

	@Override
	public int getCrossValidationSize() {
		return crossValidationSize;
	}

	@Override
	public void setCrossValidationSize(int crossValidationSize) {
		this.crossValidationSize = crossValidationSize;
	}

	@Override
	public int getStartSentence() {
		return startSentence;
	}

	@Override
	public void setStartSentence(int startSentence) {
		this.startSentence = startSentence;
	}

}
