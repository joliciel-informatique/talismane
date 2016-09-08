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
package com.joliciel.talismane.utils;

public class CoNLLFormatter {
	/**
	 * Convert a string to CoNLL format.
	 */
	public static String toCoNLL(String text) {
		String conllText = text;
		if (conllText.equals("_"))
			conllText = "&und;";
		else if (conllText.length() == 0)
			conllText = "_";
		return conllText;
	}

	/**
	 * Convert a string from CoNLL format.
	 */
	public static String fromCoNLL(String conllText) {
		String text = conllText;
		if (conllText.equals("_"))
			text = "";
		else if (conllText.equals("&und;"))
			text = "_";
		return text;
	}
}
