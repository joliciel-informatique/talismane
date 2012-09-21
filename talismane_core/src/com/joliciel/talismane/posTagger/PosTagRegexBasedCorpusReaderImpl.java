package com.joliciel.talismane.posTagger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.posTagger.PosTagSetImpl.UnknownPosTagException;
import com.joliciel.talismane.tokeniser.PretokenisedSequence;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;

class PosTagRegexBasedCorpusReaderImpl implements
		PosTagRegexBasedCorpusReader {
	private String regex;
	private Pattern pattern;
	private Scanner scanner;
	private PosTagSequence sentence = null;
	private List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();
	private int tokenGroupIndex = 0;
	private int posTagGroupIndex = 0;
	private int lineNumber = 0;
	
	private PosTaggerServiceInternal posTaggerServiceInternal;
	private TokeniserService tokeniserService;
	
	public PosTagRegexBasedCorpusReaderImpl(Reader reader, String regex) {
		this.regex = regex;
		
		int tokenPos = regex.indexOf("TOKEN");
		int posTagPos = regex.indexOf("POSTAG");
		if (tokenPos<0)
			throw new TalismaneException("The regex must contain the string \"TOKEN\"");
		if (posTagPos<0)
			throw new TalismaneException("The regex must contain the string \"POSTAG\"");
		
		if (tokenPos<posTagPos) {
			tokenGroupIndex = 1;
			posTagGroupIndex = 2;
		} else {
			tokenGroupIndex = 2;
			posTagGroupIndex = 1;
		}
		
		String regexWithGroups = regex.replace("TOKEN", "(.*)");
		regexWithGroups = regexWithGroups.replace("POSTAG", "(.+)");
		this.pattern = Pattern.compile(regexWithGroups);
		this.scanner = new Scanner(reader);
	}
	
	@Override
	public boolean hasNextSentence() {
		while (sentence==null) {
			PretokenisedSequence tokenSequence = tokeniserService.getEmptyPretokenisedSequence();
			List<PosTag> posTags = new ArrayList<PosTag>();
			boolean hasLine = false;
			if (!scanner.hasNextLine())
				break;
			while (scanner.hasNextLine()&&sentence==null) {
				String line = scanner.nextLine().replace("\r", "");
				lineNumber++;
				if (line.length()==0) {
					if (!hasLine)
						continue;
					
					tokenSequence.cleanSlate();
					for (TokenFilter tokenFilter : this.tokenFilters) {
						tokenFilter.apply(tokenSequence);
					}
					
					sentence = posTaggerServiceInternal.getPosTagSequence(tokenSequence, tokenSequence.size());
					int i = 0;
					PosTagSet posTagSet = TalismaneSession.getPosTagSet();
    				for (PosTag posTag : posTags) {
    					Token token = tokenSequence.get(i++);
    					if (tokenSequence.getTokensAdded().contains(token)) {
    						Decision<PosTag> nullDecision = posTagSet.createDefaultDecision(PosTag.NULL_POS_TAG);
    						PosTaggedToken emptyToken = posTaggerServiceInternal.getPosTaggedToken(token, nullDecision);
    						sentence.add(emptyToken);
    						token = tokenSequence.get(i++);
    					}
    					Decision<PosTag> corpusDecision = posTagSet.createDefaultDecision(posTag);
    					PosTaggedToken posTaggedToken = posTaggerServiceInternal.getPosTaggedToken(token, corpusDecision);
    					sentence.add(posTaggedToken);
    				}
				} else {
					hasLine = true;
					Matcher matcher = pattern.matcher(line);
					if (!matcher.matches())
						throw new TalismaneException("Didn't match pattern on line " + lineNumber);
					
					if (matcher.groupCount()!=2) {
						throw new TalismaneException("Expected 2 matches (but found " + matcher.groupCount() + ") on line " + lineNumber);
					}
					
					String token = matcher.group(tokenGroupIndex);
					String posTagCode = matcher.group(posTagGroupIndex);
					this.addToken(tokenSequence, token);
					
    				PosTagSet posTagSet = TalismaneSession.getPosTagSet();
    				PosTag posTag = null;
    				try {
    					posTag = posTagSet.getPosTag(posTagCode);
    				} catch (UnknownPosTagException upte) {
    					throw new TalismaneException("Unknown posTag on line " + lineNumber + ": " + posTagCode);
    				}
    				posTags.add(posTag);
				}
			}
		}
		return (sentence!=null);
	}

	void addToken(PretokenisedSequence pretokenisedSequence, String token) {
		if (token.equals("_")) {
			pretokenisedSequence.addToken("");
		} else {
			if (pretokenisedSequence.size()==0) {
				// do nothing
			} else if (pretokenisedSequence.get(pretokenisedSequence.size()-1).getText().endsWith("'")) {
				// do nothing
			} else if (token.equals(".")||token.equals(",")||token.equals(")")||token.equals("]")) {
				// do nothing
			} else {
				// add a space
				pretokenisedSequence.addToken(" ");
			}
			pretokenisedSequence.addToken(token.replace("_", " "));
		}
	}
	
	@Override
	public PosTagSequence nextSentence() {
		PosTagSequence nextSentence = sentence;
		sentence = null;
		return nextSentence;
	}

	@Override
	public void addTokenFilter(TokenFilter tokenFilter) {
		this.tokenFilters.add(tokenFilter);
	}
	
	@Override
	public String getRegex() {
		return regex;
	}

	public PosTaggerServiceInternal getPosTaggerServiceInternal() {
		return posTaggerServiceInternal;
	}

	public void setPosTaggerServiceInternal(
			PosTaggerServiceInternal posTaggerServiceInternal) {
		this.posTaggerServiceInternal = posTaggerServiceInternal;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

}
