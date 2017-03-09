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
package com.joliciel.talismane.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.LinguisticRules;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.CompactLexicalEntry;
import com.joliciel.talismane.lexicon.CompactLexicalEntrySupport;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.LexicalEntryReader;
import com.joliciel.talismane.lexicon.WritableLexicalEntry;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.UnknownPosTagException;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.sentenceAnnotators.SentenceAnnotator;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.PretokenisedSequence;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.io.CurrentFileObserver;
import com.typesafe.config.Config;

/**
 * A corpus reader that expects one pos-tagged token with dependency info per
 * line, and analyses the line content based on a regex supplied during
 * construction. The regex needs to contain the following five capturing groups,
 * indicated by the following strings:<br/>
 * <li>%INDEX%: a unique index for a given token (typically just a sequential
 * index)</li>
 * <li>%TOKEN%: the token - note that we assume CoNLL formatting (with
 * underscores for spaces and for empty tokens). The sequence &amp;und; should
 * be used for true underscores.</li>
 * <li>%POSTAG%: the token's pos-tag</li>
 * <li>%LABEL%: the dependency label governing this token</li>
 * <li>%GOVERNOR%: the index of the token governing this token - a value of 0
 * indicates an invisible "root" token as a governor</li> It can optionally
 * contain the following capturing groups as well:<br/>
 * <li>%FILENAME%: the file containing the token</li>
 * <li>%ROW%: the row containing the token</li>
 * <li>%COLUMN%: the column on which the token starts</li>
 * <li>%END_ROW%: the row containing the token's end</li>
 * <li>%END_COLUMN%: the column just after the token end</li> The token
 * placeholder will be replaced by (.*). Other placeholders will be replaced by
 * (.+) meaning no empty strings allowed.
 * 
 * @author Assaf Urieli
 *
 */
public class ParserRegexBasedCorpusReader extends ParserAnnotatedCorpusReader implements CurrentFileObserver {
	private static final Logger LOG = LoggerFactory.getLogger(ParserRegexBasedCorpusReader.class);
	private static final String INDEX_PLACEHOLDER = "%INDEX%";
	private static final String TOKEN_PLACEHOLDER = "%TOKEN%";
	private static final String GOVERNOR_PLACEHOLDER = "%GOVERNOR%";
	private static final String LABEL_PLACEHOLDER = "%LABEL%";
	private static final String NON_PROJ_GOVERNOR_PLACEHOLDER = "%NON_PROJ_GOVERNOR%";
	private static final String NON_PROJ_LABEL_PLACEHOLDER = "%NON_PROJ_LABEL%";
	private static final String POSTAG_PLACEHOLDER = "%POSTAG%";
	private static final String FILENAME_PLACEHOLDER = "%FILENAME%";
	private static final String ROW_PLACEHOLDER = "%ROW%";
	private static final String COLUMN_PLACEHOLDER = "%COLUMN%";
	private static final String END_ROW_PLACEHOLDER = "%END_ROW%";
	private static final String END_COLUMN_PLACEHOLDER = "%END_COLUMN%";
	private static final String POSTAG_COMMENT_PLACEHOLDER = "%POSTAG_COMMENT%";
	private static final String DEP_COMMENT_PLACEHOLDER = "%DEP_COMMENT%";

	private Pattern pattern;
	private ParseConfiguration configuration = null;
	private Scanner scanner;
	private File corpusLocation;
	private Charset charset;

	private int sentenceCount = 0;
	private int lineNumber = 0;
	private int totalSentenceCount = 0;
	private String excludeFileName = null;
	private List<File> files;
	private int currentFileIndex = 0;
	private boolean needsToReturnBlankLine = false;
	private File currentFile;

	private Map<String, Integer> placeholderIndexMap = new HashMap<String, Integer>();

	private LexicalEntryReader lexicalEntryReader;

	private final boolean predictTransitions;

	private final TalismaneSession session;
	private final String regex;
	private final CompactLexicalEntrySupport lexicalEntrySupport = new CompactLexicalEntrySupport("");

	private SentenceDetectorAnnotatedCorpusReader sentenceReader = null;

	public ParserRegexBasedCorpusReader(Reader reader, Config config, TalismaneSession session) {
		this(config.getString("preannotated-pattern"), reader, config, session);
	}

