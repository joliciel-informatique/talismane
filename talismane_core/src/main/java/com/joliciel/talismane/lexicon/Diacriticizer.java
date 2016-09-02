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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.Collator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.tokeniser.filters.DiacriticRemover;
import com.joliciel.talismane.utils.LogUtils;

/**
 * An interface for retrieving, for a given original word, assumed to be
 * uppercase without diacritics, the various lowercase possibilities which can
 * contain diacritics. Useful for converting text in ALL CAPS to possible words.
 * 
 * @author Assaf Urieli
 *
 */
public class Diacriticizer implements Serializable {
	private static final Logger LOG = LoggerFactory.getLogger(Diacriticizer.class);

	private static final long serialVersionUID = 1L;
	private Map<String, Set<String>> map = new HashMap<String, Set<String>>();
	private Set<String> emptySet = new HashSet<String>();
	private Map<String, String> lowercasePreferences = new HashMap<String, String>();
	private Locale locale;

	public Diacriticizer() {
	}

	public Diacriticizer(Lexicon lexicon) {
		this.addLexicon(lexicon);
	}

	/**
	 * Given a word, will try to find equivalent lowercase words with
	 * diacritics. By equivalent we mean: for each letter in the original word,
	 * if the letter is undecorated uppercase, the equivalent letter must be a
	 * decorated or undecorated lowercase or uppercase. If the original letter
	 * is in the lowercase, it must remain identical. If the original letter is
	 * a decorated uppercase, the equivalent letter must be the decorated
	 * lowercase or uppercase.<br/>
	 * Thus, for a french glossary, "MANGE" will return "mangé" and "mange", "A"
	 * will return "à" and "a", "À" will return only "à", and "a" will return
	 * only "a".
	 */
	public Set<String> diacriticize(String originalWord) {
		String undecorated = DiacriticRemover.removeDiacritics(originalWord);
		String key = undecorated.toLowerCase();
		String lowercase = originalWord.toLowerCase();
		Set<String> results = map.get(key);
		if (results == null)
			return emptySet;
		Set<String> validResults = null;
		if (locale != null)
			validResults = new TreeSet<String>(Collator.getInstance(locale));
		else
			validResults = new TreeSet<String>();

		for (String result : results) {
			boolean validResult = true;
			for (int i = 0; i < originalWord.length(); i++) {
				char cO = originalWord.charAt(i);
				char cR = result.charAt(i);
				char cU = undecorated.charAt(i);
				char cL = lowercase.charAt(i);
				if (Character.isUpperCase(cO)) {
					if (cO == cU) {
						// original is undecorated uppercase? anything goes.
						continue;
					}
					if (cL == cR || cO == cR) {
						// original is decorated uppercase, decorated lowercase
						// or uppercase version == result? Fine.
						continue;
					}
					// original is decorated uppercase, decorated lowercase
					// version != result. Bad.
					validResult = false;
					break;
				} else {
					if (cO == cR) {
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

		String lowercasePreference = this.getLowercasePreferences().get(originalWord);
		if (lowercasePreference != null) {
			Set<String> orderedResults = new LinkedHashSet<String>();
			orderedResults.add(lowercasePreference);
			orderedResults.addAll(validResults);
			validResults = orderedResults;
		}

		return validResults;
	}

	public void addLexicon(Lexicon lexicon) {
		Iterator<LexicalEntry> entries = lexicon.getAllEntries();
		while (entries.hasNext()) {
			LexicalEntry entry = entries.next();
			String key = DiacriticRemover.removeDiacritics(entry.getWord().toLowerCase());
			Set<String> values = map.get(key);
			if (values == null) {
				values = new HashSet<String>();
				map.put(key, values);
			}
			values.add(entry.getWord());
		}
	}

	public void serialize() {

	}

	public static Diacriticizer deserialize(File inFile) {
		try {
			FileInputStream fis = new FileInputStream(inFile);
			ZipInputStream zis = new ZipInputStream(fis);
			ZipEntry ze = null;
			Diacriticizer diacriticizer = null;
			while ((ze = zis.getNextEntry()) != null) {
				if (ze.getName().endsWith(".obj")) {
					LOG.debug("deserializing " + ze.getName());
					@SuppressWarnings("resource")
					ObjectInputStream in = new ObjectInputStream(zis);
					diacriticizer = (Diacriticizer) in.readObject();

					break;
				}
			}
			zis.close();

			return diacriticizer;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	public Map<String, String> getLowercasePreferences() {
		return lowercasePreferences;
	}

	public void setLowercasePreferences(Map<String, String> lowercasePreferences) {
		this.lowercasePreferences = lowercasePreferences;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

}
