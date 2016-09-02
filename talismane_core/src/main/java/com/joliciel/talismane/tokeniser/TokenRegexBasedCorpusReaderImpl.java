package com.joliciel.talismane.tokeniser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.CoNLLFormatter;

class TokenRegexBasedCorpusReaderImpl implements TokenRegexBasedCorpusReader {
	private static final Logger LOG = LoggerFactory.getLogger(TokenRegexBasedCorpusReaderImpl.class);

	private String regex = TokenRegexBasedCorpusReader.DEFAULT_REGEX;
	private static final String TOKEN_PLACEHOLDER = "%TOKEN%";
	private static final String FILENAME_PLACEHOLDER = "%FILENAME%";
	private static final String ROW_PLACEHOLDER = "%ROW%";
	private static final String COLUMN_PLACEHOLDER = "%COLUMN%";
	private Map<String, Integer> placeholderIndexMap = new HashMap<String, Integer>();
	private Pattern pattern;
	private Scanner scanner;
	private PretokenisedSequence tokenSequence = null;

	private List<TokenSequenceFilter> tokenSequenceFilters = new ArrayList<TokenSequenceFilter>();
	private List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();
	private TokenSequenceFilter tokenFilterWrapper = null;

	private int lineNumber = 0;
	private int maxSentenceCount = 0;
	private int startSentence = 0;
	private int sentenceCount = 0;
	private int includeIndex = -1;
	private int excludeIndex = -1;
	private int crossValidationSize = 0;

	private TokeniserService tokeniserService;
	private TokenFilterService tokenFilterService;

	private SentenceDetectorAnnotatedCorpusReader sentenceReader = null;
	private final TalismaneSession talismaneSession;

	public TokenRegexBasedCorpusReaderImpl(Reader reader, TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
		this.scanner = new Scanner(reader);
	}

	@Override
	public boolean hasNextTokenSequence() {
		if (maxSentenceCount > 0 && sentenceCount >= maxSentenceCount) {
			// we've reached the end, do nothing
		} else {
			while (tokenSequence == null) {
				boolean hasLine = false;
				if (!scanner.hasNextLine())
					break;
				while (scanner.hasNextLine() || hasLine) {
					String line = "";
					if (scanner.hasNextLine())
						line = scanner.nextLine().replace("\r", "");
					lineNumber++;
					if (line.length() == 0) {
						if (!hasLine)
							continue;

						// end of sentence

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
						LOG.debug("sentenceCount: " + sentenceCount);

						if (!includeMe) {
							hasLine = false;
							tokenSequence = null;
							continue;
						}

						tokenSequence.cleanSlate();

						// first apply the token filters - which might replace
						// the text of an individual token
						// with something else
						if (tokenFilterWrapper == null) {
							tokenFilterWrapper = tokenFilterService.getTokenSequenceFilter(this.tokenFilters);
						}
						tokenFilterWrapper.apply(tokenSequence);

						// now apply the token sequence filters
						for (TokenSequenceFilter tokenFilter : this.tokenSequenceFilters) {
							tokenFilter.apply(tokenSequence);
						}

						break;
					} else {
						hasLine = true;

						if (tokenSequence == null) {
							if (sentenceReader != null && sentenceReader.hasNextSentence()) {
								tokenSequence = new PretokenisedSequence(sentenceReader.nextSentence(), talismaneSession);
							} else {
								tokenSequence = new PretokenisedSequence(talismaneSession);
							}
						}

						Matcher matcher = this.getPattern().matcher(line);
						if (!matcher.matches())
							throw new TalismaneException("Didn't match pattern \"" + regex + "\" on line " + lineNumber + ": " + line);

						if (matcher.groupCount() != placeholderIndexMap.size()) {
							throw new TalismaneException(
									"Expected " + placeholderIndexMap.size() + " matches (but found " + matcher.groupCount() + ") on line " + lineNumber);
						}

						String word = matcher.group(placeholderIndexMap.get(TOKEN_PLACEHOLDER));
						word = CoNLLFormatter.fromCoNLL(word);
						Token token = tokenSequence.addToken(word);

						if (placeholderIndexMap.containsKey(FILENAME_PLACEHOLDER))
							token.setFileName(matcher.group(placeholderIndexMap.get(FILENAME_PLACEHOLDER)));
						if (placeholderIndexMap.containsKey(ROW_PLACEHOLDER))
							token.setLineNumber(Integer.parseInt(matcher.group(placeholderIndexMap.get(ROW_PLACEHOLDER))));
						if (placeholderIndexMap.containsKey(COLUMN_PLACEHOLDER))
							token.setColumnNumber(Integer.parseInt(matcher.group(placeholderIndexMap.get(COLUMN_PLACEHOLDER))));
					}
				}
			}
		}
		return (tokenSequence != null);
	}

