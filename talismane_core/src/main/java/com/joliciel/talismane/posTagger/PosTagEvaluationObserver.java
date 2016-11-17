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
package com.joliciel.talismane.posTagger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.TalismaneSession;
import com.typesafe.config.Config;

/**
 * An interface that observes a pos-tagger evaluation while its occurring.
 * 
 * @author Assaf Urieli
 *
 */
public interface PosTagEvaluationObserver {
	/**
	 * Called when the next pos-tag sequence has been processed.
	 */
	public void onNextPosTagSequence(PosTagSequence realSequence, List<PosTagSequence> guessedSequences);

	public void onEvaluationComplete();

	public static List<PosTagEvaluationObserver> getObservers(TalismaneSession session) throws IOException {
		Config config = session.getConfig();
		Config posTaggerConfig = config.getConfig("talismane.core.pos-tagger");
		Config evalConfig = posTaggerConfig.getConfig("evaluate");

		List<PosTagEvaluationObserver> observers = new ArrayList<>();

		boolean outputGuesses = evalConfig.getBoolean("output-guesses");
		if (outputGuesses) {
			File csvFile = new File(session.getOutDir(), session.getBaseName() + "_sentences.csv");
			csvFile.delete();
			csvFile.createNewFile();
			Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), session.getCsvCharset()));

			int guessCount = 1;
			int outputGuessCount = evalConfig.getInt("output-guess-count");
			if (outputGuessCount > 0)
				guessCount = outputGuessCount;
			else
				guessCount = posTaggerConfig.getInt("beam-width");

			PosTagEvaluationSentenceWriter sentenceWriter = new PosTagEvaluationSentenceWriter(csvFileWriter, guessCount);
			observers.add(sentenceWriter);
		}

		File fscoreFile = new File(session.getOutDir(), session.getBaseName() + "_fscores.csv");

		PosTagEvaluationFScoreCalculator posTagFScoreCalculator = new PosTagEvaluationFScoreCalculator(fscoreFile);
		if (evalConfig.getBoolean("include-unknown-word-results")) {
			File fscoreUnknownWordFile = new File(session.getOutDir(), session.getBaseName() + "_unknown.csv");
			posTagFScoreCalculator.setFScoreUnknownInLexiconFile(fscoreUnknownWordFile);
			File fscoreKnownWordFile = new File(session.getOutDir(), session.getBaseName() + "_known.csv");
			posTagFScoreCalculator.setFScoreKnownInLexiconFile(fscoreKnownWordFile);
		}

		observers.add(posTagFScoreCalculator);

		PosTagSequenceProcessor processor = PosTagSequenceProcessor.getProcessor(session);

		File freemarkerFile = new File(session.getOutDir(), session.getBaseName() + "_output.txt");
		freemarkerFile.delete();
		freemarkerFile.createNewFile();
		Writer freemakerFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(freemarkerFile, false), "UTF8"));
		PosTaggerGuessTemplateWriter templateWriter = new PosTaggerGuessTemplateWriter(processor, freemakerFileWriter);
		observers.add(templateWriter);

		if (evalConfig.getBoolean("include-lexicon-coverage")) {
			File lexiconCoverageFile = new File(session.getOutDir(), session.getBaseName() + ".lexiconCoverage.csv");
			PosTagEvaluationLexicalCoverageTester lexiconCoverageTester = new PosTagEvaluationLexicalCoverageTester(lexiconCoverageFile);
			observers.add(lexiconCoverageTester);
		}

		return observers;
	}

}
