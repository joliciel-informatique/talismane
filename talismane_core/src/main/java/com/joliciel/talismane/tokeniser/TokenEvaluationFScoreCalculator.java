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
package com.joliciel.talismane.tokeniser;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.stats.FScoreCalculator;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.StringUtils;

/**
 * Calculates the f-score of a tokeniser evaluation.
 * 
 * @author Assaf Urieli
 *
 */
public class TokenEvaluationFScoreCalculator implements TokenEvaluationObserver {
	private static final Logger LOG = LoggerFactory.getLogger(TokenEvaluationFScoreCalculator.class);
	private static final CSVFormatter CSV = new CSVFormatter(4);
	private static final int NUM_CHARS = 20;
	private FScoreCalculator<TokeniserOutcome> fScoreCalculator = new FScoreCalculator<TokeniserOutcome>();

	private Map<String, FScoreCalculator<TokeniserOutcome>> taggerFScoreCalculators = new TreeMap<String, FScoreCalculator<TokeniserOutcome>>();
	private Map<String, List<TokeniserErrorRecord>> errorMap = new TreeMap<String, List<TokeniserErrorRecord>>();
	private Writer errorWriter;
	private Writer csvErrorWriter;

	private File fScoreFile;

	@Override
	public void onNextTokenSequence(TokenSequence realSequence, List<TokenisedAtomicTokenSequence> guessedAtomicSequences) {

		List<Integer> realSplits = realSequence.getTokenSplits();
		String sentence = realSequence.getSentence().getText().toString();

		TokenisedAtomicTokenSequence tokeniserAtomicTokenSequence = guessedAtomicSequences.get(0);
		TokenSequence guessedSequence = tokeniserAtomicTokenSequence.inferTokenSequence();
		List<Integer> guessedSplits = guessedSequence.getTokenSplits();

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

		for (TaggedToken<TokeniserOutcome> guessTag : tokeniserAtomicTokenSequence) {
			TokeniserOutcome guessDecision = guessTag.getTag();
			boolean realSplit = realSplits.contains(guessTag.getToken().getStartIndex());

			TokeniserOutcome realDecision = realSplit ? TokeniserOutcome.SEPARATE : TokeniserOutcome.JOIN;

			if (!realDecision.equals(guessDecision)) {
				int start1 = guessTag.getToken().getStartIndex() - NUM_CHARS;
				int end1 = guessTag.getToken().getStartIndex() + NUM_CHARS;

				if (start1 < 0)
					start1 = 0;
				String startString = sentence.substring(start1, guessTag.getToken().getStartIndex());
				startString = StringUtils.padLeft(startString, NUM_CHARS);

				if (end1 >= sentence.length())
					end1 = sentence.length() - 1;

				String symbol = "+";
				if (realDecision == TokeniserOutcome.SEPARATE)
					symbol = "-";

				TokeniserErrorRecord errorRecord = new TokeniserErrorRecord();
				errorRecord.realDecision = realDecision;
				errorRecord.guessDecision = guessDecision;
				errorRecord.context = startString + "[" + symbol + "]" + sentence.substring(guessTag.getToken().getStartIndex(), end1);
				LOG.debug("guess " + guessDecision + ", real " + realDecision + ", context: " + errorRecord.context);
				for (String authority : guessTag.getDecision().getAuthorities()) {
					List<TokeniserErrorRecord> errors = errorMap.get(authority);
					if (errors == null) {
						errors = new ArrayList<TokeniserErrorRecord>();
						errorMap.put(authority, errors);
					}
					errors.add(errorRecord);
				}
			}

			fScoreCalculator.increment(realDecision, guessDecision);
			for (String authority : guessTag.getDecision().getAuthorities()) {
				FScoreCalculator<TokeniserOutcome> taggerFScoreCalculator = taggerFScoreCalculators.get(authority);
				if (taggerFScoreCalculator == null) {
					taggerFScoreCalculator = new FScoreCalculator<TokeniserOutcome>();
					taggerFScoreCalculators.put(authority, taggerFScoreCalculator);
				}
				taggerFScoreCalculator.increment(realDecision, guessDecision);
			}
		} // next decision
	}