	public ParserRegexBasedCorpusReader(String regex, Reader reader, Config config, TalismaneSession session) {
		super(reader, config, session);
		this.regex = regex;
		this.session = session;
		this.scanner = new Scanner(reader);
		this.predictTransitions = config.getBoolean("predict-transitions");
	}

	@Override
	public boolean hasNextConfiguration() {
		if (this.getMaxSentenceCount() > 0 && sentenceCount >= this.getMaxSentenceCount()) {
			// we've reached the end, do nothing
		} else {
			while (configuration == null) {
				List<ParseDataLine> dataLines = new ArrayList<ParseDataLine>();
				List<LexicalEntry> lexicalEntries = new ArrayList<LexicalEntry>();
				boolean hasLine = false;
				if (!this.hasNextLine())
					break;

				int sentenceStartLineNumber = lineNumber;
				while (configuration == null) {
					// break out when there's no next line & nothing in the
					// buffer to process
					if (!this.hasNextLine() && !hasLine)
						break;

					String line = "";
					if (this.hasNextLine())
						line = this.nextLine().replace("\r", "");

					lineNumber++;
					if (line.trim().length() == 0) {
						if (!hasLine)
							continue;

						// end of sentence

						// check cross-validation
						boolean includeMe = true;
						if (this.getCrossValidationSize() > 0) {
							if (this.getIncludeIndex() >= 0) {
								if (totalSentenceCount % this.getCrossValidationSize() != this.getIncludeIndex()) {
									includeMe = false;
								}
							} else if (this.getExcludeIndex() >= 0) {
								if (totalSentenceCount % this.getCrossValidationSize() == this.getExcludeIndex()) {
									includeMe = false;
								}
							}
						}

						if (this.getStartSentence() > totalSentenceCount) {
							includeMe = false;
						}

						totalSentenceCount++;
						if (LOG.isTraceEnabled())
							LOG.trace("totalSentenceCount: " + totalSentenceCount);

						if (!includeMe) {
							dataLines = new ArrayList<ParseDataLine>();
							lexicalEntries = new ArrayList<LexicalEntry>();
							hasLine = false;
							continue;
						}

						// construct the configuration
						if (dataLines.size() > 0) {
							boolean badConfig = false;
							for (ParseDataLine dataLine : dataLines) {
								badConfig = !this.checkDataLine(dataLine);
								if (badConfig) {
									dataLines = new ArrayList<ParseDataLine>();
									hasLine = false;
									break;
								}
							}

							if (!badConfig) {
								Sentence sentence = null;
								if (sentenceReader != null && sentenceReader.hasNextSentence()) {
									sentence = sentenceReader.nextSentence();
								} else {
									LinguisticRules rules = session.getLinguisticRules();
									if (rules == null)
										throw new TalismaneException("Linguistic rules have not been set.");

									String text = "";
									for (ParseDataLine dataLine : dataLines) {
										String word = dataLine.getWord();
										// check if a space should be added
										// before this
										// token

										if (rules.shouldAddSpace(text, word))
											text += " ";
										text += word;
									}
									sentence = new Sentence(text, session);
								}

								for (SentenceAnnotator annotator : session.getSentenceAnnotators()) {
									annotator.annotate(sentence);
								}

								int maxIndex = 0;
								PretokenisedSequence tokenSequence = new PretokenisedSequence(sentence, session);
								for (ParseDataLine dataLine : dataLines) {
									Token token = tokenSequence.addToken(dataLine.getWord());
									dataLine.setToken(token);
									if (dataLine.getIndex() > maxIndex)
										maxIndex = dataLine.getIndex();
									token.setFileName(dataLine.getOriginalFileName());
									token.setLineNumber(dataLine.getOriginalLineNumber());
									token.setColumnNumber(dataLine.getOriginalColumnNumber());
									token.setLineNumberEnd(dataLine.getOriginalEndLineNumber());
									token.setColumnNumberEnd(dataLine.getOriginalEndColumnNumber());
								}

								tokenSequence.cleanSlate();

								LOG.debug("Sentence " + (sentenceCount) + " (Abs " + (totalSentenceCount - 1) + ") (Line " + (sentenceStartLineNumber + 1)
										+ "): " + tokenSequence.getSentence().getText());

								boolean hasSkip = false;
								for (int i = 0; i < dataLines.size(); i++) {
									this.updateDataLine(dataLines, i);
									ParseDataLine dataLine = dataLines.get(i);
									if (dataLine.getWord().equals("") && dataLine.getPosTagCode().equals(""))
										dataLine.setSkip(true);
									if (dataLine.isSkip())
										hasSkip = true;
								}

								if (hasSkip) {
									List<ParseDataLine> newDataLines = new ArrayList<ParseDataLine>();
									for (ParseDataLine dataLine : dataLines) {
										if (dataLine.isSkip()) {
											tokenSequence.removeEmptyToken(dataLine.getToken());
										} else {
											newDataLines.add(dataLine);
										}
									}
									dataLines = newDataLines;
								}

								if (LOG.isTraceEnabled()) {
									LOG.trace("Data lines after update:");
									for (ParseDataLine dataLine : dataLines) {
										LOG.trace(dataLine.toString());
									}
								}

								tokenSequence.getSentence().setStartLineNumber(sentenceStartLineNumber + 1);

								PosTagSequence posTagSequence = new PosTagSequence(tokenSequence);
								Map<Integer, PosTaggedToken> idTokenMap = new HashMap<Integer, PosTaggedToken>();
								int i = 0;
								int lexicalEntryIndex = 0;
								PosTagSet posTagSet = session.getPosTagSet();
								for (ParseDataLine dataLine : dataLines) {
									Token token = tokenSequence.get(i);

									PosTag posTag = null;
									try {
										posTag = posTagSet.getPosTag(dataLine.getPosTagCode());
									} catch (UnknownPosTagException upte) {
										String fileName = "";
										if (currentFile != null)
											fileName = currentFile.getPath();

										throw new TalismaneException(
												"Unknown posTag, " + fileName + ", on line " + dataLine.getLineNumber() + ": " + dataLine.getPosTagCode());
									}
									Decision posTagDecision = new Decision(posTag.getCode());
									PosTaggedToken posTaggedToken = new PosTaggedToken(token, posTagDecision, session);
									if (LOG.isTraceEnabled()) {
										LOG.trace(posTaggedToken.toString());
									}

									posTaggedToken.setComment(dataLine.getPosTagComment());

									// set the lexical entry if we have one
									if (this.lexicalEntryReader != null) {
										List<LexicalEntry> lexicalEntrySet = new ArrayList<LexicalEntry>(1);
										if (!tokenSequence.getTokensAdded().contains(token)) {
											lexicalEntrySet.add(lexicalEntries.get(lexicalEntryIndex++));
										}
										posTaggedToken.setLexicalEntries(lexicalEntrySet);
									}
									posTagSequence.addPosTaggedToken(posTaggedToken);
									idTokenMap.put(dataLine.getIndex(), posTaggedToken);
									i++;
								}

								PosTaggedToken rootToken = posTagSequence.prependRoot();
								idTokenMap.put(0, rootToken);

								TransitionSystem transitionSystem = session.getTransitionSystem();
								Set<DependencyArc> dependencies = new TreeSet<DependencyArc>();
								for (ParseDataLine dataLine : dataLines) {
									PosTaggedToken head = idTokenMap.get(dataLine.getGovernorIndex());
									PosTaggedToken dependent = idTokenMap.get(dataLine.getIndex());

									if (transitionSystem.getDependencyLabels().size() > 1) {
										if (dataLine.getDependencyLabel().length() > 0
												&& !transitionSystem.getDependencyLabels().contains(dataLine.getDependencyLabel())) {
											throw new TalismaneException("Unknown dependency label, " + (currentFile == null ? "" : currentFile.getPath())
													+ ", on line " + dataLine.getLineNumber() + ": " + dataLine.getDependencyLabel());
										}
										if (dataLine.getNonProjectiveLabel().length() > 0
												&& !transitionSystem.getDependencyLabels().contains(dataLine.getNonProjectiveLabel())) {
											throw new TalismaneException("Unknown dependency label, " + (currentFile == null ? "" : currentFile.getPath())
													+ ", on line " + dataLine.getLineNumber() + ": " + dataLine.getNonProjectiveLabel());
										}

									}
									DependencyArc arc = new DependencyArc(head, dependent, dataLine.getDependencyLabel());
									if (LOG.isTraceEnabled())
										LOG.trace(arc.toString());
									dependencies.add(arc);
									arc.setComment(dataLine.getDependencyComment());
								}

								configuration = new ParseConfiguration(posTagSequence);
								if (this.predictTransitions) {
									transitionSystem.predictTransitions(configuration, dependencies);
								} else {
									for (DependencyArc arc : dependencies) {
										configuration.addDependency(arc.getHead(), arc.getDependent(), arc.getLabel(), null);
									}
								}

								// Add manual non-projective dependencies,
								// if there are any
								if (placeholderIndexMap.containsKey(NON_PROJ_GOVERNOR_PLACEHOLDER)) {
									Set<DependencyArc> nonProjDeps = new TreeSet<DependencyArc>();
									if (LOG.isTraceEnabled())
										LOG.trace("Non projective dependencies: ");

									for (ParseDataLine dataLine : dataLines) {
										PosTaggedToken head = idTokenMap.get(dataLine.getNonProjectiveGovernorIndex());
										PosTaggedToken dependent = idTokenMap.get(dataLine.getIndex());
										DependencyArc nonProjArc = new DependencyArc(head, dependent, dataLine.getNonProjectiveLabel());
										if (LOG.isTraceEnabled())
											LOG.trace(nonProjArc.toString());
										nonProjDeps.add(nonProjArc);
										nonProjArc.setComment(dataLine.getDependencyComment());
									}

									for (DependencyArc nonProjArc : nonProjDeps) {
										configuration.addManualNonProjectiveDependency(nonProjArc.getHead(), nonProjArc.getDependent(), nonProjArc.getLabel());
									}
								}

								sentenceCount++;
							} // is the configuration a valid one
						} // have we data lines?
					} else {
						// add a token to the current sentence
						hasLine = true;
						if (totalSentenceCount >= this.getStartSentence()) {
							Matcher matcher = this.getPattern().matcher(line);
							if (!matcher.matches())
								throw new TalismaneException("Didn't match pattern \"" + regex + "\" on line " + lineNumber + ": " + line);

							if (matcher.groupCount() != placeholderIndexMap.size()) {
								throw new TalismaneException(
										"Expected " + placeholderIndexMap.size() + " matches (but found " + matcher.groupCount() + ") on line " + lineNumber);
							}

							int index = Integer.parseInt(matcher.group(placeholderIndexMap.get(INDEX_PLACEHOLDER)));
							String rawWord = matcher.group(placeholderIndexMap.get(TOKEN_PLACEHOLDER));
							String word = this.readWord(rawWord);
							String posTagCode = matcher.group(placeholderIndexMap.get(POSTAG_PLACEHOLDER));
							String depLabel = matcher.group(placeholderIndexMap.get(LABEL_PLACEHOLDER));
							if (depLabel.equals("_"))
								depLabel = "";
							int governorIndex = Integer.parseInt(matcher.group(placeholderIndexMap.get(GOVERNOR_PLACEHOLDER)));

							ParseDataLine dataLine = new ParseDataLine();
							dataLine.setLineNumber(this.lineNumber);
							dataLine.setIndex(index);
							dataLine.setWord(word);
							dataLine.setPosTagCode(posTagCode);
							dataLine.setDependencyLabel(depLabel);
							dataLine.setGovernorIndex(governorIndex);
							if (placeholderIndexMap.containsKey(NON_PROJ_GOVERNOR_PLACEHOLDER)) {
								int nonProjectiveGovernorIndex = Integer.parseInt(matcher.group(placeholderIndexMap.get(NON_PROJ_GOVERNOR_PLACEHOLDER)));
								dataLine.setNonProjectiveGovernorIndex(nonProjectiveGovernorIndex);
							}
							if (placeholderIndexMap.containsKey(NON_PROJ_LABEL_PLACEHOLDER)) {
								String nonProjLabel = matcher.group(placeholderIndexMap.get(NON_PROJ_LABEL_PLACEHOLDER));
								if (nonProjLabel.equals("_"))
									nonProjLabel = "";
								dataLine.setNonProjectiveLabel(nonProjLabel);
							}

							if (placeholderIndexMap.containsKey(FILENAME_PLACEHOLDER))
								dataLine.setOriginalFileName(matcher.group(placeholderIndexMap.get(FILENAME_PLACEHOLDER)));
							if (placeholderIndexMap.containsKey(ROW_PLACEHOLDER))
								dataLine.setOriginalLineNumber(Integer.parseInt(matcher.group(placeholderIndexMap.get(ROW_PLACEHOLDER))));
							if (placeholderIndexMap.containsKey(COLUMN_PLACEHOLDER))
								dataLine.setOriginalColumnNumber(Integer.parseInt(matcher.group(placeholderIndexMap.get(COLUMN_PLACEHOLDER))));
							if (placeholderIndexMap.containsKey(END_ROW_PLACEHOLDER))
								dataLine.setOriginalEndLineNumber(Integer.parseInt(matcher.group(placeholderIndexMap.get(END_ROW_PLACEHOLDER))));
							if (placeholderIndexMap.containsKey(END_COLUMN_PLACEHOLDER))
								dataLine.setOriginalEndColumnNumber(Integer.parseInt(matcher.group(placeholderIndexMap.get(END_COLUMN_PLACEHOLDER))));
							if (placeholderIndexMap.containsKey(POSTAG_COMMENT_PLACEHOLDER))
								dataLine.setPosTagComment(matcher.group(placeholderIndexMap.get(POSTAG_COMMENT_PLACEHOLDER)));
							if (placeholderIndexMap.containsKey(DEP_COMMENT_PLACEHOLDER))
								dataLine.setDependencyComment(matcher.group(placeholderIndexMap.get(DEP_COMMENT_PLACEHOLDER)));

							dataLines.add(dataLine);

							if (this.lexicalEntryReader != null) {
								WritableLexicalEntry lexicalEntry = new CompactLexicalEntry(lexicalEntrySupport);
								this.lexicalEntryReader.readEntry(line, lexicalEntry);
								lexicalEntries.add(lexicalEntry);
							}
						}
					}
				}
			} // is configuration still null?
		} // have we reached the max sentence count?

		return configuration != null;
	}

