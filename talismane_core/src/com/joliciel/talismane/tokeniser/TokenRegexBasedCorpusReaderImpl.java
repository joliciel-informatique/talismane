package com.joliciel.talismane.tokeniser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.tokeniser.PretokenisedSequence;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;

class TokenRegexBasedCorpusReaderImpl implements
		TokenRegexBasedCorpusReader {
	private String regex = TokenRegexBasedCorpusReader.DEFAULT_REGEX;
	private Pattern pattern;
	private Scanner scanner;
	private PretokenisedSequence tokenSequence = null;
	
	private List<TokenSequenceFilter> tokenSequenceFilters = new ArrayList<TokenSequenceFilter>();
	private List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();
	private TokenSequenceFilter tokenFilterWrapper = null;
	
	private int lineNumber = 0;
	
	private TokeniserService tokeniserService;
	private TokenFilterService tokenFilterService;
	
	public TokenRegexBasedCorpusReaderImpl(Reader reader) {
		this.scanner = new Scanner(reader);
	}
	
	@Override
	public boolean hasNextTokenSequence() {
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
					for (TokenSequenceFilter tokenFilter : this.tokenSequenceFilters) {
						tokenFilter.apply(tokenSequence);
					}
					if (tokenFilterWrapper==null) {
						tokenFilterWrapper = tokenFilterService.getTokenSequenceFilter(this.tokenFilters);
					}
					tokenFilterWrapper.apply(tokenSequence);
					
					break;
				} else {
					hasLine = true;
					Matcher matcher = this.getPattern().matcher(line);
					if (!matcher.matches())
						throw new TalismaneException("Didn't match pattern \"" + regex + "\" on line " + lineNumber + ": " + line);
					
					if (matcher.groupCount()!=1) {
						throw new TalismaneException("Expected 1 match (but found " + matcher.groupCount() + ") on line " + lineNumber);
					}
					
					if (tokenSequence==null) {
						tokenSequence = tokeniserService.getEmptyPretokenisedSequence();
					}
					
					String token = matcher.group(1);
					this.addToken(tokenSequence, token);
				}
			}
		}
		return (tokenSequence!=null);
	}

	void addToken(PretokenisedSequence pretokenisedSequence, String tokenText) {
		if (tokenText.equals("_")) {
			pretokenisedSequence.addToken("");
		} else {
			if (pretokenisedSequence.size()==0) {
				// do nothing
			} else if (pretokenisedSequence.get(pretokenisedSequence.size()-1).getText().endsWith("'")) {
				// do nothing
			} else if (tokenText.equals(".")||tokenText.equals(",")||tokenText.equals(")")||tokenText.equals("]")) {
				// do nothing
			} else {
				// add a space
				pretokenisedSequence.addToken(" ");
			}
			pretokenisedSequence.addToken(tokenText.replace("_", " "));
		}
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
		if (this.pattern==null) {
			this.pattern = Pattern.compile(regex);
		}
		return this.pattern;
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

}
