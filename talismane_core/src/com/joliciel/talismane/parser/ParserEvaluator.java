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

import java.io.Writer;

import com.joliciel.talismane.stats.FScoreCalculator;

/**
 * Evaluate a parser.
 * @author Assaf Urieli
 *
 */
public interface ParserEvaluator {
	public FScoreCalculator<String> evaluate(ParserAnnotatedCorpusReader corpusReader);
	
	public Parser getParser();
	public void setParser(Parser parser);

	public abstract void setLabeledEvaluation(boolean labeledEvaluation);

	public abstract boolean isLabeledEvaluation();
	
	/**
	 * If provided, will write the evaluation of each sentence
	 * to a csv file.
	 * @param csvFileWriter
	 */
	public Writer getCsvFileWriter();
	public void setCsvFileWriter(Writer csvFileWriter);
}