	/**
	 * Returns true if the data line is valid, false otherwise.
	 */
	protected boolean checkDataLine(ParseDataLine dataLine) {
		return true;
	}

	/**
	 * Updates the data line prior to processing. At this point, empty lines may
	 * have been added to correspond to empty tokens that were added by filters.
	 */
	protected void updateDataLine(List<ParseDataLine> dataLines, int index) {
		// nothing to do in the base class
	}

	@Override
	public ParseConfiguration nextConfiguration() {
		ParseConfiguration nextConfiguration = null;
		if (this.hasNextConfiguration()) {
			nextConfiguration = configuration;
			configuration = null;
		}
		return nextConfiguration;
	}

	protected static class ParseDataLine {
		public ParseDataLine() {
		}

		private int lineNumber = 0;
		private int index;
		private String word = "";
		private String posTagCode = "";
		private int governorIndex;
		private String dependencyLabel = "";
		private int nonProjectiveGovernorIndex;
		private String nonProjectiveLabel = "";
		private Token token;
		private String originalFileName = "";
		private int originalLineNumber = 0;
		private int originalColumnNumber = 0;
		private int originalEndLineNumber = 0;
		private int originalEndColumnNumber = 0;
		private boolean skip = false;
		private String posTagComment = "";
		private String dependencyComment = "";

