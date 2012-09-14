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
package com.joliciel.frenchTreebank.export;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.frenchTreebank.PhraseUnit;
import com.joliciel.frenchTreebank.Sentence;
import com.joliciel.frenchTreebank.TreebankReader;
import com.joliciel.frenchTreebank.TreebankService;
import com.joliciel.frenchTreebank.util.CSVFormatter;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.utils.util.PerformanceMonitor;

/**
 * A token reader for the French Treebank corpus.
 * @author Assaf Urieli
 *
 */
class FrenchTreebankTokenReader implements TokeniserAnnotatedCorpusReader, PosTagAnnotatedCorpusReader {
    private static final Log LOG = LogFactory.getLog(FrenchTreebankTokenReader.class);
	TreebankService treebankService;
	TokeniserService tokeniserService;
	PosTaggerService posTaggerService;
	
	FtbPosTagMapper ftbPosTagMapper;
	Writer csvFileErrorWriter = null;
	private boolean ignoreCase = true;
	private List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();
	TreebankReader treebankReader;
	
	public FrenchTreebankTokenReader(TreebankReader treebankReader) {
		this.treebankReader = treebankReader;
	}

	@Override
	public boolean hasNextSentence() {
		return treebankReader.hasNextSentence();
	}

	@Override
	public PosTagSequence nextSentence() {
		if (this.ftbPosTagMapper==null) {
			throw new RuntimeException("Cannot get next PosTagSequence without PosTagMapper");
		}
		List<Integer> tokenSplits = new ArrayList<Integer>();
		PosTagSequence posTagSequence = this.nextSentenceInternal(tokenSplits);
		return posTagSequence;
	}

	@Override
	public String nextSentence(List<Integer> tokenSplits) {
		PosTagSequence posTagSequence = this.nextSentenceInternal(tokenSplits);
		return posTagSequence.getTokenSequence().getSentence();
	}
	
