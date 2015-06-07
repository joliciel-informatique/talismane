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

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.LogUtils;

/**
 * Writes each sentence to a CSV file as it gets parsed, along with the guesses made and their probability.
 * @author Assaf Urieli
 *
 */
public class ParseEvaluationSentenceWriter implements ParseEvaluationObserver {
	private static final Log LOG = LogFactory.getLog(ParseEvaluationSentenceWriter.class);
	private static final CSVFormatter CSV = new CSVFormatter();
	private Writer csvFileWriter;
	private int guessCount;
	private boolean hasTokeniser = false;
	private boolean hasPosTagger = false;
	
	public ParseEvaluationSentenceWriter(Writer csvFileWriter, int guessCount) {
		super();
		this.csvFileWriter = csvFileWriter;
		this.guessCount = guessCount;
	}


	@Override
	public void onParseEnd(ParseConfiguration realConfiguration,
			List<ParseConfiguration> guessedConfigurations) {
		try {
			TreeSet<Integer> startIndexes = new TreeSet<Integer>();
			for (PosTaggedToken posTaggedToken : realConfiguration.getPosTagSequence()) {
				if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
					Token token = posTaggedToken.getToken();
					startIndexes.add(token.getStartIndex());
				}
			}
			if (hasTokeniser || hasPosTagger) {
				int i = 0;
				for (ParseConfiguration guessedConfiguration : guessedConfigurations) {
					for (PosTaggedToken posTaggedToken : guessedConfiguration.getPosTagSequence()) {
						if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
							Token token = posTaggedToken.getToken();
							startIndexes.add(token.getStartIndex());							
						}
					}
					i++;
					if (i==guessCount)
						break;
				}
			}
			Map<Integer,Integer> startIndexMap = new HashMap<Integer, Integer>();
			int j=0;
			for (int startIndex : startIndexes) {
				startIndexMap.put(startIndex, j++);
			}
			
