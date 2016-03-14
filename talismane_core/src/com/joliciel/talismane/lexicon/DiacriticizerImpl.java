///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.filters.DiacriticRemover;

class DiacriticizerImpl implements Diacriticizer, Serializable {
	private static final long serialVersionUID = 1L;
	private Map<String,Set<String>> map = new HashMap<String,Set<String>>();
	private Set<String> emptySet = new HashSet<String>();
	private TalismaneSession talismaneSession;
	
	public DiacriticizerImpl() {
	}

	@Override
	public Set<String> diacriticize(String originalWord) {
		String undecorated = DiacriticRemover.removeDiacritics(originalWord);
		String key =  undecorated.toLowerCase();
		String lowercase = originalWord.toLowerCase();
		Set<String> results = map.get(key);
		if (results==null)
			return emptySet;
		Set<String> validResults = new HashSet<String>();
		for (String result : results) {
			boolean validResult = true;
			for (int i=0; i<originalWord.length(); i++) {
				char cO = originalWord.charAt(i);
				char cR = result.charAt(i);
				char cU = undecorated.charAt(i);
				char cL = lowercase.charAt(i);
				if (Character.isUpperCase(cO)) {
					if (cO==cU) {
						// original is undecorated uppercase? anything goes.
						continue;
					}
					if (cL==cR || cO==cR) {
						// original is decorated uppercase, decorated lowercase or uppercase version == result? Fine.
						continue;
					}
					// original is decorated uppercase, decorated lowercase version != result. Bad.
					validResult = false;
					break;
				} else {
					if (cO==cR) {
						// original lowercase == result. Fine
						continue;
					}
					validResult = false;
					break;
				}
			}
			if (validResult)
				validResults.add(result);
		}
		
		if (talismaneSession!=null) {
			String lowercasePreference = talismaneSession.getImplementation().getLowercasePreferences().get(originalWord);
			if (lowercasePreference!=null) {
				Set<String> orderedResults = new LinkedHashSet<String>();
				orderedResults.add(lowercasePreference);
				orderedResults.addAll(validResults);
				validResults = orderedResults;
			}
		}
		return validResults;
	}

	public void addLexicon(Lexicon lexicon) {
		Iterator<LexicalEntry> entries = lexicon.getAllEntries();
		while (entries.hasNext()) {
			LexicalEntry entry = entries.next();
			String key = DiacriticRemover.removeDiacritics(entry.getWord().toLowerCase());
			Set<String> values = map.get(key);
			if (values==null) {
				values = new HashSet<String>();
				map.put(key, values);
			}
			values.add(entry.getWord());
		}
	}

	public TalismaneSession getTalismaneSession() {
		return talismaneSession;
	}

	public void setTalismaneSession(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
	}

	
}