	PosTagSequence nextSentenceInternal(List<Integer> tokenSplits) {
		PerformanceMonitor.startTask("FrenchTreebankTokenReader.nextSentenceInternal");
		try {
			Sentence sentence = treebankReader.nextSentence();
			LOG.debug("Sentence " + sentence.getSentenceNumber());
			String text = sentence.getText();
			// get rid of duplicate white space
			Pattern duplicateWhiteSpace = Pattern.compile("\\s[\\s]+");
			text = duplicateWhiteSpace.matcher(text).replaceAll(" ");
	
			// there's no guarantee that the phrase units align to the original sentence text
			// given the issues we had for aligning sentences in the first place
			List<PhraseUnit> phraseUnits = sentence.getAllPhraseUnits();
			LOG.trace("Phrase units: " + phraseUnits.size());
			Pattern separators = Tokeniser.SEPARATORS;
			Pattern whitespace = Pattern.compile("\\s+");
	
			Matcher matcher = separators.matcher(text);
			List<String> allTokens = new ArrayList<String>();
			int currentPos = 0;
			while (matcher.find()) {
				if (matcher.start()>currentPos) {
					String leftoverToken = text.substring(currentPos,matcher.start());
					allTokens.add(leftoverToken);
				}
				String token = text.substring(matcher.start(), matcher.end());
				allTokens.add(token);
				currentPos = matcher.end();
			}
			if (currentPos<text.length())
				allTokens.add(text.substring(currentPos));
			
			TokenSequence tokenSequence = this.tokeniserService.getTokenSequence(text);
			List<PosTaggedToken> posTaggedTokens = new ArrayList<PosTaggedToken>();
			
			PhraseUnitReader phraseUnitReader = new ComplexPhraseUnitReaderWithEmptyTokens(phraseUnits);
	
			phraseUnitReader.setTreebankService(treebankService);
			if (ftbPosTagMapper!=null)
				phraseUnitReader.setFtbPosTagMapper(ftbPosTagMapper);
			
			String phraseUnitText = phraseUnitReader.nextString();
	
			LOG.trace("phrase unit: " + phraseUnitText);
			currentPos = 0;
			int lastSplit = 0;
			tokenSplits.add(0);
			
			while (phraseUnitText!=null && phraseUnitText.length()==0) {
				tokenSplits.add(currentPos);
				Token aToken = tokenSequence.addEmptyToken(currentPos);
				PosTag posTag = phraseUnitReader.getPosTag();
				PosTaggedToken posTaggedToken = posTaggerService.getPosTaggedToken(aToken, posTag, 1.0);
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
				if ((!ignoreCase && phraseUnitText.equals(token))
						||(ignoreCase && phraseUnitText.equalsIgnoreCase(token))) {
					// exact match
					
					if (addEmptyTokenBeforeNextToken) {
						if (LOG.isTraceEnabled())
							LOG.trace("Adding empty token at " + (currentPos - token.length()));
						tokenSplits.add((currentPos - token.length()));
						Token emptyToken = tokenSequence.addEmptyToken((currentPos - token.length()));
						PosTaggedToken posTaggedToken2 = posTaggerService.getPosTaggedToken(emptyToken, emptyTokenPosTag, 1.0);
						posTaggedTokens.add(posTaggedToken2);
						addEmptyTokenBeforeNextToken = false;
					}
					
					if (LOG.isTraceEnabled())
						LOG.trace("Adding split " + currentPos);
					tokenSplits.add(currentPos);
	
					Token aToken = tokenSequence.addToken(lastSplit, currentPos);
					PosTag posTag = phraseUnitReader.getPosTag();
					PosTaggedToken posTaggedToken = posTaggerService.getPosTaggedToken(aToken, posTag, 1.0);
					posTaggedTokens.add(posTaggedToken);
	
					lastSplit = currentPos;
					phraseUnitText = phraseUnitReader.nextString();
					if (LOG.isTraceEnabled())
						LOG.trace("phrase unit: " + phraseUnitText);
					while (phraseUnitText!=null && phraseUnitText.length()==0) {
						Token emptyToken = null;
						emptyTokenPosTag = phraseUnitReader.getPosTag();
						phraseUnitText = phraseUnitReader.nextString();
						if (LOG.isTraceEnabled())
							LOG.trace("phrase unit: " + phraseUnitText);
						
						// Empty tokens need to be attached either to the right (auquel, duquel)
						// or to the left (du, des)
						if (phraseUnitText.equals("duquel")
							||phraseUnitText.equals("auquel")
							||phraseUnitText.equals("desquels")
							||phraseUnitText.equals("auxquels")
							||phraseUnitText.equals("desquelles")
							||phraseUnitText.equals("auxquelles"))	{
							// attach empty token to the "duquel" that follows it
							addEmptyTokenBeforeNextToken = true;
						} else {
							if (LOG.isTraceEnabled())
								LOG.trace("Adding empty token at " + currentPos);
							tokenSplits.add(currentPos);
							emptyToken = tokenSequence.addEmptyToken(currentPos);
							PosTaggedToken posTaggedToken2 = posTaggerService.getPosTaggedToken(emptyToken, emptyTokenPosTag, 1.0);
							posTaggedTokens.add(posTaggedToken2);
						}
					}
					inPhraseUnit = false;
				} else if (phraseUnitText.length()>=token.length() &&
						((!ignoreCase && phraseUnitText.substring(0, token.length()).equals(token))
								||(ignoreCase && phraseUnitText.substring(0, token.length()).equalsIgnoreCase(token)))
						) {
					// the current phrase unit text starts with this token
					phraseUnitText = phraseUnitText.substring(token.length());
					if (LOG.isTraceEnabled())
						LOG.trace("phrase unit: " + phraseUnitText);
					inPhraseUnit = true;
				} else if (token.length()==1 && whitespace.matcher(token).matches()) {
					// white space, always add split unless we're already inside white space
					if (!inPhraseUnit) {
						if (LOG.isTraceEnabled())
							LOG.trace("Adding split " + currentPos);
						tokenSplits.add(currentPos);
						tokenSequence.addToken(lastSplit, currentPos);
						lastSplit = currentPos;
					}
				} else {
					// non-white space, what to do? either we skip the token, or we skip the phrase unit!
					// for now let's assume it never happens and see what results!
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
	        		if (csvFileErrorWriter!=null) {
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
		        		// instead of throwing an error, write these to a file (or do both)
		        		// so we can catch them all in one fell swoop
						throw new RuntimeException("Unexpected text: " + token);
	        		}
				}
			}
			if (lastSplit<currentPos) {
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
	
			for (TokenFilter tokenFilter : this.tokenFilters) {
				LOG.debug("Applying filter: " + tokenFilter.getClass().getSimpleName());
				tokenFilter.apply(tokenSequence);
			}
			tokenSequence.finalise();
			
			PosTagSequence posTagSequence = this.posTaggerService.getPosTagSequence(tokenSequence, allTokens.size() / 2);
			int i = 0;
			for (Token token : tokenSequence) {
				LOG.debug("Token : \"" + token.getText() + "\" (was \"" + token.getOriginalText() + "\")");
				PosTaggedToken posTaggedToken = posTaggedTokens.get(i);
				if (token.equals(posTaggedToken.getToken())) {
					posTagSequence.add(posTaggedToken);
					i++;
				} else if (token.getStartIndex()==token.getEndIndex()) {
					LOG.debug("Adding null pos tag at position " + token.getStartIndex());
					PosTaggedToken emptyTagToken = posTaggerService.getPosTaggedToken(token, PosTag.NULL_POS_TAG, 1.0);
					posTagSequence.add(emptyTagToken);
				} else {
					throw new RuntimeException("Expected only empty tokens added. Postag Token = " + posTaggedToken.getToken().getText() + ", start: " + token.getStartIndex() + ", end:" + token.getEndIndex());
				}
			}
			return posTagSequence;
		} finally {
			PerformanceMonitor.endTask("FrenchTreebankTokenReader.nextSentenceInternal");
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

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}
	
	public void addTokenFilter(TokenFilter tokenFilter) {
		this.tokenFilters.add(tokenFilter);
	}

	@Override
	public Map<String, String> getCharacteristics() {
		Map<String,String> characteristics = new HashMap<String, String>();

		characteristics.put("treebankReader", treebankReader.getClass().getSimpleName());
		characteristics.putAll(this.treebankReader.getCharacteristics());
		if (ftbPosTagMapper!=null)
			characteristics.put("posTagSet", "" + ftbPosTagMapper.getPosTagSet().getName());
		characteristics.put("ignoreCase", "" + ignoreCase);
		
		int i = 0;
		for (TokenFilter tokenFilter : this.tokenFilters) {
			characteristics.put("filter" + i, "" + tokenFilter.getClass().getSimpleName());
			
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
	
	
}