			PosTagSequence posTagSequence = realConfiguration.getPosTagSequence();
			PosTaggedToken[] realTokens = new PosTaggedToken[startIndexes.size()];
			for (PosTaggedToken posTaggedToken : posTagSequence) {
				if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
					realTokens[startIndexMap.get(posTaggedToken.getToken().getStartIndex())] = posTaggedToken;
				}
			}
			
			for (PosTaggedToken posTaggedToken : realTokens) {
				if (posTaggedToken != null) {
					csvFileWriter.write(CSV.format(posTaggedToken.getToken().getOriginalText()));
				} else {
					csvFileWriter.write(CSV.getCsvSeparator());
				}
			}
			
			csvFileWriter.write("\n");
			for (PosTaggedToken posTaggedToken : realTokens) {
				if (posTaggedToken != null) {
					csvFileWriter.write(CSV.format(posTaggedToken.getTag().getCode()));
				} else {
					csvFileWriter.write(CSV.getCsvSeparator());
				}
			}
			csvFileWriter.write("\n");
			for (PosTaggedToken posTaggedToken : realTokens) {
				if (posTaggedToken != null) {
					DependencyArc realArc = realConfiguration.getGoverningDependency(posTaggedToken);
					String realLabel = realArc.getLabel()==null ? "null" : realArc.getLabel();
					csvFileWriter.write(CSV.format(realLabel));
				} else {
					csvFileWriter.write(CSV.getCsvSeparator());
				}
			}
			csvFileWriter.write("\n");
			for (PosTaggedToken posTaggedToken : realTokens) {
				if (posTaggedToken != null) {
					DependencyArc realArc = realConfiguration.getGoverningDependency(posTaggedToken);
					int startIndex = -1;
					if (realArc!=null) {
						PosTaggedToken head = realArc.getHead();
						if (!head.getTag().equals(PosTag.ROOT_POS_TAG)) {
							startIndex = head.getToken().getStartIndex();
						}
					}
					if (startIndex<0)
						csvFileWriter.write(CSV.format("ROOT"));
					else
						csvFileWriter.write(CSV.getColumnLabel(startIndexMap.get(startIndex)) + CSV.getCsvSeparator());
				} else {
					csvFileWriter.write(CSV.getCsvSeparator());
				}
			}
			csvFileWriter.write("\n");
			
			for (int i = 0; i < guessCount; i++) {
				if (i<guessedConfigurations.size()) {
					ParseConfiguration guessedConfiguration = guessedConfigurations.get(i);
					PosTaggedToken[] guessedTokens = new PosTaggedToken[startIndexes.size()];
					for (PosTaggedToken posTaggedToken : guessedConfiguration.getPosTagSequence()) {
						if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
							guessedTokens[startIndexMap.get(posTaggedToken.getToken().getStartIndex())] = posTaggedToken;
						}
					}
					
					if (hasTokeniser) {
						for (PosTaggedToken posTaggedToken : guessedTokens) {
							if (posTaggedToken != null) {
								csvFileWriter.write(CSV.format(posTaggedToken.getToken().getOriginalText()));
							} else {
								csvFileWriter.write(CSV.getCsvSeparator());
							}
						}
						
						csvFileWriter.write("\n");
					}
					
					if (hasPosTagger) {
						for (PosTaggedToken posTaggedToken : guessedTokens) {
							if (posTaggedToken != null) {
								csvFileWriter.write(CSV.format(posTaggedToken.getTag().getCode()));
							} else {
								csvFileWriter.write(CSV.getCsvSeparator());
							}
						}
						csvFileWriter.write("\n");
					}
					
					for (PosTaggedToken posTaggedToken : guessedTokens) {
						if (posTaggedToken != null) {
							DependencyArc guessedArc = guessedConfiguration.getGoverningDependency(posTaggedToken);
							String guessedLabel = "";
							if (guessedArc!=null) {
								guessedLabel = guessedArc.getLabel()==null ? "null" : guessedArc.getLabel();
							}
							csvFileWriter.write(CSV.format(guessedLabel) );
						} else {
							csvFileWriter.write(CSV.getCsvSeparator());
						}					
					}
					csvFileWriter.write("\n");
					for (PosTaggedToken posTaggedToken : guessedTokens) {
						if (posTaggedToken != null) {
							DependencyArc guessedArc = guessedConfiguration.getGoverningDependency(posTaggedToken);
							int startIndex = -1;
							if (guessedArc!=null) {
								PosTaggedToken head = guessedArc.getHead();
								if (!head.getTag().equals(PosTag.ROOT_POS_TAG)) {
									startIndex = head.getToken().getStartIndex();
								}
							}
							if (startIndex<0)
								csvFileWriter.write(CSV.format("ROOT"));
							else
								csvFileWriter.write(CSV.getColumnLabel(startIndexMap.get(startIndex)) + CSV.getCsvSeparator());
						} else {
							csvFileWriter.write(CSV.getCsvSeparator());
						}
					}
					csvFileWriter.write("\n");
					for (PosTaggedToken posTaggedToken : guessedTokens) {
						if (posTaggedToken != null) {
							DependencyArc guessedArc = guessedConfiguration.getGoverningDependency(posTaggedToken);
							double prob = 1.0;
							if (guessedArc!=null) {
								Transition transition = guessedConfiguration.getTransition(guessedArc);
								if (transition!=null)
									prob = transition.getDecision().getProbability();
							}
							csvFileWriter.write(CSV.format(prob));
						} else {
							csvFileWriter.write(CSV.getCsvSeparator());
						}	
					}
					csvFileWriter.write("\n");
					
				} else {
					csvFileWriter.write("\n");
					csvFileWriter.write("\n");							
				} // have more configurations
			} // next guessed configuration
			csvFileWriter.flush();
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

	@Override
	public void onEvaluationComplete() {
		try {
			csvFileWriter.flush();
			csvFileWriter.close();
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}


	public boolean isHasTokeniser() {
		return hasTokeniser;
	}


	public void setHasTokeniser(boolean hasTokeniser) {
		this.hasTokeniser = hasTokeniser;
	}


	public boolean isHasPosTagger() {
		return hasPosTagger;
	}


	public void setHasPosTagger(boolean hasPosTagger) {
		this.hasPosTagger = hasPosTagger;
	}

	@Override
	public void onParseStart(ParseConfiguration realConfiguration,
			List<PosTagSequence> posTagSequences) {
	}
}
