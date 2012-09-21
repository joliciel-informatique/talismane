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
package com.joliciel.talismane.posTagger;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.stats.FScoreCalculator;
import com.joliciel.talismane.tokeniser.TaggedToken;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.utils.CSVFormatter;

class PosTaggerEvaluatorImpl implements PosTaggerEvaluator {
	private static final Log LOG = LogFactory.getLog(PosTaggerEvaluatorImpl.class);

	private Writer csvFileWriter;
	private PosTagger posTagger;
	private Tokeniser tokeniser;
	private boolean propagateBeam = false;
	private FScoreCalculator<String> fscoreUnknownInCorpus;
	private FScoreCalculator<String> fscoreUnknownInLexicon;
	
	private Set<String> unknownWords;
	
	/**
	 */
	public PosTaggerEvaluatorImpl(PosTagger posTagger, Writer csvFileWriter) {
		this.posTagger = posTagger;
		this.csvFileWriter = csvFileWriter;
	}

	@Override
	public FScoreCalculator<String> evaluate(PosTagAnnotatedCorpusReader corpusReader) {
		FScoreCalculator<String> fScoreCalculator = new FScoreCalculator<String>();
		if (unknownWords!=null)
			fscoreUnknownInCorpus = new FScoreCalculator<String>();
		fscoreUnknownInLexicon = new FScoreCalculator<String>();
		
		while (corpusReader.hasNextSentence()) {
			PosTagSequence realPosTagSequence = corpusReader.nextSentence();
			
			List<TokenSequence> tokenSequences = null;
			List<PosTagSequence> posTagSequences = null;
			
			TokenSequence tokenSequence = realPosTagSequence.getTokenSequence();
			PosTagSequence guessedSequence = null;
			
			if (this.tokeniser!=null) {
				tokenSequences = tokeniser.tokenise(tokenSequence.getSentence());
				tokenSequence = tokenSequences.get(0);
				if (!propagateBeam) {
					tokenSequences = new ArrayList<TokenSequence>();
					tokenSequences.add(tokenSequence);
				}
			} else {
				tokenSequences = new ArrayList<TokenSequence>();
				tokenSequences.add(tokenSequence);
			}
			
			if (posTagger instanceof NonDeterministicPosTagger) {
				NonDeterministicPosTagger nonDeterministicPosTagger = (NonDeterministicPosTagger) posTagger;
				posTagSequences = nonDeterministicPosTagger.tagSentence(tokenSequences);
				guessedSequence = posTagSequences.get(0);
			} else {
				guessedSequence = posTagger.tagSentence(tokenSequence);
			}
			
			if (LOG.isDebugEnabled()) {
				StringBuilder stringBuilder = new StringBuilder();
				for (PosTaggedToken posTaggedToken : guessedSequence) {
					Set<String> lemmas = new TreeSet<String>();
					stringBuilder.append(posTaggedToken.getToken().getOriginalText());
					stringBuilder.append("[" + posTaggedToken.getTag());
					
					Set<LexicalEntry> entries = posTaggedToken.getLexicalEntries();
					boolean dropCurrentWord = false;
					if (entries.size()>1) 
						dropCurrentWord = true;
					for (LexicalEntry entry : posTaggedToken.getLexicalEntries()) {
						if (!lemmas.contains(entry.getLemma())) {
							if (dropCurrentWord&&posTaggedToken.getToken().getText().equals(entry.getLemma())) {
								dropCurrentWord = false;
								continue;
							}
							stringBuilder.append("|" + entry.getLemma());
//							stringBuilder.append("/" + entry.getCategory());
							stringBuilder.append("/" + entry.getMorphology());
							lemmas.add(entry.getLemma());
						}
					}
					stringBuilder.append("] ");
				}
				LOG.debug(stringBuilder.toString());
			}
			
			int j = 0;
			for (int i = 0; i<realPosTagSequence.size(); i++) {
				TaggedToken<PosTag> realToken = realPosTagSequence.get(i);
				TaggedToken<PosTag> testToken = guessedSequence.get(j);
				boolean tokenError = false;
				if (realToken.getToken().getStartIndex()==testToken.getToken().getStartIndex()
						&& realToken.getToken().getEndIndex()==testToken.getToken().getEndIndex()) {
					// no token error
					j++;
					if (j==guessedSequence.size()) {
						j--;
					}
				} else {
					tokenError = true;
					while (realToken.getToken().getEndIndex()>=testToken.getToken().getEndIndex()) {
						j++;
						if (j==guessedSequence.size()) {
							j--;
							break;
						}
						testToken = guessedSequence.get(j);
					}
				}

				if (LOG.isTraceEnabled()) {
					LOG.trace("Token " + testToken.getToken().getText() + ", guessed: " + testToken.getTag().getCode() + " (" + testToken.getDecision().getProbability() + "), actual: " + realToken.getTag().getCode());
				}
				
				String result = testToken.getTag().getCode();
				if (tokenError)
					result = "TOKEN_ERROR";
				
				fScoreCalculator.increment(realToken.getTag().getCode(), result);
				if (testToken.getToken().getPossiblePosTags()!=null&&testToken.getToken().getPossiblePosTags().size()==0)
					fscoreUnknownInLexicon.increment(realToken.getTag().getCode(), result);
				if (unknownWords!=null&&unknownWords.contains(testToken.getToken().getText()))
					fscoreUnknownInCorpus.increment(realToken.getTag().getCode(), result);
			}

			if (this.csvFileWriter!=null) {
				try {
					for (int i = 0; i<realPosTagSequence.size(); i++) {
						String token =  realPosTagSequence.get(i).getToken().getText();
						csvFileWriter.write(CSVFormatter.format(token) + ",");
					}
					csvFileWriter.write("\n");
					for (int i = 0; i<realPosTagSequence.size(); i++)
						csvFileWriter.write(realPosTagSequence.get(i).getTag().getCode() + ",");
					csvFileWriter.write("\n");
					
					int beamWidth = 1;
					if (posTagger instanceof NonDeterministicPosTagger) {
						beamWidth = ((NonDeterministicPosTagger) posTagger).getBeamWidth();
					}
					for (int k = 0; k<beamWidth; k++) {
						PosTagSequence posTagSequence = null;
						if (k<posTagSequences.size()) {
							posTagSequence = posTagSequences.get(k);
						} else {
							csvFileWriter.write("\n");
							csvFileWriter.write("\n");
							continue;
						}
						j = 0;
						String probs = "";
						for (int i = 0; i<realPosTagSequence.size(); i++) {
							TaggedToken<PosTag> realToken = realPosTagSequence.get(i);
							TaggedToken<PosTag> testToken = posTagSequence.get(j);
							boolean tokenError = false;
							if (realToken.getToken().getStartIndex()==testToken.getToken().getStartIndex()
									&& realToken.getToken().getEndIndex()==testToken.getToken().getEndIndex()) {
								// no token error
								j++;
								if (j==posTagSequence.size()) {
									j--;
								}
							} else {
								tokenError = true;
								while (realToken.getToken().getEndIndex()>=testToken.getToken().getEndIndex()) {
									j++;
									if (j==posTagSequence.size()) {
										j--;
										break;
									}
									testToken = posTagSequence.get(j);
								}
							}
							if (tokenError) {
								csvFileWriter.write("BAD_TOKEN,");
							} else {
								csvFileWriter.write(CSVFormatter.format(testToken.getTag().getCode()) + ",");
							}
							probs += CSVFormatter.format(testToken.getDecision().getProbability()) + ",";
						}
						csvFileWriter.write("\n");
						csvFileWriter.write(probs + "\n");
					}
					csvFileWriter.flush();
				} catch (IOException ioe) {
					throw new RuntimeException(ioe);
				}
			}
		}
		fScoreCalculator.getTotalFScore();

		return fScoreCalculator;
	}

	public PosTagger getPosTagger() {
		return posTagger;
	}

	public void setPosTagger(PosTagger posTagger) {
		this.posTagger = posTagger;
	}

	@Override
	public FScoreCalculator<String> getFscoreUnknownInCorpus() {
		return fscoreUnknownInCorpus;
	}

	@Override
	public FScoreCalculator<String> getFscoreUnknownInLexicon() {
		return fscoreUnknownInLexicon;
	}

	@Override
	public Set<String> getUnknownWords() {
		return unknownWords;
	}

	@Override
	public void setUnknownWords(Set<String> unknownWords) {
		this.unknownWords = unknownWords;
	}

	@Override
	public Tokeniser getTokeniser() {
		return tokeniser;
	}

	@Override
	public void setTokeniser(Tokeniser tokeniser) {
		this.tokeniser = tokeniser;
	}
	
	@Override
	public boolean isPropagateBeam() {
		return propagateBeam;
	}

	@Override
	public void setPropagateBeam(boolean propagateBeam) {
		this.propagateBeam = propagateBeam;
	}

	public Writer getCsvFileWriter() {
		return csvFileWriter;
	}

	public void setCsvFileWriter(Writer csvFileWriter) {
		this.csvFileWriter = csvFileWriter;
	}
	
}
