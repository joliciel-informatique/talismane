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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;


class ParseComparatorImpl implements ParseComparator {
	private static final Log LOG = LogFactory.getLog(ParseComparatorImpl.class);
	private int sentenceCount = 0;
	
	private ParserServiceInternal parserServiceInternal;
	private List<ParseEvaluationObserver> observers = new ArrayList<ParseEvaluationObserver>();
	
	@Override
	public void evaluate(ParserAnnotatedCorpusReader referenceCorpusReader,
			ParserAnnotatedCorpusReader evaluationCorpusReader) {
		int sentenceIndex = 0;
		while (referenceCorpusReader.hasNextConfiguration()) {
			ParseConfiguration realConfiguration = referenceCorpusReader.nextConfiguration();
			ParseConfiguration guessConfiguaration = evaluationCorpusReader.nextConfiguration();
			List<ParseConfiguration> guessConfigurations = new ArrayList<ParseConfiguration>();
			guessConfigurations.add(guessConfiguaration);
			
			double realLength = realConfiguration.getPosTagSequence().getTokenSequence().getSentence().getText().length();
			double guessedLength = guessConfiguaration.getPosTagSequence().getTokenSequence().getSentence().getText().length();

			double ratio = realLength > guessedLength ? guessedLength / realLength : realLength / guessedLength;
			if (ratio < 0.9) {
				LOG.info("Mismatched sentences");
				LOG.info(realConfiguration.getPosTagSequence().getTokenSequence().getSentence().getText());
				LOG.info(guessConfiguaration.getPosTagSequence().getTokenSequence().getSentence().getText());
				
				throw new TalismaneException("Mismatched sentences");
			}
			
			for (ParseEvaluationObserver observer : this.observers) {
				observer.onParseEnd(realConfiguration, guessConfigurations);
			}
			sentenceIndex++;
			if (sentenceCount>0 && sentenceIndex==sentenceCount)
				break;
		} // next sentence
		
		for (ParseEvaluationObserver observer : this.observers) {
			observer.onEvaluationComplete();
		}

	}
	
	public ParserServiceInternal getParserServiceInternal() {
		return parserServiceInternal;
	}

	public void setParserServiceInternal(ParserServiceInternal parserServiceInternal) {
		this.parserServiceInternal = parserServiceInternal;
	}

	public List<ParseEvaluationObserver> getObservers() {
		return observers;
	}

	public void setObservers(List<ParseEvaluationObserver> observers) {
		this.observers = observers;
	}

	public void addObserver(ParseEvaluationObserver observer) {
		this.observers.add(observer);
	}

	@Override
	public int getSentenceCount() {
		return sentenceCount;
	}

	@Override
	public void setSentenceCount(int sentenceCount) {
		this.sentenceCount = sentenceCount;
	}
	
	
}
