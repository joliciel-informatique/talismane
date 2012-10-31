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
package com.joliciel.talismane.lexicon;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.joliciel.talismane.TalismaneException;

public class RegexLexicalEntryReader implements LexicalEntryReader {
	private static final String DEFAULT_REGEX = ".*\\tTOKEN\\tLEMMA\\tPOSTAG\\t.*\\tMORPH\\t.*\\t.*\\t_\\t_";;
	private Pattern pattern;
	private String regex = DEFAULT_REGEX;
	private Map<String, Integer> placeholderIndexMap = new HashMap<String, Integer>();
	private LexicalEntryMorphologyReader morphologyReader;
	
	public RegexLexicalEntryReader(LexicalEntryMorphologyReader morphologyReader) {
		this.morphologyReader = morphologyReader;
	}
	
	@Override
	public LexicalEntry readEntry(String line) {
		Matcher matcher = this.getPattern().matcher(line);
		if (!matcher.matches())
			throw new TalismaneException("Didn't match pattern \"" + regex + "\" on line: " + line);
		
		if (matcher.groupCount()!=4) {
			throw new TalismaneException("Expected 4 matches (but found " + matcher.groupCount() + ") on line:" + line);
		}
		
		String token =  matcher.group(placeholderIndexMap.get("TOKEN"));
		String lemma =  matcher.group(placeholderIndexMap.get("LEMMA"));
		String postag = matcher.group(placeholderIndexMap.get("POSTAG"));
		String morphology = matcher.group(placeholderIndexMap.get("MORPH"));
		
		LexicalEntry lexicalEntry = this.morphologyReader.readEntry(token, lemma, postag, morphology);
		return lexicalEntry;
	}
	
	public Pattern getPattern() {
		if (this.pattern == null) {
			int tokenPos = regex.indexOf("TOKEN");
			if (tokenPos<0)
				throw new TalismaneException("The regex must contain the string \"TOKEN\"");

			int lemmaPos = regex.indexOf("LEMMA");
			if (lemmaPos<0)
				throw new TalismaneException("The regex must contain the string \"LEMMA\"");

			int posTagPos = regex.indexOf("POSTAG");
			if (posTagPos<0)
				throw new TalismaneException("The regex must contain the string \"POSTAG\"");

			int morphPos = regex.indexOf("MORPH");
			if (morphPos<0)
				throw new TalismaneException("The regex must contain the string \"MORPH\"");
			
			
			Map<Integer, String> placeholderMap = new TreeMap<Integer, String>();
			placeholderMap.put(tokenPos, "TOKEN");
			placeholderMap.put(lemmaPos, "LEMMA");
			placeholderMap.put(posTagPos, "POSTAG");
			placeholderMap.put(morphPos, "MORPH");
			
			int i = 1;
			for (String placeholderName : placeholderMap.values()) {
				placeholderIndexMap.put(placeholderName, i++);
			}
			
			String regexWithGroups = regex.replace("TOKEN", "(.*)");
			regexWithGroups = regexWithGroups.replace("LEMMA", "(.*)");
			regexWithGroups = regexWithGroups.replace("POSTAG", "(.+)");
			regexWithGroups = regexWithGroups.replace("MORPH", "(.*)");
			this.pattern = Pattern.compile(regexWithGroups);
		}
		return pattern;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}
}
