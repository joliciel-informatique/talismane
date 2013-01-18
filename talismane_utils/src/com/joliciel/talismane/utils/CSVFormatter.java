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
package com.joliciel.talismane.utils;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Various utilities for formatting text to CSV and reading out of a CSV file.
 * @author Assaf Urieli
 *
 */
public class CSVFormatter {
    private static DecimalFormat decFormat;
    private static DecimalFormat intFormat;
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static boolean addQuotesAlwaysLocal = false;
    private static String csvSeparator = ",";
    
	private static Pattern csvSeparators = Pattern.compile("[,\"]");
	private enum TokenType {
		COMMA, QUOTE, OTHER
	};
    
    static {
	    decFormat = (DecimalFormat) DecimalFormat.getNumberInstance(Locale.US);
	    decFormat.applyPattern("##0.00000000");
	    intFormat = (DecimalFormat) DecimalFormat.getNumberInstance(Locale.US);
	    intFormat.applyPattern("##0");
	    
    }
    
    /**
     * Format a double for inclusion in a CSV.
     * @param number
     * @return
     */
    public static String format(double number) {
    	if (addQuotesAlwaysLocal)
    		return "\"" + decFormat.format(number) + "\"" + csvSeparator;
		return decFormat.format(number) + csvSeparator;
	}
    
    /**
     * Format a float for inclusion in a CSV.
     * @param number
     * @return
     */
    public static String format(float number) {
    	if (addQuotesAlwaysLocal)
    		return "\"" + decFormat.format(number) + "\"" + csvSeparator;
		return decFormat.format(number) + csvSeparator;
	}
    
    /**
     * Format an int for inclusion in a CSV.
     * @param number
     * @return
     */
    public static String format(int number) {
    	if (addQuotesAlwaysLocal)
    		return "\"" + intFormat.format(number) + "\"" + csvSeparator;
		return intFormat.format(number) + csvSeparator;
	}
    
    /**
     * Format a String for inclusion in a CSV.
     * @param number
     * @return
     */
    public static String format(String string) {
    	int quotePos = string.indexOf('"');
    	int commaPos = string.indexOf(csvSeparator);
       	int apostrophePos = string.indexOf('\'');
		if (quotePos>=0) {
			string = string.replace("\"", "\"\"");
		}
		if (quotePos>=0||commaPos>=0||apostrophePos>=0||addQuotesAlwaysLocal)
			return "\"" + string + "\"" + csvSeparator;
		else
			return string + csvSeparator;
   	
    }
    
	/**
	 * Extract a list of cell contents from a given CSV line.
	 * 
	 * @param csvLine
	 * @return
	 */
	public static List<String> getCSVCells(String csvLine) {
		List<String> cells = new ArrayList<String>();
		Matcher matcher = csvSeparators.matcher(csvLine);
		int currentPos = 0;
		List<String> tokens = new ArrayList<String>();
		while (matcher.find()) {
			if (matcher.start() > currentPos) {
				tokens.add(csvLine.substring(currentPos, matcher.start()));
			}
			tokens.add(csvLine.substring(matcher.start(), matcher.end()));
			currentPos = matcher.end();
		}
		tokens.add(csvLine.substring(currentPos));
		StringBuilder currentCell = new StringBuilder();
		boolean inQuote = false;
		TokenType lastToken = TokenType.OTHER;
		for (String token : tokens) {
			if (token.equals("\"")) {
				inQuote = !inQuote;
				if (lastToken.equals(TokenType.QUOTE)) {
					currentCell.append(token);
					lastToken = TokenType.OTHER;
				} else {
					lastToken = TokenType.QUOTE;
				}
			} else if (token.equals(csvSeparator)) {
				if (inQuote) {
					currentCell.append(token);
					lastToken = TokenType.OTHER;
				} else {
					cells.add(currentCell.toString().trim());
					currentCell = new StringBuilder();
					lastToken = TokenType.COMMA;
				}
			} else {
				currentCell.append(token);
				lastToken = TokenType.OTHER;
			}
		}
		if (currentCell.length() > 0)
			cells.add(currentCell.toString().trim());
		return cells;
	}
	
	/**
	 * Return the spreadsheet column label corresponding to a certain index.
	 * @param index
	 * @return
	 */
	public static String getColumnLabel(int index) {
		String columnLabel = "";
		int result = index / 26;
		int remainder = index % 26;
		if (result==0) {
			columnLabel = "" + ALPHABET.charAt(remainder);
		} else {
			columnLabel = "" + ALPHABET.charAt(result - 1) + ALPHABET.charAt(remainder);
		}
		return columnLabel;
	}
	
	/**
	 * Whether or not to systematically add quotes around all cell contents.
	 * @param addQuotesAlways
	 */
	public static void setAddQuotesAlways(boolean addQuotesAlways) {
		addQuotesAlwaysLocal = addQuotesAlways;
	}

	/**
	 * The CSV separator to be used (default is a comma).
	 * @return
	 */
	public static String getCsvSeparator() {
		return csvSeparator;
	}

	public static void setCsvSeparator(String separator) {
		csvSeparator = separator;
		csvSeparators = Pattern.compile("[" + csvSeparator + "\"]");
	}
	
	
}
