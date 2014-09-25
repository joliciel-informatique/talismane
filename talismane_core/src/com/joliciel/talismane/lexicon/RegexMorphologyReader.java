package com.joliciel.talismane.lexicon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A morphology reader where the parts are identified via a set of regex.
 * These can be read from a Scanner, where each line is a regex through which one or more morphological attributes
 * can be extracted. These contain the following placeholders:<br/>
 * <li>Gender: %GENDER%</li>
 * <li>Number: %NUMBER%</li>
 * <li>Tense: %TENSE%</li>
 * <li>Person: %PERSON%</li>
 * <li>Posser number: %POSSESSOR%</li>
 * <p>
 * For example, if the input string looked like "g=f|p=3|n=s" (for gender=female, person=3, number=singular), we might 
 * have the following input:</p>
 * <pre>
 * \bg=%GENDER%\b
 * \bp=%PERSON%\b
 * \bn=%NUMBER%\b
 * </pre>
 * @author Assaf Urieli
 *
 */
public class RegexMorphologyReader implements LexicalEntryMorphologyReader {
	List<Pattern> patterns = new ArrayList<Pattern>();
	Map<String,Integer> patternIndexMap = new HashMap<String, Integer>();
	Map<String,Integer> groupIndexMap = new HashMap<String, Integer>();
	
	public RegexMorphologyReader(Scanner scanner) {
		int i=0;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			
			Map<Integer,String> placeholderMap = new TreeMap<Integer, String>();
			this.updateMaps(line, i, "%GENDER%", placeholderMap);
			this.updateMaps(line, i, "%NUMBER%", placeholderMap);
			this.updateMaps(line, i, "%PERSON%", placeholderMap);
			this.updateMaps(line, i, "%TENSE%", placeholderMap);
			this.updateMaps(line, i, "%POSSESSOR%", placeholderMap);
			
			int previousGroups = 0;
			for (String placeholder : placeholderMap.values()) {
				groupIndexMap.put(placeholder, groupIndexMap.get(placeholder)+previousGroups);
				previousGroups++;
			}
			line = line.replace("%GENDER%", "(.+)");
			line = line.replace("%NUMBER%", "(.+)");
			line = line.replace("%PERSON%", "(.+)");
			line = line.replace("%TENSE%", "(.+)");
			line = line.replace("%POSSESSOR%", "(.+)");
			
			Pattern pattern = Pattern.compile(line, Pattern.UNICODE_CHARACTER_CLASS);
			patterns.add(pattern);
		}
		scanner.close();
	}
	
	private void updateMaps(String line, int i, String placeholder, Map<Integer,String> placeholderMap) {
		int linePos = line.indexOf(placeholder);
		if (linePos>0) {
			int groupNumber = this.getGroupNumber(line, linePos);
			patternIndexMap.put(placeholder, i);
			groupIndexMap.put(placeholder, groupNumber);
			placeholderMap.put(linePos, placeholder);
		}
	}
	
	private int getGroupNumber(String line, int index) {
		int groupCount = 0;
		for (int i=0; i<index; i++) {
			char c = line.charAt(i);
			if (c=='\\') {
				// skip next character
				i++;
			} else if (c=='(') {
				groupCount++;
			}
		}
		return groupCount+1;
	}
	
	@Override
	public LexicalEntry readEntry(String token, String lemma, String category,
			String morphology) {
		DefaultLexicalEntry lexicalEntry = new DefaultLexicalEntry();
		lexicalEntry.setWord(token);
		lexicalEntry.setLemma(lemma);
		lexicalEntry.setCategory(category);
		
		int i=0;
		for (Pattern pattern : patterns) {
			Matcher matcher = pattern.matcher(morphology);
			if (patternIndexMap.containsKey("%GENDER%") && patternIndexMap.get("%GENDER%")==i) {
				int groupIndex = groupIndexMap.get("%GENDER%");
				String gender = matcher.group(groupIndex);
				lexicalEntry.getGender().add(gender);
			}
			if (patternIndexMap.containsKey("%NUMBER%") && patternIndexMap.get("%NUMBER%")==i) {
				int groupIndex = groupIndexMap.get("%NUMBER%");
				String number = matcher.group(groupIndex);
				lexicalEntry.getNumber().add(number);
			}
			if (patternIndexMap.containsKey("%PERSON%") && patternIndexMap.get("%PERSON%")==i) {
				int groupIndex = groupIndexMap.get("%PERSON%");
				String person = matcher.group(groupIndex);
				lexicalEntry.getPerson().add(person);
			}
			if (patternIndexMap.containsKey("%TENSE%") && patternIndexMap.get("%TENSE%")==i) {
				int groupIndex = groupIndexMap.get("%TENSE%");
				String tense = matcher.group(groupIndex);
				lexicalEntry.getTense().add(tense);
			}
			if (patternIndexMap.containsKey("%POSSESSOR%") && patternIndexMap.get("%POSSESSOR%")==i) {
				int groupIndex = groupIndexMap.get("%POSSESSOR%");
				String possessor = matcher.group(groupIndex);
				lexicalEntry.getPossessorNumber().add(possessor);
			}
			i++;
		}
		return lexicalEntry;
	}

}