		public int getLineNumber() {
			return lineNumber;
		}

		public void setLineNumber(int lineNumber) {
			this.lineNumber = lineNumber;
		}

		public int getIndex() {
			return index;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		public String getWord() {
			return word;
		}

		public void setWord(String word) {
			this.word = word;
		}

		public String getPosTagCode() {
			return posTagCode;
		}

		public void setPosTagCode(String posTagCode) {
			this.posTagCode = posTagCode;
		}

		public int getGovernorIndex() {
			return governorIndex;
		}

		public void setGovernorIndex(int governorIndex) {
			this.governorIndex = governorIndex;
		}

		public String getDependencyLabel() {
			return dependencyLabel;
		}

		public void setDependencyLabel(String dependencyLabel) {
			this.dependencyLabel = dependencyLabel;
		}

		public Token getToken() {
			return token;
		}

		public void setToken(Token token) {
			this.token = token;
		}

		/**
		 * Should this data line be skipped or not? This should only be set for
		 * data lines corresponding to empty tokens. The empty token will be
		 * removed.
		 */
		public boolean isSkip() {
			return skip;
		}

		public void setSkip(boolean skip) {
			this.skip = skip;
		}

		public String getOriginalFileName() {
			return originalFileName;
		}

		public void setOriginalFileName(String originalFileName) {
			this.originalFileName = originalFileName;
		}

		public int getOriginalLineNumber() {
			return originalLineNumber;
		}

		public void setOriginalLineNumber(int originalLineNumber) {
			this.originalLineNumber = originalLineNumber;
		}

		public int getOriginalColumnNumber() {
			return originalColumnNumber;
		}

		public void setOriginalColumnNumber(int originalColumnNumber) {
			this.originalColumnNumber = originalColumnNumber;
		}

		public int getOriginalEndLineNumber() {
			return originalEndLineNumber;
		}

		public void setOriginalEndLineNumber(int originalEndLineNumber) {
			this.originalEndLineNumber = originalEndLineNumber;
		}

		public int getOriginalEndColumnNumber() {
			return originalEndColumnNumber;
		}

		public void setOriginalEndColumnNumber(int originalEndColumnNumber) {
			this.originalEndColumnNumber = originalEndColumnNumber;
		}

		public String getPosTagComment() {
			return posTagComment;
		}

		public void setPosTagComment(String posTagComment) {
			this.posTagComment = posTagComment;
		}

		public String getDependencyComment() {
			return dependencyComment;
		}

		public void setDependencyComment(String dependencyComment) {
			this.dependencyComment = dependencyComment;
		}

		public int getNonProjectiveGovernorIndex() {
			return nonProjectiveGovernorIndex;
		}

		public void setNonProjectiveGovernorIndex(int nonProjectiveGovernorIndex) {
			this.nonProjectiveGovernorIndex = nonProjectiveGovernorIndex;
		}

		public String getNonProjectiveLabel() {
			return nonProjectiveLabel;
		}

		public void setNonProjectiveLabel(String nonProjectiveLabel) {
			this.nonProjectiveLabel = nonProjectiveLabel;
		}

		@Override
		public String toString() {
			String string = lineNumber + ": " + index + "," + word + "," + posTagCode + "," + nonProjectiveGovernorIndex + "," + nonProjectiveLabel + ","
					+ governorIndex + "," + dependencyLabel;
			return string;
		}
	}

