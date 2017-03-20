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
package com.joliciel.talismane.languageDetector;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * A processor which writes each block of text followed by a list of possible
 * languages and probabilities.
 * 
 * @author Assaf Urieli
 *
 */
public class DefaultLanguageDetectorProcessor implements LanguageDetectorProcessor {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(DefaultLanguageDetectorProcessor.class);

	private Writer writer;

	public DefaultLanguageDetectorProcessor(Writer writer) {
		this.writer = writer;
	}

	@Override
	public void onNextText(String text, List<WeightedOutcome<Locale>> results) throws IOException {
		writer.write(text + "\n");
		for (WeightedOutcome<Locale> result : results) {
			writer.write(result.getOutcome().toLanguageTag() + "\t" + result.getWeight() + "\n");
		}
		writer.flush();
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

}
