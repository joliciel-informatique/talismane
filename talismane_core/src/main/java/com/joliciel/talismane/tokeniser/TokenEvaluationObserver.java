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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.utils.ArrayListNoNulls;

/**
 * An interface that observes a tokeniser evaluation while its occurring.
 * 
 * @author Assaf Urieli
 *
 */
public interface TokenEvaluationObserver {
	/**
	 * Called when the next token sequence has been processed.
	 */
	public void onNextTokenSequence(TokenSequence realSequence, List<TokenisedAtomicTokenSequence> guessedAtomicSequences);

	public void onEvaluationComplete();

	public static List<TokenEvaluationObserver> getTokenEvaluationObservers(TalismaneSession session) throws IOException {
		List<TokenEvaluationObserver> observers = new ArrayListNoNulls<TokenEvaluationObserver>();
		Writer errorFileWriter = null;
		File errorFile = new File(session.getOutDir(), session.getBaseName() + ".errorList.txt");
		errorFile.delete();
		errorFile.createNewFile();
		errorFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(errorFile, false), "UTF8"));

		Writer csvErrorFileWriter = null;
		File csvErrorFile = new File(session.getOutDir(), session.getBaseName() + ".errors.csv");
		csvErrorFile.delete();
		csvErrorFile.createNewFile();
		csvErrorFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvErrorFile, false), session.getCsvCharset()));

		File fScoreFile = new File(session.getOutDir(), session.getBaseName() + ".fscores.csv");

		TokenEvaluationFScoreCalculator tokenFScoreCalculator = new TokenEvaluationFScoreCalculator();
		tokenFScoreCalculator.setErrorWriter(errorFileWriter);
		tokenFScoreCalculator.setCsvErrorWriter(csvErrorFileWriter);
		tokenFScoreCalculator.setFScoreFile(fScoreFile);
		observers.add(tokenFScoreCalculator);

		Writer corpusFileWriter = null;
		File corpusFile = new File(session.getOutDir(), session.getBaseName() + ".corpus.txt");
		corpusFile.delete();
		corpusFile.createNewFile();
		corpusFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(corpusFile, false), "UTF8"));

		TokenEvaluationCorpusWriter corpusWriter = new TokenEvaluationCorpusWriter(corpusFileWriter);
		observers.add(corpusWriter);

		TokenSequenceProcessor processor = TokenSequenceProcessor.getProcessor(session);

		File freemarkerFile = new File(session.getOutDir(), session.getBaseName() + "_output.txt");
		freemarkerFile.delete();
		freemarkerFile.createNewFile();
		Writer freemakerFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(freemarkerFile, false), session.getOutputCharset()));
		TokeniserGuessTemplateWriter templateWriter = new TokeniserGuessTemplateWriter(processor, freemakerFileWriter);
		observers.add(templateWriter);

		return observers;
	}
}
