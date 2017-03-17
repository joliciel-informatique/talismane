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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.joliciel.talismane.Talismane.Module;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.typesafe.config.Config;

/**
 * An interface that observes a parsing evaluation while its occurring.
 * 
 * @author Assaf Urieli
 *
 */
public interface ParseEvaluationObserver {

	/**
	 * Called before parsing begins
	 */
	public void onParseStart(ParseConfiguration realConfiguration, List<PosTagSequence> posTagSequences);

	/**
	 * Called when the next parse configuration has been processed.
	 * 
	 * @throws TalismaneException
	 */
	public void onParseEnd(ParseConfiguration realConfiguration, List<ParseConfiguration> guessedConfigurations) throws TalismaneException;

	/**
	 * Called when full evaluation has completed.
	 */
	public void onEvaluationComplete();

	public static List<ParseEvaluationObserver> getObservers(File outDir, TalismaneSession session) throws IOException {
		Config config = session.getConfig();
		Config parserConfig = config.getConfig("talismane.core.parser");
		Config evalConfig = parserConfig.getConfig("evaluate");

		List<ParseEvaluationObserver> observers = new ArrayList<>();

		ParseTimeByLengthObserver parseTimeByLengthObserver = new ParseTimeByLengthObserver();
		if (evalConfig.getBoolean("include-time-per-token")) {
			File timePerTokenFile = new File(outDir, session.getBaseName() + ".timePerToken.csv");
			Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(timePerTokenFile, false), session.getCsvCharset()));
			parseTimeByLengthObserver.setWriter(csvFileWriter);
		}
		observers.add(parseTimeByLengthObserver);

		boolean labeledEvaluation = evalConfig.getBoolean("labeled-evaluation");

		File fscoreFile = new File(outDir, session.getBaseName() + ".fscores.csv");

		ParseEvaluationFScoreCalculator parseFScoreCalculator = new ParseEvaluationFScoreCalculator(fscoreFile);
		parseFScoreCalculator.setLabeledEvaluation(labeledEvaluation);

		Module startModule = Module.valueOf(evalConfig.getString("start-module"));
		if (startModule == Module.tokeniser)
			parseFScoreCalculator.setHasTokeniser(true);
		if (startModule == Module.tokeniser || startModule == Module.posTagger)
			parseFScoreCalculator.setHasPosTagger(true);
		observers.add(parseFScoreCalculator);

		if (evalConfig.getBoolean("output-guesses")) {
			File csvFile = new File(outDir, session.getBaseName() + "_sentences.csv");
			csvFile.delete();
			csvFile.createNewFile();
			Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), session.getCsvCharset()));
			int guessCount = 1;
			int outputGuessCount = evalConfig.getInt("output-guess-count");
			if (outputGuessCount > 0)
				guessCount = outputGuessCount;
			else
				guessCount = parserConfig.getInt("beam-width");

			ParseEvaluationSentenceWriter sentenceWriter = new ParseEvaluationSentenceWriter(csvFileWriter, guessCount);
			if (startModule == Module.tokeniser)
				sentenceWriter.setHasTokeniser(true);
			if (startModule == Module.tokeniser || startModule == Module.posTagger)
				sentenceWriter.setHasPosTagger(true);
			observers.add(sentenceWriter);
		}

		if (evalConfig.getBoolean("include-distance-fscores")) {
			File csvFile = new File(outDir, session.getBaseName() + "_distances.csv");
			csvFile.delete();
			csvFile.createNewFile();
			Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), session.getCsvCharset()));
			ParserFScoreCalculatorByDistance calculator = new ParserFScoreCalculatorByDistance(csvFileWriter);
			calculator.setLabeledEvaluation(labeledEvaluation);
			if (evalConfig.hasPath("skip-label")) {
				String skipLabel = evalConfig.getString("skip-label");
				calculator.setSkipLabel(skipLabel);
			}
			observers.add(calculator);
		}

		if (evalConfig.getBoolean("include-transition-log")) {
			File csvFile = new File(outDir, session.getBaseName() + "_transitions.csv");
			csvFile.delete();
			csvFile.createNewFile();
			Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), session.getCsvCharset()));
			ParseConfigurationProcessor transitionLogWriter = new TransitionLogWriter(csvFileWriter);
			ParseEvaluationObserverImpl observer = new ParseEvaluationObserverImpl(transitionLogWriter);
			observer.setWriter(csvFileWriter);
			List<String> errorLabels = evalConfig.getStringList("error-labels");
			observer.setErrorLabels(new HashSet<>(errorLabels));
			observers.add(observer);
		}

		List<ParseConfigurationProcessor> processors = ParseConfigurationProcessor.getProcessors(null, outDir, session);
		for (ParseConfigurationProcessor processor : processors) {
			ParseEvaluationGuessTemplateWriter templateWriter = new ParseEvaluationGuessTemplateWriter(processor);
			observers.add(templateWriter);
		}

		return observers;
	}
}
