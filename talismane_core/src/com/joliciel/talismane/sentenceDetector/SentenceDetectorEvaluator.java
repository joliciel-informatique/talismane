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
package com.joliciel.talismane.sentenceDetector;

import java.io.Writer;

import com.joliciel.talismane.filters.TextFilter;
import com.joliciel.talismane.stats.FScoreCalculator;

/**
 * An interface for evaluating a given sentence detector.
 * @author Assaf Urieli
 *
 */
public interface SentenceDetectorEvaluator {

	/**
	 * Evaluate a given sentence detector.
	 * @param reader for reading manually separated sentences from a corpus
	 * @return an f-score calculator for this sentence detector
	 */
	public FScoreCalculator<SentenceDetectorOutcome> evaluate(SentenceDetectorAnnotatedCorpusReader reader,
			Writer errorWriter);
	

	/**
	 * Add a text filter to be applied prior to analysis.
	 * @param textFilter
	 */
	public void addTextFilter(TextFilter textFilter);
}
