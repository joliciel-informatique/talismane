///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Joliciel Informatique
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
package com.joliciel.talismane.sentenceAnnotators;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Removes all diacritics from text.
 * 
 * @author Assaf Urieli
 *
 */
public class DiacriticRemover implements TextReplacer {
	private static Pattern diacriticPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

	@Override
	public void replace(List<String> tokens) {
		for (int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);
			tokens.set(i, removeDiacritics(token));
		}
	}

	public static String removeDiacritics(String string) {
		return diacriticPattern.matcher(Normalizer.normalize(string, Form.NFD)).replaceAll("");
	}
}
