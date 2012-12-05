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
package com.joliciel.talismane;

import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;

/**
 * An interface for processing an input stream and writing the analysis result to an output stream.<br/>
 * The processing must go from a given start module to a given end module in sequence, where the modules available are:
 * Sentence detector, Tokeniser, Pos tagger, Parser.<br/>
 * There is a default input format for each start module,
 * which can be over-ridden by providing a regex for processing lines of input. The default format is:
 * <li>Sentence detector: newlines indicate sentence breaks.</li>
 * <li>Tokeniser: expect exactly one sentence per newline.</li>
 * <li>Pos tagger: {@link  com.joliciel.talismane.tokeniser.TokenRegexBasedCorpusReader#DEFAULT_REGEX default regex} </li>
 * <li>Parser: {@link  com.joliciel.talismane.posTagger.PosTagRegexBasedCorpusReader#DEFAULT_REGEX default regex} </li>
 * <br/>The output format is determined by the processor corresponding to the end-module.<br/>
 * @author Assaf Urieli
 *
 */
public interface Talismane {
	/**
	 * A module within the Talismane Suite.
	 * @author Assaf Urieli
	 *
	 */
	public enum Module {
		SentenceDetector,
		Tokeniser,
		PosTagger,
		Parser
	}
	
	/**
	 * The command which Talismane is asked to perform.
	 * @author Assaf Urieli
	 *
	 */
	public enum Command {
		/**
		 * Analyse a corpus and add annotations.
		 */
		analyse,
		/**
		 * Evaluate an annotated corpus, by re-analysing the corpus and comparing the new annotations to the existing ones.
		 */
		evaluate,
		/**
		 * Process an annotated corpus - Talismane simply reads the corpus using the appropriate corpus reader
		 * and passes the results to the appropriate processors.
		 */
		process
	}

	/**
	 * Analyse the reader from the startModule to the endModule,
	 * where the analysis results are processed by the processors provided.
	 */
	public void analyse(TalismaneConfig config);
	

	public SentenceProcessor getSentenceProcessor();
	public void setSentenceProcessor(SentenceProcessor sentenceProcessor);

	public TokenSequenceProcessor getTokenSequenceProcessor();
	public void setTokenSequenceProcessor(
			TokenSequenceProcessor tokenSequenceProcessor);

	public PosTagSequenceProcessor getPosTagSequenceProcessor();
	public void setPosTagSequenceProcessor(
			PosTagSequenceProcessor posTagSequenceProcessor);

	public ParseConfigurationProcessor getParseConfigurationProcessor();
	public void setParseConfigurationProcessor(
			ParseConfigurationProcessor parseConfigurationProcessor);
	
	/**
	 * If an error occurs during analysis, should Talismane stop immediately, or try to keep going with the next sentence?
	 * Default is true (stop immediately).
	 * @return
	 */
	public boolean isStopOnError();
	public void setStopOnError(boolean stopOnError);
}