	@Override
	public Map<String, String> getCharacteristics() {
		Map<String, String> attributes = super.getCharacteristics();
		attributes.put("transitionSystem", session.getTransitionSystem().getClass().getSimpleName());

		return attributes;
	}

	/**
	 * The regex used to find the various data items.
	 */
	public String getRegex() {
		return regex;
	}

	public Pattern getPattern() {
		if (this.pattern == null) {
			int indexPos = regex.indexOf(INDEX_PLACEHOLDER);
			if (indexPos < 0)
				throw new TalismaneException("The regex must contain the string \"" + INDEX_PLACEHOLDER + "\": " + regex);

			int tokenPos = regex.indexOf(TOKEN_PLACEHOLDER);
			if (tokenPos < 0)
				throw new TalismaneException("The regex must contain the string \"" + TOKEN_PLACEHOLDER + "\"");

			int posTagPos = regex.indexOf(POSTAG_PLACEHOLDER);
			if (posTagPos < 0)
				throw new TalismaneException("The regex must contain the string \"" + POSTAG_PLACEHOLDER + "\"");

			int labelPos = regex.indexOf(LABEL_PLACEHOLDER);
			if (labelPos < 0)
				throw new TalismaneException("The regex must contain the string \"" + LABEL_PLACEHOLDER + "\"");

			int governorPos = regex.indexOf(GOVERNOR_PLACEHOLDER);
			if (governorPos < 0)
				throw new TalismaneException("The regex must contain the string \"" + GOVERNOR_PLACEHOLDER + "\"");

			int nonProjGovernorPos = regex.indexOf(NON_PROJ_GOVERNOR_PLACEHOLDER);
			int nonProjLabelPos = regex.indexOf(NON_PROJ_LABEL_PLACEHOLDER);
			int filenamePos = regex.indexOf(FILENAME_PLACEHOLDER);
			int rowNumberPos = regex.indexOf(ROW_PLACEHOLDER);
			int columnNumberPos = regex.indexOf(COLUMN_PLACEHOLDER);
			int endRowNumberPos = regex.indexOf(END_ROW_PLACEHOLDER);
			int endColumnNumberPos = regex.indexOf(END_COLUMN_PLACEHOLDER);
			int posTagCommentPos = regex.indexOf(POSTAG_COMMENT_PLACEHOLDER);
			int depCommentPos = regex.indexOf(DEP_COMMENT_PLACEHOLDER);

			Map<Integer, String> placeholderMap = new TreeMap<Integer, String>();
			placeholderMap.put(indexPos, INDEX_PLACEHOLDER);
			placeholderMap.put(tokenPos, TOKEN_PLACEHOLDER);
			placeholderMap.put(posTagPos, POSTAG_PLACEHOLDER);
			placeholderMap.put(labelPos, LABEL_PLACEHOLDER);
			placeholderMap.put(governorPos, GOVERNOR_PLACEHOLDER);
			if (nonProjGovernorPos >= 0)
				placeholderMap.put(nonProjGovernorPos, NON_PROJ_GOVERNOR_PLACEHOLDER);
			if (nonProjLabelPos >= 0)
				placeholderMap.put(nonProjLabelPos, NON_PROJ_LABEL_PLACEHOLDER);
			if (filenamePos >= 0)
				placeholderMap.put(filenamePos, FILENAME_PLACEHOLDER);
			if (rowNumberPos >= 0)
				placeholderMap.put(rowNumberPos, ROW_PLACEHOLDER);
			if (columnNumberPos >= 0)
				placeholderMap.put(columnNumberPos, COLUMN_PLACEHOLDER);
			if (endRowNumberPos >= 0)
				placeholderMap.put(endRowNumberPos, END_ROW_PLACEHOLDER);
			if (endColumnNumberPos >= 0)
				placeholderMap.put(endColumnNumberPos, END_COLUMN_PLACEHOLDER);
			if (posTagCommentPos >= 0)
				placeholderMap.put(posTagCommentPos, POSTAG_COMMENT_PLACEHOLDER);
			if (depCommentPos >= 0)
				placeholderMap.put(depCommentPos, DEP_COMMENT_PLACEHOLDER);

			for (int j = 0; j < regex.length(); j++) {
				if (regex.charAt(j) == '(') {
					placeholderMap.put(j, "");
				}
			}
			int i = 1;
			for (int placeholderIndex : placeholderMap.keySet()) {
				String placeholderName = placeholderMap.get(placeholderIndex);
				if (placeholderName.length() > 0)
					placeholderIndexMap.put(placeholderName, i);
				i++;
			}

			String regexWithGroups = regex.replace(INDEX_PLACEHOLDER, "(\\d+)");
			regexWithGroups = regexWithGroups.replace(TOKEN_PLACEHOLDER, "(.*?)");
			regexWithGroups = regexWithGroups.replace(POSTAG_PLACEHOLDER, "(.+?)");
			regexWithGroups = regexWithGroups.replace(LABEL_PLACEHOLDER, "(.*?)");
			regexWithGroups = regexWithGroups.replace(GOVERNOR_PLACEHOLDER, "(\\d+)");
			regexWithGroups = regexWithGroups.replace(NON_PROJ_LABEL_PLACEHOLDER, "(.*?)");
			regexWithGroups = regexWithGroups.replace(NON_PROJ_GOVERNOR_PLACEHOLDER, "(\\d+)");
			regexWithGroups = regexWithGroups.replace(FILENAME_PLACEHOLDER, "(.+?)");
			regexWithGroups = regexWithGroups.replace(ROW_PLACEHOLDER, "(\\d+)");
			regexWithGroups = regexWithGroups.replace(COLUMN_PLACEHOLDER, "(\\d+)");
			regexWithGroups = regexWithGroups.replace(END_ROW_PLACEHOLDER, "(\\d+)");
			regexWithGroups = regexWithGroups.replace(END_COLUMN_PLACEHOLDER, "(\\d+)");
			regexWithGroups = regexWithGroups.replace(POSTAG_COMMENT_PLACEHOLDER, "(.*?)");
			regexWithGroups = regexWithGroups.replace(DEP_COMMENT_PLACEHOLDER, "(.*?)");

			this.pattern = Pattern.compile(regexWithGroups, Pattern.UNICODE_CHARACTER_CLASS);
		}
		return pattern;
	}