	@Override
	public void onEvaluationComplete() throws IOException {
		for (String tagger : taggerFScoreCalculators.keySet()) {
			LOG.debug("###### Tagger " + tagger);
			FScoreCalculator<TokeniserOutcome> taggerFScoreCalculator = taggerFScoreCalculators.get(tagger);
			LOG.debug("###### Tagger " + tagger + ": f-score = " + taggerFScoreCalculator.getTotalFScore());
		}

		if (fScoreFile != null) {
			fScoreCalculator.writeScoresToCSVFile(fScoreFile);
		}

		if (errorWriter != null) {
			try {
				for (String tagger : taggerFScoreCalculators.keySet()) {
					FScoreCalculator<TokeniserOutcome> taggerFScoreCalculator = taggerFScoreCalculators.get(tagger);
					errorWriter.write("###### Tagger " + tagger + ": f-score = " + taggerFScoreCalculator.getTotalFScore() + "\n");
					errorWriter.write(
							"Total " + (taggerFScoreCalculator.getTotalTruePositiveCount() + taggerFScoreCalculator.getTotalFalseNegativeCount()) + "\n");
					errorWriter.write("True + " + taggerFScoreCalculator.getTotalTruePositiveCount() + "\n");
					errorWriter.write("False- " + taggerFScoreCalculator.getTotalFalseNegativeCount() + "\n");
					errorWriter.write("False+ " + taggerFScoreCalculator.getTotalFalsePositiveCount() + "\n");
					for (TokeniserOutcome outcome : taggerFScoreCalculator.getOutcomeSet()) {
						errorWriter.write(outcome + " total  "
								+ (taggerFScoreCalculator.getTruePositiveCount(outcome) + taggerFScoreCalculator.getFalseNegativeCount(outcome)) + "\n");
						errorWriter.write(outcome + " true + " + (taggerFScoreCalculator.getTruePositiveCount(outcome)) + "\n");
						errorWriter.write(outcome + " false- " + (taggerFScoreCalculator.getFalseNegativeCount(outcome)) + "\n");
						errorWriter.write(outcome + " false+ " + (taggerFScoreCalculator.getFalsePositiveCount(outcome)) + "\n");
						errorWriter.write(outcome + " precis " + (taggerFScoreCalculator.getPrecision(outcome)) + "\n");
						errorWriter.write(outcome + " recall " + (taggerFScoreCalculator.getRecall(outcome)) + "\n");
						errorWriter.write(outcome + " fscore " + (taggerFScoreCalculator.getFScore(outcome)) + "\n");
					}

					List<TokeniserErrorRecord> errors = errorMap.get(tagger);
					if (errors != null) {
						for (TokeniserErrorRecord errorRecord : errors) {
							errorWriter.write(
									"guess " + errorRecord.guessDecision + ", real " + errorRecord.realDecision + ", context: " + errorRecord.context + "\n");
						}
					}
					errorWriter.flush();
				}
			} finally {
				errorWriter.close();
			}
		}

		if (csvErrorWriter != null) {
			try {
				for (String tagger : taggerFScoreCalculators.keySet()) {
					FScoreCalculator<TokeniserOutcome> taggerFScoreCalculator = taggerFScoreCalculators.get(tagger);
					csvErrorWriter.write(CSV.format("Authority") + CSV.format("total") + CSV.format("true+") + CSV.format("false-") + CSV.format("false+")
							+ CSV.format("precis") + CSV.format("recall") + CSV.format("fscore") + "\n");
					csvErrorWriter.write(CSV.format(tagger)
							+ CSV.format((taggerFScoreCalculator.getTotalTruePositiveCount() + taggerFScoreCalculator.getTotalFalseNegativeCount()))
							+ CSV.format(taggerFScoreCalculator.getTotalTruePositiveCount()) + CSV.format(taggerFScoreCalculator.getTotalFalseNegativeCount())
							+ CSV.format(taggerFScoreCalculator.getTotalFalsePositiveCount()) + CSV.format(taggerFScoreCalculator.getTotalPrecision())
							+ CSV.format(taggerFScoreCalculator.getTotalRecall()) + CSV.format(taggerFScoreCalculator.getTotalFScore()) + "\n");

					for (TokeniserOutcome outcome : taggerFScoreCalculator.getOutcomeSet()) {
						csvErrorWriter.write(CSV.format(outcome.name())
								+ CSV.format((taggerFScoreCalculator.getTruePositiveCount(outcome) + taggerFScoreCalculator.getFalseNegativeCount(outcome)))
								+ CSV.format(taggerFScoreCalculator.getTruePositiveCount(outcome))
								+ CSV.format(taggerFScoreCalculator.getFalseNegativeCount(outcome))
								+ CSV.format(taggerFScoreCalculator.getFalsePositiveCount(outcome)) + CSV.format(taggerFScoreCalculator.getPrecision(outcome))
								+ CSV.format(taggerFScoreCalculator.getRecall(outcome)) + CSV.format(taggerFScoreCalculator.getFScore(outcome)) + "\n");
					}

					List<TokeniserErrorRecord> errors = errorMap.get(tagger);
					if (errors != null) {
						for (TokeniserErrorRecord errorRecord : errors) {
							csvErrorWriter.write(CSV.format(errorRecord.guessDecision.name()));
							csvErrorWriter.write(CSV.format(errorRecord.realDecision.name()));
							csvErrorWriter.write(CSV.format(errorRecord.context));
							csvErrorWriter.write("\n");
						}
					}
					csvErrorWriter.flush();
				}
			} finally {
				errorWriter.close();
			}
		}
	}

	public Writer getErrorWriter() {
		return errorWriter;
	}

	public void setErrorWriter(Writer errorWriter) {
		this.errorWriter = errorWriter;
	}

	public Writer getCsvErrorWriter() {
		return csvErrorWriter;
	}

	public void setCsvErrorWriter(Writer csvErrorWriter) {
		this.csvErrorWriter = csvErrorWriter;
	}

	public File getFScoreFile() {
		return fScoreFile;
	}

	public void setFScoreFile(File fScoreFile) {
		this.fScoreFile = fScoreFile;
	}

	public FScoreCalculator<TokeniserOutcome> getFScoreCalculator() {
		return fScoreCalculator;
	}

	public Map<String, FScoreCalculator<TokeniserOutcome>> getTaggerFScoreCalculators() {
		return taggerFScoreCalculators;
	}

	private static final class TokeniserErrorRecord {
		TokeniserOutcome guessDecision;
		TokeniserOutcome realDecision;
		String context;
	}
}
