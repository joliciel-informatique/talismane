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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.typesafe.config.Config;

/**
 * Compare two annotated corpora, one serving as reference and the other as an
 * evaluation. Note: it is assumed that the corpora have an exact matching set
 * of parse configurations!
 * 
 * @author Assaf Urieli
 *
 */
public class ParseComparator {
	private static final Logger LOG = LoggerFactory.getLogger(ParseComparator.class);

	private final ParserAnnotatedCorpusReader referenceCorpusReader;
	private final ParserAnnotatedCorpusReader evaluationCorpusReader;

	private final List<ParseEvaluationObserver> observers;

	public ParseComparator(Reader referenceReader, Reader evalReader, File outDir, TalismaneSession session)
			throws ClassNotFoundException, IOException, ReflectiveOperationException {
		Config config = session.getConfig();
		Config parserConfig = config.getConfig("talismane.core.parser");

		this.referenceCorpusReader = ParserAnnotatedCorpusReader.getCorpusReader(referenceReader, parserConfig.getConfig("input"), session);

		this.evaluationCorpusReader = ParserAnnotatedCorpusReader.getCorpusReader(evalReader, parserConfig.getConfig("evaluate"), session);

		this.observers = ParseEvaluationObserver.getObservers(outDir, session);
	}

	public ParseComparator(ParserAnnotatedCorpusReader referenceCorpusReader, ParserAnnotatedCorpusReader evaluationCorpusReader) {
		this.referenceCorpusReader = referenceCorpusReader;
		this.evaluationCorpusReader = evaluationCorpusReader;
		this.observers = new ArrayList<>();
	}

	public void evaluate() {
		while (referenceCorpusReader.hasNextSentence()) {
			ParseConfiguration realConfiguration = referenceCorpusReader.nextConfiguration();
			ParseConfiguration guessConfiguaration = evaluationCorpusReader.nextConfiguration();
			List<ParseConfiguration> guessConfigurations = new ArrayList<ParseConfiguration>();
			guessConfigurations.add(guessConfiguaration);

			double realLength = realConfiguration.getPosTagSequence().getTokenSequence().getSentence().getText().length();
			double guessedLength = guessConfiguaration.getPosTagSequence().getTokenSequence().getSentence().getText().length();

			double ratio = realLength > guessedLength ? guessedLength / realLength : realLength / guessedLength;
			if (ratio < 0.9) {
				LOG.info("Mismatched sentences");
				LOG.info(realConfiguration.getPosTagSequence().getTokenSequence().getSentence().getText().toString());
				LOG.info(guessConfiguaration.getPosTagSequence().getTokenSequence().getSentence().getText().toString());

				throw new TalismaneException("Mismatched sentences");
			}

			for (ParseEvaluationObserver observer : this.observers) {
				observer.onParseEnd(realConfiguration, guessConfigurations);
			}
		} // next sentence

		for (ParseEvaluationObserver observer : this.observers) {
			observer.onEvaluationComplete();
		}

	}

	public List<ParseEvaluationObserver> getObservers() {
		return observers;
	}

	public void addObserver(ParseEvaluationObserver observer) {
		this.observers.add(observer);
	}

}