	private boolean hasNextLine() {
		try {
			if (needsToReturnBlankLine)
				return true;

			if (this.scanner == null && currentFileIndex == 0) {
				if (corpusLocation == null) {
					return false;
				} else if (corpusLocation.isDirectory()) {
					files = new ArrayList<File>();
					this.addFiles(corpusLocation, files);
					File file = files.get(0);
					this.onNextFile(file);
					Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
					this.scanner = new Scanner(reader);
				} else {
					Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(corpusLocation), charset));
					this.scanner = new Scanner(reader);
				}
				currentFileIndex++;
			}

			while (this.scanner != null) {
				if (this.scanner.hasNextLine())
					return true;

				needsToReturnBlankLine = true;
				this.scanner = null;

				if (files != null) {
					if (currentFileIndex < files.size()) {
						File file = files.get(currentFileIndex);
						this.onNextFile(file);
						Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
						this.scanner = new Scanner(reader);
						currentFileIndex++;
					}
				}
			}

			return false;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	private void addFiles(File directory, List<File> files) {
		File[] theFiles = directory.listFiles();
		Arrays.sort(theFiles);
		for (File file : theFiles) {
			if (!file.getName().equals(excludeFileName)) {
				if (file.isDirectory())
					this.addFiles(file, files);
				else
					files.add(file);
			}
		}
	}

