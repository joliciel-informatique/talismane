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

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.tokeniser.PretokenisedSequence;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.utils.CoNLLFormatter;

class TokenRegexBasedCorpusReaderImpl implements
		TokenRegexBasedCorpusReader {
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
	private int sentenceCount = 0;
	
	private TokeniserService tokeniserService;
	private TokenFilterService tokenFilterService;
	
	public TokenRegexBasedCorpusReaderImpl(Reader reader) {
		this.scanner = new Scanner(reader);
	}
	
	@Override
	public boolean hasNextTokenSequence() {
		if (maxSentenceCount>0 && sentenceCount>=maxSentenceCount) {
			// we've reached the end, do nothing
		} else {
			while (tokenSequence==null) {
				boolean hasLine = false;
				if (!scanner.hasNextLine())
					break;
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine().replace("\r", "");
					lineNumber++;
					if (line.length()==0) {
						if (!hasLine)
							continue;
						
						tokenSequence.cleanSlate();
						
						// first apply the token filters - which might replace the text of an individual token
						// with something else
						if (tokenFilterWrapper==null) {
							tokenFilterWrapper = tokenFilterService.getTokenSequenceFilter(this.tokenFilters);
						}
						tokenFilterWrapper.apply(tokenSequence);
						
						// now apply the token sequence filters
						for (TokenSequenceFilter tokenFilter : this.tokenSequenceFilters) {
							tokenFilter.apply(tokenSequence);
						}
						sentenceCount++;
						break;
					} else {
						hasLine = true;
						
						if (tokenSequence==null) {
							tokenSequence = tokeniserService.getEmptyPretokenisedSequence();
						}
						
						Matcher matcher = this.getPattern().matcher(line);
						if (!matcher.matches())
							throw new TalismaneException("Didn't match pattern \"" + regex + "\" on line " + lineNumber + ": " + line);
						
						if (matcher.groupCount()!=placeholderIndexMap.size()) {
							throw new TalismaneException("Expected " + placeholderIndexMap.size() + " matches (but found " + matcher.groupCount() + ") on line " + lineNumber);
						}
						
						String word =  matcher.group(placeholderIndexMap.get(TOKEN_PLACEHOLDER));
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
		return (tokenSequence!=null);
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
			if (tokenPos<0)
				throw new TalismaneException("The regex must contain the string \"" + TOKEN_PLACEHOLDER + "\"");
				
			int filenamePos = regex.indexOf(FILENAME_PLACEHOLDER);
			int rowNumberPos = regex.indexOf(ROW_PLACEHOLDER);
			int columnNumberPos = regex.indexOf(COLUMN_PLACEHOLDER);
			Map<Integer, String> placeholderMap = new TreeMap<Integer, String>();
			placeholderMap.put(tokenPos, TOKEN_PLACEHOLDER);

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
			
			String regexWithGroups = regex.replace(TOKEN_PLACEHOLDER, "(.*)");
			regexWithGroups = regexWithGroups.replace(FILENAME_PLACEHOLDER, "(.+)");
			regexWithGroups = regexWithGroups.replace(ROW_PLACEHOLDER, "(.+)");
			regexWithGroups = regexWithGroups.replace(COLUMN_PLACEHOLDER, "(.+)");
			
			this.pattern = Pattern.compile(regexWithGroups);
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
		Map<String,String> characteristics = new LinkedHashMap<String, String>();
		return characteristics;
	}


	public int getMaxSentenceCount() {
		return maxSentenceCount;
	}

	public void setMaxSentenceCount(int maxSentenceCount) {
		this.maxSentenceCount = maxSentenceCount;
	}
}