	@Override
	public TokenSequence nextTokenSequence() {
		TokenSequence nextSequence = tokenSequence;
		tokenSequence = null;
		return nextSequence;
	}

	@Override
	public void addTokenSequenceFilter(TokenSequenceFilter tokenFilter) {
		this.tokenSequenceFilters.add(tokenFilter);
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
	public String getRegex() {
		return regex;
	}

	@Override
	public void setRegex(String regex) {
		this.regex = regex;
	}

	public Pattern getPattern() {
		if (this.pattern == null) {
			int tokenPos = regex.indexOf(TOKEN_PLACEHOLDER);
			if (tokenPos < 0)
				throw new TalismaneException("The regex must contain the string \"" + TOKEN_PLACEHOLDER + "\"");

			int filenamePos = regex.indexOf(FILENAME_PLACEHOLDER);
			int rowNumberPos = regex.indexOf(ROW_PLACEHOLDER);
			int columnNumberPos = regex.indexOf(COLUMN_PLACEHOLDER);
			Map<Integer, String> placeholderMap = new TreeMap<Integer, String>();
			placeholderMap.put(tokenPos, TOKEN_PLACEHOLDER);

			if (filenamePos >= 0)
				placeholderMap.put(filenamePos, FILENAME_PLACEHOLDER);
			if (rowNumberPos >= 0)
				placeholderMap.put(rowNumberPos, ROW_PLACEHOLDER);
			if (columnNumberPos >= 0)
				placeholderMap.put(columnNumberPos, COLUMN_PLACEHOLDER);

			int i = 1;
			for (String placeholderName : placeholderMap.values()) {
				placeholderIndexMap.put(placeholderName, i++);
			}

			String regexWithGroups = regex.replace(TOKEN_PLACEHOLDER, "(.*)");
			regexWithGroups = regexWithGroups.replace(FILENAME_PLACEHOLDER, "(.+)");
			regexWithGroups = regexWithGroups.replace(ROW_PLACEHOLDER, "(.+)");
			regexWithGroups = regexWithGroups.replace(COLUMN_PLACEHOLDER, "(.+)");

			this.pattern = Pattern.compile(regexWithGroups, Pattern.UNICODE_CHARACTER_CLASS);
		}
		return pattern;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public TokenFilterService getTokenFilterService() {
		return tokenFilterService;
	}

	public void setTokenFilterService(TokenFilterService tokenFilterService) {
		this.tokenFilterService = tokenFilterService;
	}

	@Override
	public Map<String, String> getCharacteristics() {
		Map<String, String> attributes = new LinkedHashMap<String, String>();

		attributes.put("maxSentenceCount", "" + this.maxSentenceCount);
		attributes.put("crossValidationSize", "" + this.crossValidationSize);
		attributes.put("includeIndex", "" + this.includeIndex);
		attributes.put("excludeIndex", "" + this.excludeIndex);

		int i = 0;
		for (TokenSequenceFilter tokenFilter : this.tokenSequenceFilters) {
			attributes.put("TokenSequenceFilter" + i, "" + tokenFilter.getClass().getSimpleName());

			i++;
		}
		i = 0;
		for (TokenFilter tokenFilter : this.tokenFilters) {
			attributes.put("TokenFilter" + i, "" + tokenFilter.getClass().getSimpleName());

			i++;
		}
		return attributes;
	}

	@Override
	public int getMaxSentenceCount() {
		return maxSentenceCount;
	}

	@Override
	public void setMaxSentenceCount(int maxSentenceCount) {
		this.maxSentenceCount = maxSentenceCount;
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
	public SentenceDetectorAnnotatedCorpusReader getSentenceReader() {
		return sentenceReader;
	}

	@Override
	public void setSentenceReader(SentenceDetectorAnnotatedCorpusReader sentenceReader) {
		this.sentenceReader = sentenceReader;
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