	private String nextLine() {
		if (needsToReturnBlankLine) {
			needsToReturnBlankLine = false;
			return "";
		}
		return this.scanner.nextLine();
	}

	@Override
	public void rewind() {
		if (this.corpusLocation == null) {
			throw new TalismaneException(this.getClass().getName() + " does not support rewind if not constructed from File");
		}

		this.scanner = null;
		this.currentFileIndex = 0;
		configuration = null;

		sentenceCount = 0;
		lineNumber = 0;
		totalSentenceCount = 0;
	}

	@Override
	public LexicalEntryReader getLexicalEntryReader() {
		return lexicalEntryReader;
	}

	@Override
	public void setLexicalEntryReader(LexicalEntryReader lexicalEntryReader) {
		this.lexicalEntryReader = lexicalEntryReader;
	}

	@Override
	public boolean hasNextPosTagSequence() {
		return this.hasNextConfiguration();
	}

	@Override
	public PosTagSequence nextPosTagSequence() {
		PosTagSequence sequence = this.nextConfiguration().getPosTagSequence();
		PosTagSequence clone = sequence.clonePosTagSequence();
		clone.removeRoot();
		return clone;
	}

	@Override
	public boolean hasNextTokenSequence() {
		return this.hasNextConfiguration();
	}

	@Override
	public TokenSequence nextTokenSequence() {
		TokenSequence tokenSequence = this.nextPosTagSequence().getTokenSequence();
		return tokenSequence;
	}

