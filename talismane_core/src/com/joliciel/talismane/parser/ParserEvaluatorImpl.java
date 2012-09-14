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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.utils.stats.FScoreCalculator;
import com.joliciel.talismane.utils.util.CSVFormatter;
import com.joliciel.talismane.utils.util.LogUtils;

public class ParserEvaluatorImpl implements ParserEvaluator {
	private static final Log LOG = LogFactory.getLog(ParserEvaluatorImpl.class);
	private NonDeterministicParser parser;
	private ParserServiceInternal parserServiceInternal;
	private boolean labeledEvaluation;
	private Writer csvFileWriter;
	
	@Override
	public FScoreCalculator<String> evaluate(
			ParseAnnotatedCorpusReader corpusReader) {
		FScoreCalculator<String> fscoreCalculator = new FScoreCalculator<String>();
		
		while (corpusReader.hasNextSentence()) {
			ParseConfiguration realConfiguration = corpusReader.nextSentence();
			PosTagSequence posTagSequence = realConfiguration.getPosTagSequence();
			List<PosTagSequence> posTagSequences = new ArrayList<PosTagSequence>();
			posTagSequences.add(posTagSequence);
			
			List<ParseConfiguration> guessedConfigurations = parser.parseSentence(posTagSequences);
			ParseConfiguration bestGuess = guessedConfigurations.get(0);
			
			for (PosTaggedToken posTaggedToken : posTagSequence) {
				if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
					DependencyArc realArc = realConfiguration.getGoverningDependency(posTaggedToken);
					DependencyArc guessedArc = bestGuess.getGoverningDependency(posTaggedToken);
					
					String realLabel = realArc==null ? "noHead" : labeledEvaluation ? realArc.getLabel() : "head";
					String guessedLabel = guessedArc==null ? "noHead" : labeledEvaluation ? guessedArc.getLabel() : "head";
					
					if (realLabel==null||realLabel.length()==0) realLabel = "noLabel";
					if (guessedLabel==null||guessedLabel.length()==0) guessedLabel = "noLabel";
					
					if (realArc==null || guessedArc==null) {
						fscoreCalculator.increment(realLabel, guessedLabel);
					} else {
						if (realArc.getHead().equals(guessedArc.getHead())) {
							fscoreCalculator.increment(realLabel, guessedLabel);
						} else if (realArc.getLabel().equals(guessedArc.getLabel())) {
							fscoreCalculator.increment(realLabel, "wrongHead");
						} else {
							fscoreCalculator.increment(realLabel, "wrongHeadWrongLabel");
						}
					}
				}
			}
			
			if (csvFileWriter!=null) {
				try {
					for (PosTaggedToken posTaggedToken : posTagSequence) {
						if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
							csvFileWriter.write(CSVFormatter.format(posTaggedToken.getToken().getOriginalText()) + ",");
						}					
					}
					csvFileWriter.write("\n");
					for (PosTaggedToken posTaggedToken : posTagSequence) {
						if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
							csvFileWriter.write(CSVFormatter.format(posTaggedToken.getTag().getCode()) + ",");
						}					
					}
					csvFileWriter.write("\n");
					for (PosTaggedToken posTaggedToken : posTagSequence) {
						if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
							DependencyArc realArc = realConfiguration.getGoverningDependency(posTaggedToken);
							String realLabel = realArc.getLabel()==null ? "null" : realArc.getLabel();
							csvFileWriter.write(CSVFormatter.format(realLabel) + ",");
						}					
					}
					csvFileWriter.write("\n");
					for (PosTaggedToken posTaggedToken : posTagSequence) {
						if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
							DependencyArc realArc = realConfiguration.getGoverningDependency(posTaggedToken);
							int index = -1;
							if (realArc!=null) {
								index = realArc.getHead().getToken().getIndex() - 1;
							}
							if (index<0)
								csvFileWriter.write("root,");
							else
								csvFileWriter.write(CSVFormatter.getColumnLabel(index) + ",");
						}					
					}
					csvFileWriter.write("\n");
					
					for (int i = 0; i < parser.getBeamWidth(); i++) {
						if (i<guessedConfigurations.size()) {
							ParseConfiguration guessedConfiguration = guessedConfigurations.get(i);
							for (PosTaggedToken posTaggedToken : posTagSequence) {
								if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
									DependencyArc guessedArc = guessedConfiguration.getGoverningDependency(posTaggedToken);
									String guessedLabel = "";
									if (guessedArc!=null) {
										guessedLabel = guessedArc.getLabel()==null ? "null" : guessedArc.getLabel();
									}
									csvFileWriter.write(CSVFormatter.format(guessedLabel) + ",");
								}					
							}
							csvFileWriter.write("\n");
							for (PosTaggedToken posTaggedToken : posTagSequence) {
								if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
									DependencyArc guessedArc = guessedConfiguration.getGoverningDependency(posTaggedToken);
									int index = -1;
									if (guessedArc!=null) {
										index = guessedArc.getHead().getToken().getIndex() - 1;
									}
									if (index<0)
										csvFileWriter.write("root,");
									else
										csvFileWriter.write(CSVFormatter.getColumnLabel(index) + ",");
								}					
							}
							csvFileWriter.write("\n");
							for (PosTaggedToken posTaggedToken : posTagSequence) {
								if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
									DependencyArc guessedArc = guessedConfiguration.getGoverningDependency(posTaggedToken);
									double prob = 1.0;
									if (guessedArc!=null) {
										Transition transition = guessedConfiguration.getTransition(guessedArc);
										if (transition!=null)
											prob = transition.getProbability();
									}
									csvFileWriter.write(CSVFormatter.format(prob) + ",");
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
			} // have csvFileWriter
		} // next sentence
		return fscoreCalculator;
	}

	public NonDeterministicParser getParser() {
		return parser;
	}

	public void setParser(NonDeterministicParser parser) {
		this.parser = parser;
	}

	public ParserServiceInternal getParserServiceInternal() {
		return parserServiceInternal;
	}

	public void setParserServiceInternal(ParserServiceInternal parserServiceInternal) {
		this.parserServiceInternal = parserServiceInternal;
	}

	@Override
	public boolean isLabeledEvaluation() {
		return labeledEvaluation;
	}

	@Override
	public void setLabeledEvaluation(boolean labeledEvaluation) {
		this.labeledEvaluation = labeledEvaluation;
	}

	public Writer getCsvFileWriter() {
		return csvFileWriter;
	}

	public void setCsvFileWriter(Writer csvFileWriter) {
		this.csvFileWriter = csvFileWriter;
	}
	
	
}
