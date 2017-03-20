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

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * Any class that can process results found by a language detector.
 * 
 * @author Assaf Urieli
 *
 */
public interface LanguageDetectorProcessor extends Closeable {
	/**
	 * Process the next text.
	 * 
	 * @throws IOException
	 */
	public void onNextText(String text, List<WeightedOutcome<Locale>> results) throws IOException;

	public static LanguageDetectorProcessor getProcessor(Writer writer, TalismaneSession session) throws IOException {
		return new DefaultLanguageDetectorProcessor(writer);
	}
}
