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
import java.util.LinkedHashMap;
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

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.LexicalEntryReader;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.UnknownPosTagException;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.tokeniser.PretokenisedSequence;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.CoNLLFormatter;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.PerformanceMonitor;
import com.joliciel.talismane.utils.io.CurrentFileObserver;

public class ParserRegexBasedCorpusReaderImpl implements ParserRegexBasedCorpusReader, CurrentFileObserver {
	private static final Logger LOG = LoggerFactory.getLogger(ParserRegexBasedCorpusReaderImpl.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(ParserRegexBasedCorpusReaderImpl.class);
	private String regex = ParserRegexBasedCorpusReader.DEFAULT_REGEX;
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

	private TalismaneService talismaneService;
	private ParserService parserService;
	private PosTaggerService posTaggerService;
	private TokeniserService tokeniserService;
	private TokenFilterService tokenFilterService;
	private MachineLearningService machineLearningService;

	private int maxSentenceCount = 0;
	private int startSentence = 0;
	private int sentenceCount = 0;
	private int lineNumber = 0;
	private int crossValidationSize = -1;
	private int includeIndex = -1;
	private int excludeIndex = -1;
	private int totalSentenceCount = 0;
	private String excludeFileName = null;
	private List<File> files;
	private int currentFileIndex = 0;
	private boolean needsToReturnBlankLine = false;
	private File currentFile;

	private List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();
	private List<TokenSequenceFilter> tokenSequenceFilters = new ArrayList<TokenSequenceFilter>();
	private List<PosTagSequenceFilter> posTagSequenceFilters = new ArrayList<PosTagSequenceFilter>();
	private TokenSequenceFilter tokenFilterWrapper = null;

	private Map<String, Integer> placeholderIndexMap = new HashMap<String, Integer>();

	private LexicalEntryReader lexicalEntryReader;
	private TalismaneSession talismaneSession;

	private boolean predictTransitions = true;

	public ParserRegexBasedCorpusReaderImpl(File corpusLocation, Charset charset) {
		this.corpusLocation = corpusLocation;
		this.charset = charset;
	}

	public ParserRegexBasedCorpusReaderImpl(Reader reader) {
		this.scanner = new Scanner(reader);
	}

	@Override
	public boolean hasNextConfiguration() {
		MONITOR.startTask("hasNextConfiguration");
		try {
			if (maxSentenceCount > 0 && sentenceCount >= maxSentenceCount) {
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
							if (crossValidationSize > 0) {
								if (includeIndex >= 0) {
									if (totalSentenceCount % crossValidationSize != includeIndex) {
										includeMe = false;
									}
								} else if (excludeIndex >= 0) {
									if (totalSentenceCount % crossValidationSize == excludeIndex) {
										includeMe = false;
									}
								}
							}

							if (totalSentenceCount < startSentence) {
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
									PretokenisedSequence tokenSequence = this.getTokeniserService().getEmptyPretokenisedSequence();

									int maxIndex = 0;
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
									LOG.debug("Sentence " + (sentenceCount) + " (Abs " + (totalSentenceCount - 1) + ") (Line " + (sentenceStartLineNumber + 1)
											+ "): " + tokenSequence.getText());

									tokenSequence.cleanSlate();

									// first apply the token filters - which
									// might replace the text of an individual
									// token
									// with something else
									if (tokenFilterWrapper == null) {
										tokenFilterWrapper = this.getTokenFilterService().getTokenSequenceFilter(this.tokenFilters);
									}
									tokenFilterWrapper.apply(tokenSequence);

									for (TokenSequenceFilter tokenFilter : this.tokenSequenceFilters) {
										tokenFilter.apply(tokenSequence);
									}

									if (tokenSequence.getTokensAdded().size() > 0) {
										// create an empty data line for each
										// empty token that was added by the
										// filters
										List<ParseDataLine> newDataLines = new ArrayList<ParseDataLine>();
										int i = 0;
										ParseDataLine lastDataLine = null;
										for (Token token : tokenSequence) {
											if (tokenSequence.getTokensAdded().contains(token)) {
												ParseDataLine emptyDataLine = new ParseDataLine();
												emptyDataLine.setToken(token);
												emptyDataLine.setWord("");
												emptyDataLine.setIndex(++maxIndex);
												if (lastDataLine != null)
													emptyDataLine.setLineNumber(lastDataLine.getLineNumber());
												else
													emptyDataLine.setLineNumber(sentenceStartLineNumber);
												newDataLines.add(emptyDataLine);
											} else {
												lastDataLine = dataLines.get(i++);
												newDataLines.add(lastDataLine);
											}
										}
										dataLines = newDataLines;
									}

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

									PosTagSequence posTagSequence = this.getPosTaggerService().getPosTagSequence(tokenSequence);
									Map<Integer, PosTaggedToken> idTokenMap = new HashMap<Integer, PosTaggedToken>();
									int i = 0;
									int lexicalEntryIndex = 0;
									PosTagSet posTagSet = talismaneSession.getPosTagSet();
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
										Decision posTagDecision = machineLearningService.createDefaultDecision(posTag.getCode());
										PosTaggedToken posTaggedToken = this.getPosTaggerService().getPosTaggedToken(token, posTagDecision);
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

									for (PosTagSequenceFilter posTagSequenceFilter : this.posTagSequenceFilters) {
										posTagSequenceFilter.apply(posTagSequence);
									}

									PosTaggedToken rootToken = posTagSequence.prependRoot();
									idTokenMap.put(0, rootToken);

									TransitionSystem transitionSystem = talismaneSession.getTransitionSystem();
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
										DependencyArc arc = this.getParserService().getDependencyArc(head, dependent, dataLine.getDependencyLabel());
										if (LOG.isTraceEnabled())
											LOG.trace(arc.toString());
										dependencies.add(arc);
										arc.setComment(dataLine.getDependencyComment());
									}

									configuration = this.getParserService().getInitialConfiguration(posTagSequence);
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
											DependencyArc nonProjArc = this.getParserService().getDependencyArc(head, dependent,
													dataLine.getNonProjectiveLabel());
											if (LOG.isTraceEnabled())
												LOG.trace(nonProjArc.toString());
											nonProjDeps.add(nonProjArc);
											nonProjArc.setComment(dataLine.getDependencyComment());
										}

										for (DependencyArc nonProjArc : nonProjDeps) {
											configuration.addManualNonProjectiveDependency(nonProjArc.getHead(), nonProjArc.getDependent(),
													nonProjArc.getLabel());
										}
									}

									sentenceCount++;
								} // is the configuration a valid one
							} // have we data lines?
						} else {
							// add a token to the current sentence
							hasLine = true;
							if (totalSentenceCount >= startSentence) {
								Matcher matcher = this.getPattern().matcher(line);
								if (!matcher.matches())
									throw new TalismaneException("Didn't match pattern \"" + regex + "\" on line " + lineNumber + ": " + line);

								if (matcher.groupCount() != placeholderIndexMap.size()) {
									throw new TalismaneException("Expected " + placeholderIndexMap.size() + " matches (but found " + matcher.groupCount()
											+ ") on line " + lineNumber);
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
									LexicalEntry lexicalEntry = this.lexicalEntryReader.readEntry(line);
									lexicalEntries.add(lexicalEntry);
								}
							}
						}
					}
				} // is configuration still null?
			} // have we reached the max sentence count?

			return configuration != null;
		} finally {
			MONITOR.endTask();
		}
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

	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public ParserService getParserService() {
		return parserService;
	}

	public void setParserService(ParserService parserService) {
		this.parserService = parserService;
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

	/**
	 * If 0, all sentences will be read - otherwise will only read a certain
	 * number of sentences.
	 */
	@Override
	public int getMaxSentenceCount() {
		return maxSentenceCount;
	}

	@Override
	public void setMaxSentenceCount(int maxSentenceCount) {
		this.maxSentenceCount = maxSentenceCount;
	}

	@Override
	public void addTokenFilter(TokenFilter tokenFilter) {
		this.tokenFilters.add(tokenFilter);
	}

	@Override
	public void addTokenSequenceFilter(TokenSequenceFilter tokenFilter) {
		this.tokenSequenceFilters.add(tokenFilter);
	}

	@Override
	public Map<String, String> getCharacteristics() {
		Map<String, String> attributes = new LinkedHashMap<String, String>();

		attributes.put("maxSentenceCount", "" + this.maxSentenceCount);
		attributes.put("crossValidationSize", "" + this.crossValidationSize);
		attributes.put("includeIndex", "" + this.includeIndex);
		attributes.put("excludeIndex", "" + this.excludeIndex);
		attributes.put("transitionSystem", talismaneSession.getTransitionSystem().getClass().getSimpleName());

		int i = 0;
		for (TokenSequenceFilter tokenFilter : this.tokenSequenceFilters) {
			attributes.put("filter" + i, "" + tokenFilter.getClass().getSimpleName());

			i++;
		}
		return attributes;
	}

	@Override
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

			int i = 1;
			for (String placeholderName : placeholderMap.values()) {
				placeholderIndexMap.put(placeholderName, i++);
			}

			String regexWithGroups = regex.replace(INDEX_PLACEHOLDER, "(\\d+)");
			regexWithGroups = regexWithGroups.replace(TOKEN_PLACEHOLDER, "(.*)");
			regexWithGroups = regexWithGroups.replace(POSTAG_PLACEHOLDER, "(.+)");
			regexWithGroups = regexWithGroups.replace(LABEL_PLACEHOLDER, "(.*)");
			regexWithGroups = regexWithGroups.replace(GOVERNOR_PLACEHOLDER, "(\\d+)");
			regexWithGroups = regexWithGroups.replace(NON_PROJ_LABEL_PLACEHOLDER, "(.*)");
			regexWithGroups = regexWithGroups.replace(NON_PROJ_GOVERNOR_PLACEHOLDER, "(\\d+)");
			regexWithGroups = regexWithGroups.replace(FILENAME_PLACEHOLDER, "(.+)");
			regexWithGroups = regexWithGroups.replace(ROW_PLACEHOLDER, "(\\d+)");
			regexWithGroups = regexWithGroups.replace(COLUMN_PLACEHOLDER, "(\\d+)");
			regexWithGroups = regexWithGroups.replace(END_ROW_PLACEHOLDER, "(\\d+)");
			regexWithGroups = regexWithGroups.replace(END_COLUMN_PLACEHOLDER, "(\\d+)");
			regexWithGroups = regexWithGroups.replace(POSTAG_COMMENT_PLACEHOLDER, "(.*)");
			regexWithGroups = regexWithGroups.replace(DEP_COMMENT_PLACEHOLDER, "(.*)");

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
	public void setRegex(String regex) {
		this.regex = regex;
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
	public void addPosTagSequenceFilter(PosTagSequenceFilter posTagSequenceFilter) {
		this.posTagSequenceFilters.add(posTagSequenceFilter);
	}

	public TokenFilterService getTokenFilterService() {
		return tokenFilterService;
	}

	public void setTokenFilterService(TokenFilterService tokenFilterService) {
		this.tokenFilterService = tokenFilterService;
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
	public String nextSentence() {
		return this.nextTokenSequence().getText();
	}

	@Override
	public boolean isNewParagraph() {
		return false;
	}

	@Override
	public List<TokenSequenceFilter> getTokenSequenceFilters() {
		return this.tokenSequenceFilters;
	}

	@Override
	public List<TokenFilter> getTokenFilters() {
		return this.tokenFilters;
	}

	protected String readWord(String rawWord) {
		return CoNLLFormatter.fromCoNLL(rawWord);
	}

	@Override
	public boolean isPredictTransitions() {
		return predictTransitions;
	}

	@Override
	public void setPredictTransitions(boolean predictTransitions) {
		this.predictTransitions = predictTransitions;
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
	public String getExcludeFileName() {
		return excludeFileName;
	}

	@Override
	public void setExcludeFileName(String excludeFileName) {
		this.excludeFileName = excludeFileName;
	}

	@Override
	public int getStartSentence() {
		return startSentence;
	}

	@Override
	public void setStartSentence(int startSentence) {
		this.startSentence = startSentence;
	}

	@Override
	public void onNextFile(File file) {
		currentFile = file;
		lineNumber = 0;
	}

	public TalismaneService getTalismaneService() {
		return talismaneService;
	}

	public void setTalismaneService(TalismaneService talismaneService) {
		this.talismaneService = talismaneService;
		this.talismaneSession = talismaneService.getTalismaneSession();
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

}
