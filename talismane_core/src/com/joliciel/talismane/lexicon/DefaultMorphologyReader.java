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

/**
 * In the morphology string, expects entries such as:
 * number=plural;tense=ind;person=1,2;<br/>
 * Where attributes are separated by semicolons and values by commas.
 * The valid attributes are: number, gender, tense, person.
 * @author Assaf
 *
 */
public class DefaultMorphologyReader implements LexicalEntryMorphologyReader {

	@Override
	public LexicalEntry readEntry(String token, String lemma, String category,
			String morphology) {
		DefaultLexicalEntry entry = new DefaultLexicalEntry();
		entry.setWord(token);
		entry.setLemma(lemma);
		entry.setCategory(category);
		String[] morphParts = morphology.split("\\|");
		for (String morphPart : morphParts) {
			morphPart = morphPart.trim();
			if (morphPart.length()>0) {
				String attribute = morphPart.substring(0, morphPart.indexOf('='));
				String[] values = morphPart.substring(morphPart.indexOf('=')+1).split(",");
				if (attribute.equals("number")) {
					for (String value : values)
						entry.getNumber().add(value);
				} else if (attribute.equals("gender")) {
					for (String value : values)
						entry.getGender().add(value);
				} else if (attribute.equals("person")) {
					for (String value : values)
						entry.getPerson().add(value);
				} else if (attribute.equals("tense")) {
					for (String value : values)
						entry.getTense().add(value);
				}
			}
		}
		return entry;
	}

}