	@Override
	public boolean hasNextSentence() {
		return this.hasNextConfiguration();
	}

	@Override
	public Sentence nextSentence() {
		return this.nextTokenSequence().getSentence();
	}

	@Override
	public boolean isNewParagraph() {
		return false;
	}

	protected String readWord(String rawWord) {
		return session.getCoNLLFormatter().fromCoNLL(rawWord);
	}

	/**
	 * Should an attempt be made to the predict the transitions that led to this
	 * configuration, or should dependencies simply be added with null
	 * transitions.
	 */
	public boolean isPredictTransitions() {
		return predictTransitions;
	}

	/**
	 * If the reader is opened based on a directory, the name of a file to
	 * exclude when training.
	 */
	public String getExcludeFileName() {
		return excludeFileName;
	}

	public void setExcludeFileName(String excludeFileName) {
		this.excludeFileName = excludeFileName;
	}

	@Override
	public void onNextFile(File file) {
		currentFile = file;
		lineNumber = 0;
	}

	/**
	 * If provided, will assign sentences with the original white space to the
	 * token sequences.
	 */
	public SentenceDetectorAnnotatedCorpusReader getSentenceReader() {
		return sentenceReader;
	}

	public void setSentenceReader(SentenceDetectorAnnotatedCorpusReader sentenceReader) {
		this.sentenceReader = sentenceReader;
	}
}
