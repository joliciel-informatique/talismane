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
package com.joliciel.talismane.lexicon;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.utils.CoNLLFormatter;

/**
 * A Lexical Entry reader based on a regex.<br/>
 * The regex needs to contain the following five capturing groups, indicated by the following strings:<br/>
 * <li>%TOKEN%: the token - note that we assume CoNLL formatting (with underscores for spaces and for empty tokens). The sequence &amp;und; should be used for true underscores.</li>
 * <li>%LEMMA%: the lemma - note that we assume CoNLL formatting (with underscores for spaces and for missing lemmas)</li>
 * <li>%POSTAG%: the token's pos-tag - optional, since this might be read from the %MORPH% string.</li>
 * <li>%MORPH%: the token's morphology.<br/>
 * The placeholders, except for %MORPH%, will be replaced by (.+) meaning no empty strings allowed.
 * @author Assaf Urieli
 *
 */
public class RegexLexicalEntryReader implements LexicalEntryReader {
	private static final String DEFAULT_REGEX = ".*\\t%TOKEN%\\t%LEMMA%\\t%POSTAG%\\t.*\\t%MORPH%\\t.*\\t.*\\t_\\t_";;
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
		
		if (matcher.groupCount()!=placeholderIndexMap.size()) {
			throw new TalismaneException("Expected " + placeholderIndexMap.size() + " matches (but found " + matcher.groupCount() + ") on line:" + line);
		}
		
		String token =  matcher.group(placeholderIndexMap.get("%TOKEN%"));
		token = CoNLLFormatter.fromCoNLL(token);
		String lemma =  matcher.group(placeholderIndexMap.get("%LEMMA%"));
		lemma = CoNLLFormatter.fromCoNLL(lemma);
		String postag = "";
		if (placeholderIndexMap.get("%POSTAG%")!=null)
			postag = matcher.group(placeholderIndexMap.get("%POSTAG%"));
		
		String morphology = matcher.group(placeholderIndexMap.get("%MORPH%"));
		
		LexicalEntry lexicalEntry = this.morphologyReader.readEntry(token, lemma, postag, morphology);
		return lexicalEntry;
	}
	
	public Pattern getPattern() {
		if (this.pattern == null) {
			int tokenPos = regex.indexOf("%TOKEN%");
			if (tokenPos<0)
				throw new TalismaneException("The regex must contain the string \"%TOKEN%\"");

			int lemmaPos = regex.indexOf("%LEMMA%");
			if (lemmaPos<0)
				throw new TalismaneException("The regex must contain the string \"%LEMMA%\"");

			int posTagPos = regex.indexOf("%POSTAG%");

			int morphPos = regex.indexOf("%MORPH%");
			if (morphPos<0)
				throw new TalismaneException("The regex must contain the string \"%MORPH%\"");
			
			
			Map<Integer, String> placeholderMap = new TreeMap<Integer, String>();
			placeholderMap.put(tokenPos, "%TOKEN%");
			placeholderMap.put(lemmaPos, "%LEMMA%");
			if (posTagPos>=0)
				placeholderMap.put(posTagPos, "%POSTAG%");
			placeholderMap.put(morphPos, "%MORPH%");
			
			int i = 1;
			for (String placeholderName : placeholderMap.values()) {
				placeholderIndexMap.put(placeholderName, i++);
			}
			
			String regexWithGroups = regex.replace("%TOKEN%", "(.+)");
			regexWithGroups = regexWithGroups.replace("%LEMMA%", "(.+)");
			regexWithGroups = regexWithGroups.replace("%POSTAG%", "(.+)");
			
			if (posTagPos>=0)
				regexWithGroups = regexWithGroups.replace("%MORPH%", "(.*)");
			else 
				regexWithGroups = regexWithGroups.replace("%MORPH%", "(.+)");
			
			this.pattern = Pattern.compile(regexWithGroups, Pattern.UNICODE_CHARACTER_CLASS);
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
