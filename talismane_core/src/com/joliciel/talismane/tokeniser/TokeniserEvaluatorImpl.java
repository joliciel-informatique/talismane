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
package com.joliciel.talismane.tokeniser;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.stats.FScoreCalculator;
import com.joliciel.talismane.utils.StringUtils;

class TokeniserEvaluatorImpl implements TokeniserEvaluator {
	private static final Log LOG = LogFactory.getLog(TokeniserEvaluatorImpl.class);
	private static final int NUM_CHARS = 20;
	Tokeniser tokeniser;
	
	private Pattern separatorPattern = null;
	
	@Override
	public FScoreCalculator<TokeniserOutcome> evaluate(
			TokeniserAnnotatedCorpusReader corpusReader,
			Writer errorWriter) {
		FScoreCalculator<TokeniserOutcome> fScoreCalculator = new FScoreCalculator<TokeniserOutcome>();
		
		//add f-score per tagger module, needs to somehow tie back to decisions the actual tags, rather than the splits
		Map<String, FScoreCalculator<TokeniserOutcome>> taggerFScoreCalculators = new TreeMap<String, FScoreCalculator<TokeniserOutcome>>();
		Map<String, List<String>> errorMap = new TreeMap<String, List<String>>();
		
		while (corpusReader.hasNextSentence()) {
			List<Integer> realSplits = new ArrayList<Integer>();
			String sentence = corpusReader.nextSentence(realSplits);
			
			List<TokenisedAtomicTokenSequence> tokeniserDecisionTagSequences = tokeniser.tokeniseWithDecisions(sentence);
			TokenisedAtomicTokenSequence tokeniserDecisionTagSequence = tokeniserDecisionTagSequences.get(0);
			TokenSequence tokenSequence = tokeniserDecisionTagSequence.inferTokenSequence();
			List<Integer> guessedSplits = tokenSequence.getTokenSplits();
			
			if (LOG.isDebugEnabled()) {
	    		int pos = 0;
	    		StringBuilder sb = new StringBuilder();
	    		for (int split : realSplits) {
	    			String aToken = sentence.substring(pos, split);
	    			sb.append('|');
	    			sb.append(aToken);
	    			pos = split;
	    		}
	    		int pos2 = 0;
	    		StringBuilder sb2 = new StringBuilder();
	    		for (int split : guessedSplits) {
	    			String aToken = sentence.substring(pos2, split);
	    			sb2.append('|');
	    			sb2.append(aToken);
	    			pos2 = split;
	    		}

	    		LOG.debug("Real:    " + sb.toString());
	    		LOG.debug("Guessed: " + sb2.toString());
			}
    		
			List<Integer> possibleSplits = new ArrayList<Integer>();
			Matcher matcher = separatorPattern.matcher(sentence);
			while (matcher.find()) {
				possibleSplits.add(matcher.start());
				possibleSplits.add(matcher.end());
			}
			
			for (TaggedToken<TokeniserOutcome> guessTag : tokeniserDecisionTagSequence) {
				TokeniserOutcome guessDecision = guessTag.getTag();
				boolean realSplit = realSplits.contains(guessTag.getToken().getStartIndex());
				
				TokeniserOutcome realDecision = realSplit ? TokeniserOutcome.DOES_SEPARATE : TokeniserOutcome.DOES_NOT_SEPARATE;
				
				if (!realDecision.equals(guessDecision)) {
					int start1 = guessTag.getToken().getStartIndex() - NUM_CHARS;
					int end1 = guessTag.getToken().getStartIndex() + NUM_CHARS;
					
					
					if (start1<0) start1=0;
					String startString = sentence.substring(start1, guessTag.getToken().getStartIndex());
					startString = StringUtils.padLeft(startString, NUM_CHARS);

					if (end1>=sentence.length()) end1 = sentence.length()-1;
					
					String error = "Guessed " + guessDecision + ", Expected " + realDecision + ". Tokens: " + startString + "[]" + sentence.substring(guessTag.getToken().getStartIndex(), end1);
					LOG.debug(error);
					for (String authority : guessTag.getDecision().getAuthorities()) {
						List<String> errors = errorMap.get(authority);
						if (errors==null) {
							errors = new ArrayList<String>();
							errorMap.put(authority, errors);
						}
						errors.add(error);
					}
				}
				fScoreCalculator.increment(realDecision, guessDecision);
				for (String authority : guessTag.getDecision().getAuthorities()) {
					FScoreCalculator<TokeniserOutcome> taggerFScoreCalculator = taggerFScoreCalculators.get(authority);
					if (taggerFScoreCalculator==null) {
						taggerFScoreCalculator = new FScoreCalculator<TokeniserOutcome>();
						taggerFScoreCalculators.put(authority, taggerFScoreCalculator);
					}
					taggerFScoreCalculator.increment(realDecision, guessDecision);
				}
			} // next decision
		} // next sentence
		
		for (String tagger : taggerFScoreCalculators.keySet()) {
			LOG.debug("###### Tagger " + tagger);
			FScoreCalculator<TokeniserOutcome> taggerFScoreCalculator = taggerFScoreCalculators.get(tagger);
			LOG.debug("###### Tagger " + tagger + ": f-score = " + taggerFScoreCalculator.getTotalFScore());
		}
		
		if (errorWriter!=null) {
			try {
				for (String tagger : taggerFScoreCalculators.keySet()) {
					FScoreCalculator<TokeniserOutcome> taggerFScoreCalculator = taggerFScoreCalculators.get(tagger);
					errorWriter.write("###### Tagger " + tagger + ": f-score = " + taggerFScoreCalculator.getTotalFScore() + "\n");
					errorWriter.write("Total " + (taggerFScoreCalculator.getTotalTruePositiveCount() + taggerFScoreCalculator.getTotalFalseNegativeCount()) + "\n");
					errorWriter.write("True + " + taggerFScoreCalculator.getTotalTruePositiveCount() + "\n");
					errorWriter.write("False- " + taggerFScoreCalculator.getTotalFalseNegativeCount() + "\n");
					errorWriter.write("False+ " + taggerFScoreCalculator.getTotalFalsePositiveCount() + "\n");
					for (TokeniserOutcome outcome : taggerFScoreCalculator.getOutcomeSet()) {
						errorWriter.write(outcome + " total  " + (taggerFScoreCalculator.getTruePositiveCount(outcome) + taggerFScoreCalculator.getFalseNegativeCount(outcome)) + "\n");
						errorWriter.write(outcome + " true + " + (taggerFScoreCalculator.getTruePositiveCount(outcome)) + "\n");
						errorWriter.write(outcome + " false- " + (taggerFScoreCalculator.getFalseNegativeCount(outcome)) + "\n");
						errorWriter.write(outcome + " false+ " + (taggerFScoreCalculator.getFalsePositiveCount(outcome)) + "\n");
						errorWriter.write(outcome + " precis " + (taggerFScoreCalculator.getPrecision(outcome)) + "\n");
						errorWriter.write(outcome + " recall " + (taggerFScoreCalculator.getRecall(outcome)) + "\n");
						errorWriter.write(outcome + " fscore " + (taggerFScoreCalculator.getFScore(outcome)) + "\n");
					}
					
					List<String> errors = errorMap.get(tagger);
					if (errors!=null) {
						for (String error : errors) {
							errorWriter.write(error + "\n");
						}
					}
				}
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}
		return fScoreCalculator;
	}

	public Tokeniser getTokeniser() {
		return tokeniser;
	}

	public void setTokeniser(Tokeniser tokeniser) {
		this.tokeniser = tokeniser;
	}


	public Pattern getSeparatorPattern() {
		return separatorPattern;
	}

	public void setSeparatorPattern(Pattern separatorPattern) {
		this.separatorPattern = separatorPattern;
	}

}
