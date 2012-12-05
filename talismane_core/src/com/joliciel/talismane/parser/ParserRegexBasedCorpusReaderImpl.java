///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.LexicalEntryReader;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.UnknownPosTagException;
import com.joliciel.talismane.tokeniser.PretokenisedSequence;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.PerformanceMonitor;

public class ParserRegexBasedCorpusReaderImpl implements
		ParserRegexBasedCorpusReader {
    private static final Log LOG = LogFactory.getLog(ParserRegexBasedCorpusReaderImpl.class);
	private String regex = ParserRegexBasedCorpusReader.DEFAULT_REGEX;
	private static final String INDEX_PLACEHOLDER = "%INDEX%";
	private static final String TOKEN_PLACEHOLDER = "%TOKEN%";
	private static final String GOVERNOR_PLACEHOLDER = "%GOVERNOR%";
	private static final String LABEL_PLACEHOLDER = "%LABEL%";
	private static final String POSTAG_PLACEHOLDER = "%POSTAG%";
	private static final String FILENAME_PLACEHOLDER = "%FILENAME%";
	private static final String ROW_PLACEHOLDER = "%ROW%";
	private static final String COLUMN_PLACEHOLDER = "%COLUMN%";
	
	private Pattern pattern;
	private ParseConfiguration configuration = null;
	private Scanner scanner;
	
	private ParserService parserService;
	private PosTaggerService posTaggerService;
	private TokeniserService tokeniserService;
	
	private int maxSentenceCount = 0;
	private int sentenceCount = 0;
	private int lineNumber = 0;

	private List<TokenSequenceFilter> tokenFilters = new ArrayList<TokenSequenceFilter>();
	private Map<String, Integer> placeholderIndexMap = new HashMap<String, Integer>();
	
	private LexicalEntryReader lexicalEntryReader;
	
	public ParserRegexBasedCorpusReaderImpl(Reader reader) {
		this.scanner = new Scanner(reader);
	}

	@Override
	public boolean hasNextConfiguration() {
		PerformanceMonitor.startTask("ParserRegexBasedCorpusReaderImpl.hasNextConfiguration");
		try {
			if (maxSentenceCount>0 && sentenceCount>=maxSentenceCount) {
				// we've reached the end, do nothing
			} else {
				while (configuration==null) {
					List<ParseDataLine> dataLines = new ArrayList<ParseDataLine>();
					List<LexicalEntry> lexicalEntries = new ArrayList<LexicalEntry>();
					boolean hasLine = false;
					if (!scanner.hasNextLine())
						break;
					
					int sentenceStartLineNumber = lineNumber;
					while (configuration==null) {
						// break out when there's no next line & nothing in the buffer to process
						if (!scanner.hasNextLine() && !hasLine)
							break;
						
						String line = "";
						if (scanner.hasNextLine())
							line = scanner.nextLine().replace("\r", "");
						
						lineNumber++;
						if (line.length()==0) {
							if (!hasLine)
								continue;
							
							// end of sentence: construct the configuration
							if (dataLines.size()>0) {
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
									PretokenisedSequence tokenSequence = tokeniserService.getEmptyPretokenisedSequence();
	
									int maxIndex = 0;
									for (ParseDataLine dataLine : dataLines) {
										Token token = this.addToken(tokenSequence, dataLine.getWord());
										dataLine.setToken(token);
										if (dataLine.getIndex()>maxIndex)
											maxIndex = dataLine.getIndex();
										token.setFileName(dataLine.getOriginalFileName());
										token.setLineNumber(dataLine.getOriginalLineNumber());
										token.setColumnNumber(dataLine.getOriginalColumnNumber());
									}
									LOG.debug("Sentence " + sentenceCount + ": " + tokenSequence.getText());
									
									tokenSequence.cleanSlate();
									for (TokenSequenceFilter tokenFilter : this.tokenFilters) {
										tokenFilter.apply(tokenSequence);
									}
									
									if (tokenSequence.getTokensAdded().size()>0) {
										// create an empty data line for each empty token that was added by the filters
										List<ParseDataLine> newDataLines = new ArrayList<ParseDataLine>();
										int i = 0;
										ParseDataLine lastDataLine = null;
										for (Token token : tokenSequence) {
											if (tokenSequence.getTokensAdded().contains(token)) {
												ParseDataLine emptyDataLine = new ParseDataLine();
												emptyDataLine.setToken(token);
												emptyDataLine.setWord("");
												emptyDataLine.setIndex(++maxIndex);
												if (lastDataLine!=null)
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
									for (int i=0; i<dataLines.size(); i++) {
										this.updateDataLine(dataLines, i);
										ParseDataLine dataLine = dataLines.get(i);
										if (dataLine.getWord().equals("")&&dataLine.getPosTagCode().equals(""))
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
									
									PosTagSequence posTagSequence = this.posTaggerService.getPosTagSequence(tokenSequence, tokenSequence.size());
									Map<Integer, PosTaggedToken> idTokenMap = new HashMap<Integer, PosTaggedToken>();
									int i = 0;
									int lexicalEntryIndex = 0;
									PosTagSet posTagSet = TalismaneSession.getPosTagSet();
									for (ParseDataLine dataLine : dataLines) {
										Token token = tokenSequence.get(i);
										
						  				PosTag posTag = null;
					    				try {
					    					posTag = posTagSet.getPosTag(dataLine.getPosTagCode());
					    				} catch (UnknownPosTagException upte) {
					    					throw new TalismaneException("Unknown posTag on line " + dataLine.getLineNumber() + ": " + dataLine.getPosTagCode());
					    				}
										Decision<PosTag> posTagDecision = posTagSet.createDefaultDecision(posTag);
										PosTaggedToken posTaggedToken = this.posTaggerService.getPosTaggedToken(token, posTagDecision);
										if (LOG.isTraceEnabled()) {
											LOG.trace(posTaggedToken.toString());
										}
										
										// set the lexical entry if we have one
										if (this.lexicalEntryReader!=null) {
											Set<LexicalEntry> lexicalEntrySet = new HashSet<LexicalEntry>(1);
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
									
									Set<DependencyArc> dependencies = new TreeSet<DependencyArc>();
									for (ParseDataLine dataLine : dataLines) {
										PosTaggedToken head = idTokenMap.get(dataLine.getGovernorIndex());
										PosTaggedToken dependent = idTokenMap.get(dataLine.getIndex());
										DependencyArc arc = this.parserService.getDependencyArc(head, dependent, dataLine.getDependencyLabel());
										if (LOG.isTraceEnabled())
											LOG.trace(arc);
										dependencies.add(arc);
									}
									
									configuration = this.parserService.getInitialConfiguration(posTagSequence);
									TransitionSystem transitionSystem = TalismaneSession.getTransitionSystem();
									transitionSystem.predictTransitions(configuration, dependencies);
	
									sentenceCount++;
								} // is the configuration a valid one
							} // have we data lines?
						} else {
							// add a token to the current sentence
							hasLine = true;
							Matcher matcher = this.getPattern().matcher(line);
							if (!matcher.matches())
								throw new TalismaneException("Didn't match pattern \"" + regex + "\" on line " + lineNumber + ": " + line);
							
							if (matcher.groupCount()!=placeholderIndexMap.size()) {
								throw new TalismaneException("Expected " + placeholderIndexMap.size() + " matches (but found " + matcher.groupCount() + ") on line " + lineNumber);
							}
							
							int index = Integer.parseInt(matcher.group(placeholderIndexMap.get(INDEX_PLACEHOLDER)));
							String word =  matcher.group(placeholderIndexMap.get(TOKEN_PLACEHOLDER));
							String posTagCode = matcher.group(placeholderIndexMap.get(POSTAG_PLACEHOLDER));
							String depLabel = matcher.group(placeholderIndexMap.get(LABEL_PLACEHOLDER));
							int governorIndex = Integer.parseInt(matcher.group(placeholderIndexMap.get(GOVERNOR_PLACEHOLDER)));
							
							ParseDataLine dataLine = new ParseDataLine();
							dataLine.setLineNumber(this.lineNumber);
							dataLine.setIndex(index);
							dataLine.setWord(word);
							dataLine.setPosTagCode(posTagCode);
							dataLine.setDependencyLabel(depLabel);
							dataLine.setGovernorIndex(governorIndex);
							if (placeholderIndexMap.containsKey(FILENAME_PLACEHOLDER)) 
								dataLine.setOriginalFileName(matcher.group(placeholderIndexMap.get(FILENAME_PLACEHOLDER)));
							if (placeholderIndexMap.containsKey(ROW_PLACEHOLDER)) 
								dataLine.setOriginalLineNumber(Integer.parseInt(matcher.group(placeholderIndexMap.get(ROW_PLACEHOLDER))));
							if (placeholderIndexMap.containsKey(COLUMN_PLACEHOLDER)) 
								dataLine.setOriginalColumnNumber(Integer.parseInt(matcher.group(placeholderIndexMap.get(COLUMN_PLACEHOLDER))));
							
							dataLines.add(dataLine);
							
							if (this.lexicalEntryReader!=null) {
								LexicalEntry lexicalEntry = this.lexicalEntryReader.readEntry(line);
								lexicalEntries.add(lexicalEntry);
							}
						}
					}
				} // is configuration still null?
			} // have we reached the max sentence count?
			
			if (configuration==null) {
				scanner.close();
			}
			
			return configuration!=null;
		} finally {
			PerformanceMonitor.endTask("ParserRegexBasedCorpusReaderImpl.hasNextConfiguration");
		}
	}

	/**
	 * Returns true if the data line is valid, false otherwise.
	 * @param dataLine
	 * @return
	 */
	protected boolean checkDataLine(ParseDataLine dataLine) {
		return true;
	}

	/**
	 * Updates the data line prior to processing.
	 * At this point, empty lines may have been added to correspond to empty tokens that were added by filters.
	 * @param dataLines
	 */
	protected void updateDataLine(List<ParseDataLine> dataLines, int index) {
		// nothing to do in the base class
	}

	Token addToken(PretokenisedSequence pretokenisedSequence, String tokenText) {
		Token token = null;
		if (tokenText.equals("_")) {
			token = pretokenisedSequence.addToken("");
		} else {
			token = pretokenisedSequence.addToken(tokenText.replace("_", " "));
		}
		return token;
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
		public ParseDataLine() { }
		private int lineNumber = 0;
		private int index;
		private String word = "";
		private String posTagCode = "";
		private int governorIndex;
		private String dependencyLabel = "";
		private Token token;
		private String originalFileName = "";
		private int originalLineNumber = 0;
		private int originalColumnNumber = 0;
		private boolean skip = false;
		
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
		 * Should this data line be skipped or not? This should only be set for data lines corresponding to empty tokens.
		 * The empty token will be removed.
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

		public String toString() {
			String string = lineNumber + ": " + index + "," + word + "," + posTagCode + "," + governorIndex + "," + dependencyLabel;
			return string;
		}
	}

	/**
	 * If 0, all sentences will be read - otherwise will only read a certain number of sentences.
	 * @return
	 */
	public int getMaxSentenceCount() {
		return maxSentenceCount;
	}

	public void setMaxSentenceCount(int maxSentenceCount) {
		this.maxSentenceCount = maxSentenceCount;
	}
	
	public void addTokenSequenceFilter(TokenSequenceFilter tokenFilter) {
		this.tokenFilters.add(tokenFilter);
	}

	@Override
	public Map<String, Object> getAttributes() {
		Map<String,Object> attributes = new HashMap<String, Object>();

		attributes.put("maxSentenceCount", this.maxSentenceCount);
		attributes.put("transitionSystem", TalismaneSession.getTransitionSystem().getClass().getSimpleName());
		
		int i = 0;
		for (TokenSequenceFilter tokenFilter : this.tokenFilters) {
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
			if (indexPos<0)
				throw new TalismaneException("The regex must contain the string \"" + INDEX_PLACEHOLDER + "\": " + regex);

			int tokenPos = regex.indexOf(TOKEN_PLACEHOLDER);
			if (tokenPos<0)
				throw new TalismaneException("The regex must contain the string \"" + TOKEN_PLACEHOLDER + "\"");
			
			int posTagPos = regex.indexOf(POSTAG_PLACEHOLDER);
			if (posTagPos<0)
				throw new TalismaneException("The regex must contain the string \"" + POSTAG_PLACEHOLDER + "\"");
			
			int labelPos = regex.indexOf(LABEL_PLACEHOLDER);
			if (labelPos<0)
				throw new TalismaneException("The regex must contain the string \"" + LABEL_PLACEHOLDER + "\"");
			
			int governorPos = regex.indexOf(GOVERNOR_PLACEHOLDER);
			if (governorPos<0)
				throw new TalismaneException("The regex must contain the string \"" + GOVERNOR_PLACEHOLDER + "\"");
			
			int filenamePos = regex.indexOf(FILENAME_PLACEHOLDER);
			int rowNumberPos = regex.indexOf(ROW_PLACEHOLDER);
			int columnNumberPos = regex.indexOf(COLUMN_PLACEHOLDER);
			Map<Integer, String> placeholderMap = new TreeMap<Integer, String>();
			placeholderMap.put(indexPos, INDEX_PLACEHOLDER);
			placeholderMap.put(tokenPos, TOKEN_PLACEHOLDER);
			placeholderMap.put(posTagPos, POSTAG_PLACEHOLDER);
			placeholderMap.put(labelPos, LABEL_PLACEHOLDER);
			placeholderMap.put(governorPos, GOVERNOR_PLACEHOLDER);
			if (filenamePos>=0)
				placeholderMap.put(filenamePos, FILENAME_PLACEHOLDER);
			if (rowNumberPos>=0)
				placeholderMap.put(rowNumberPos, ROW_PLACEHOLDER);
			if (columnNumberPos>=0)
				placeholderMap.put(columnNumberPos, COLUMN_PLACEHOLDER);
			
			int i = 1;
			for (String placeholderName : placeholderMap.values()) {
				placeholderIndexMap.put(placeholderName, i++);
			}
			
			String regexWithGroups = regex.replace(INDEX_PLACEHOLDER, "(.+)");
			regexWithGroups = regexWithGroups.replace(TOKEN_PLACEHOLDER, "(.*)");
			regexWithGroups = regexWithGroups.replace(POSTAG_PLACEHOLDER, "(.+)");
			regexWithGroups = regexWithGroups.replace(LABEL_PLACEHOLDER, "(.+)");
			regexWithGroups = regexWithGroups.replace(GOVERNOR_PLACEHOLDER, "(.+)");
			regexWithGroups = regexWithGroups.replace(FILENAME_PLACEHOLDER, "(.+)");
			regexWithGroups = regexWithGroups.replace(ROW_PLACEHOLDER, "(.+)");
			regexWithGroups = regexWithGroups.replace(COLUMN_PLACEHOLDER, "(.+)");
			
			this.pattern = Pattern.compile(regexWithGroups);
		}
		return pattern;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public LexicalEntryReader getLexicalEntryReader() {
		return lexicalEntryReader;
	}

	public void setLexicalEntryReader(LexicalEntryReader lexicalEntryReader) {
		this.lexicalEntryReader = lexicalEntryReader;
	}
	
	
}
