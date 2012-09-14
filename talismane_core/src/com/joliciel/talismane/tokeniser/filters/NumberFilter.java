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
package com.joliciel.talismane.tokeniser.filters;

import java.text.DecimalFormatSymbols;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * Replace all numbers by certain the following classes:
 * 9.99 if it's a decimal number (9,99 in France etc.)
 * 31 if it's from 1 to 31
 * 1999 if it's from 1000 to 2200
 * 999 otherwise
 * @author Assaf Urieli
 */
public class NumberFilter implements TokenFilter {
	Pattern numberPattern = null;
	int fractionalGroup = 11;
	int scientificGroup = 12;
	String decimalSeparator = "";
	String thousandsSeparator = "";
	
	public NumberFilter() {
		DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance(TalismaneSession.getLocale());
		decimalSeparator = "" + decimalFormatSymbols.getDecimalSeparator();
		thousandsSeparator = "" + decimalFormatSymbols.getGroupingSeparator();
		// note there's an invisible no-break space in the if statements below! (it looks like a space, but isn't)
		if (decimalSeparator.equals(",") && (thousandsSeparator.equals(".") || thousandsSeparator.equals(" ")|| thousandsSeparator.equals(" "))) {
			thousandsSeparator = "[ \\.]";
		} else if (thousandsSeparator.equals(" ") || thousandsSeparator.equals(" ")) {
			thousandsSeparator = "[ ]";
		}
		String sign = "([-+])"; // 1 group
		String integerPart = "(([1-9][0-9]*)|([1-9][0-9]{0,2}(" + thousandsSeparator + "[0-9]{3})*))"; // 4 groups
		String fractionalPart = "(" + decimalSeparator + "[0-9]+)"; // 1 group
		String scientificNotation = "([eE][-+]?[0-9]+)"; // 1 group
		String regex = sign + "?" + "(" + integerPart + "|" + integerPart + "?" + fractionalPart + ")" + scientificNotation + "?";
		numberPattern = Pattern.compile(regex);
	}
	
	@Override
	public void apply(TokenSequence tokenSequence) {
		for (Token token : tokenSequence) {
			String newWord = this.replaceNumberString(token.getText());
			if (newWord!=null)
				token.setText(newWord);
		}
	}

	String replaceNumberString(String word) {
		String newWord = word;
		Matcher matcher = numberPattern.matcher(word);
		if (matcher.matches()) {
			String fraction = matcher.group(fractionalGroup);
			String exponent = matcher.group(scientificGroup);
			if ((fraction!=null && fraction.length()>0) || (exponent!=null && exponent.length()>0))
				newWord = "9" + decimalSeparator + "99";
			else {
				try {
					int number = Integer.parseInt(word);
					if (number>0 && number<=31)
						newWord = "31";
					else if (number>=1000 && number<=2200)
						newWord = "1999";
					else
						newWord = "999";
				} catch (NumberFormatException nfe) {
					newWord = "999";			
				}				
			}
		}

		return newWord;
	}
}
