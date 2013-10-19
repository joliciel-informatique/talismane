///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;
import com.joliciel.talismane.tokeniser.patterns.TokenPatternMatchSequence;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternManager;

class TokenComparatorImpl implements TokenComparator {
	private static final Log LOG = LogFactory.getLog(TokenComparatorImpl.class);
	private List<TokenEvaluationObserver> observers = new ArrayList<TokenEvaluationObserver>();
	private int sentenceCount;
	private TokeniserServiceInternal tokeniserServiceInternal;
	private TokeniserDecisionFactory tokeniserDecisionFactory = new TokeniserDecisionFactory();
	private TokeniserAnnotatedCorpusReader referenceCorpusReader;
	private TokeniserAnnotatedCorpusReader evaluationCorpusReader;
	private TokeniserPatternManager tokeniserPatternManager;
	
	public TokenComparatorImpl(
			TokeniserAnnotatedCorpusReader referenceCorpusReader,
			TokeniserAnnotatedCorpusReader evaluationCorpusReader,
			TokeniserPatternManager tokeniserPatternManager) {
		super();
		this.referenceCorpusReader = referenceCorpusReader;
		this.evaluationCorpusReader = evaluationCorpusReader;
		this.tokeniserPatternManager = tokeniserPatternManager;
	}

	@Override
	public void compare() {
		int sentenceIndex = 0;
		while (referenceCorpusReader.hasNextTokenSequence()) {
			TokenSequence realSequence = referenceCorpusReader.nextTokenSequence();
			
			TokenSequence guessedSequence = null;
			if (evaluationCorpusReader.hasNextTokenSequence())
				guessedSequence = evaluationCorpusReader.nextTokenSequence();
			else {
				throw new TalismaneException("Wrong number of sentences in eval corpus: " + realSequence.getText());
			}
			
			Sentence sentence = realSequence.getSentence();
			
			Set<TokenPlaceholder> placeholders = new HashSet<TokenPlaceholder>();
			
			// Initially, separate the sentence into tokens using the separators provided
			TokenSequence realAtomicSequence = this.tokeniserServiceInternal.getTokenSequence(sentence, Tokeniser.SEPARATORS, placeholders);
			TokenSequence guessedAtomicSequence = this.tokeniserServiceInternal.getTokenSequence(guessedSequence.getSentence(), Tokeniser.SEPARATORS, placeholders);
			
			List<TokenPatternMatchSequence> matchingSequences = new ArrayList<TokenPatternMatchSequence>();
			Map<Token,Set<TokenPatternMatchSequence>> tokenMatchSequenceMap = new HashMap<Token, Set<TokenPatternMatchSequence>>();
			Set<Token> matchedTokens = new HashSet<Token>();
			
			for (TokenPattern parsedPattern : tokeniserPatternManager.getParsedTestPatterns()) {
				List<TokenPatternMatchSequence> matchesForThisPattern = parsedPattern.match(realAtomicSequence);
				for (TokenPatternMatchSequence matchSequence : matchesForThisPattern) {
					matchingSequences.add(matchSequence);
					matchedTokens.addAll(matchSequence.getTokensToCheck());
					
					Token token = null;
					for (Token aToken : matchSequence.getTokensToCheck()) {
						token = aToken;
						if (!aToken.isWhiteSpace()) {
							break;
						}
					}
					
					Set<TokenPatternMatchSequence> matchSequences = tokenMatchSequenceMap.get(token);
					if (matchSequences==null) {
						matchSequences = new TreeSet<TokenPatternMatchSequence>();
						tokenMatchSequenceMap.put(token, matchSequences);
					}
					matchSequences.add(matchSequence);
				}
			}


			TokenisedAtomicTokenSequence guess = tokeniserServiceInternal.getTokenisedAtomicTokenSequence(realSequence.getSentence(), 0);
			
			int i=0;
			int mismatches = 0;
			for (Token token : realAtomicSequence) {
				if (!token.getText().equals(guessedAtomicSequence.get(i).getToken().getText())) {
					// skipped stuff at start of sentence on guess, if it's been through the parser
					Decision<TokeniserOutcome> decision = this.tokeniserDecisionFactory.createDefaultDecision(TokeniserOutcome.SEPARATE);
					decision.addAuthority("_" + this.getClass().getSimpleName());
					Set<TokenPatternMatchSequence> matchSequences = tokenMatchSequenceMap.get(token);
					if (matchSequences!=null) {
						decision.addAuthority("_Patterns");
						for (TokenPatternMatchSequence matchSequence : matchSequences) {
							decision.addAuthority(matchSequence.getTokenPattern().getName());
						}
					}					
					guess.addTaggedToken(token, decision);
					mismatches++;
					LOG.debug("Mismatch: '" + token.getText() + "', '" + guessedAtomicSequence.get(i).getToken().getText() + "'");
					if (mismatches>6) {
						LOG.info("Real sequence: " + realSequence.getText());
						LOG.info("Guessed sequence: " + guessedSequence.getText());
						throw new TalismaneException("Too many mismatches for sentence: " + realSequence.getText());
					}
					continue;
				}
				TokeniserOutcome outcome = TokeniserOutcome.JOIN;
				
				if (guessedSequence.getTokenSplits().contains(guessedAtomicSequence.get(i).getToken().getStartIndex())) {
					outcome = TokeniserOutcome.SEPARATE;
				}
				Decision<TokeniserOutcome> decision = this.tokeniserDecisionFactory.createDefaultDecision(outcome);
				decision.addAuthority("_" + this.getClass().getSimpleName());
				
				Set<TokenPatternMatchSequence> matchSequences = tokenMatchSequenceMap.get(token);
				if (matchSequences!=null) {
					decision.addAuthority("_Patterns");
					for (TokenPatternMatchSequence matchSequence : matchSequences) {
						decision.addAuthority(matchSequence.getTokenPattern().getName());
					}
				}
				guess.addTaggedToken(token, decision);
				i++;
			}

			List<TokenisedAtomicTokenSequence> guessedAtomicSequences = new ArrayList<TokenisedAtomicTokenSequence>();
			guessedAtomicSequences.add(guess);
			
			for (TokenEvaluationObserver observer : observers) {
				observer.onNextTokenSequence(realSequence, guessedAtomicSequences);
			}
			sentenceIndex++;
			if (sentenceCount>0 && sentenceIndex==sentenceCount)
				break;
		} // next sentence
		
		for (TokenEvaluationObserver observer : observers) {
			observer.onEvaluationComplete();
		}
	}

	@Override
	public void addObserver(TokenEvaluationObserver observer) {
		this.observers.add(observer);
	}

	public int getSentenceCount() {
		return sentenceCount;
	}

	public void setSentenceCount(int sentenceCount) {
		this.sentenceCount = sentenceCount;
	}

	public TokeniserServiceInternal getTokeniserServiceInternal() {
		return tokeniserServiceInternal;
	}

	public void setTokeniserServiceInternal(
			TokeniserServiceInternal tokeniserServiceInternal) {
		this.tokeniserServiceInternal = tokeniserServiceInternal;
	}
	

}
