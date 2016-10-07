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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.filters.DiacriticRemover;
import com.joliciel.talismane.utils.LogUtils;

class DiacriticizerImpl implements Diacriticizer, Serializable {
	private static final Log LOG = LogFactory.getLog(DiacriticizerImpl.class);

	private static final long serialVersionUID = 1L;
	private Map<String, Set<String>> map = new HashMap<String, Set<String>>();
	private Set<String> emptySet = new HashSet<String>();
	private transient TalismaneSession talismaneSession;

	public DiacriticizerImpl() {
	}

	@Override
	public Set<String> diacriticize(String originalWord) {
		// TODO: this code relies on usage of precomposed characters only
		// where there is a one-to-one equivalence in length between all strings, whether decorated or not
		// In order to make this function for the latin alphabet, we recompose any combining diacriticals
		// However this is not an ideal solution, as many alphabets do not contain precomposed characters
		originalWord = this.recompose(originalWord);
		
		String undecorated = DiacriticRemover.removeDiacritics(originalWord);
		String key = undecorated.toLowerCase();
		String lowercase = originalWord.toLowerCase();
		Set<String> results = map.get(key);
		if (results == null)
			return emptySet;
		Set<String> validResults = null;
		if (talismaneSession != null)
			validResults = new TreeSet<String>(Collator.getInstance(talismaneSession.getLocale()));
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

		if (talismaneSession != null) {
			String lowercasePreference = talismaneSession.getImplementation().getLowercasePreferences().get(originalWord);
			if (lowercasePreference != null) {
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
			if (values == null) {
				values = new HashSet<String>();
				map.put(key, values);
			}
			values.add(this.recompose(entry.getWord()));
		}
	}
	
	private String recompose(String original) {
		String recomposed = original;
		if (recomposed.indexOf('\u0300')>=0) {
			recomposed = recomposed.replaceAll("a\u0300", "à");
			recomposed = recomposed.replaceAll("e\u0300", "è");
			recomposed = recomposed.replaceAll("i\u0300", "ì");
			recomposed = recomposed.replaceAll("o\u0300", "ò");
			recomposed = recomposed.replaceAll("u\u0300", "ù");
			recomposed = recomposed.replaceAll("A\u0300", "À");
			recomposed = recomposed.replaceAll("E\u0300", "È");
			recomposed = recomposed.replaceAll("I\u0300", "Ì");
			recomposed = recomposed.replaceAll("O\u0300", "Ò");
			recomposed = recomposed.replaceAll("U\u0300", "Ù");
		}
		if (recomposed.indexOf('\u0301')>=0) {
			recomposed = recomposed.replaceAll("a\u0301", "á");
			recomposed = recomposed.replaceAll("e\u0301", "é");
			recomposed = recomposed.replaceAll("i\u0301", "í");
			recomposed = recomposed.replaceAll("o\u0301", "ó");
			recomposed = recomposed.replaceAll("u\u0301", "ú");
			recomposed = recomposed.replaceAll("A\u0301", "Á");
			recomposed = recomposed.replaceAll("E\u0301", "É");
			recomposed = recomposed.replaceAll("I\u0301", "Í");
			recomposed = recomposed.replaceAll("O\u0301", "Ó");
			recomposed = recomposed.replaceAll("U\u0301", "Ú");
		}
		if (recomposed.indexOf('\u0302')>=0) {
			recomposed = recomposed.replaceAll("a\u0302", "â");
			recomposed = recomposed.replaceAll("e\u0302", "ê");
			recomposed = recomposed.replaceAll("i\u0302", "î");
			recomposed = recomposed.replaceAll("o\u0302", "ô");
			recomposed = recomposed.replaceAll("u\u0302", "û");
			recomposed = recomposed.replaceAll("A\u0302", "Â");
			recomposed = recomposed.replaceAll("E\u0302", "Ê");
			recomposed = recomposed.replaceAll("I\u0302", "Î");
			recomposed = recomposed.replaceAll("O\u0302", "Ô");
			recomposed = recomposed.replaceAll("U\u0302", "Û");
		}
		if (recomposed.indexOf('\u0308')>=0) {
			recomposed = recomposed.replaceAll("a\u0308", "ä");
			recomposed = recomposed.replaceAll("e\u0308", "ë");
			recomposed = recomposed.replaceAll("i\u0308", "ï");
			recomposed = recomposed.replaceAll("o\u0308", "ö");
			recomposed = recomposed.replaceAll("u\u0308", "ü");
			recomposed = recomposed.replaceAll("A\u0308", "Ä");
			recomposed = recomposed.replaceAll("E\u0308", "Ë");
			recomposed = recomposed.replaceAll("I\u0308", "Ï");
			recomposed = recomposed.replaceAll("O\u0308", "Ö");
			recomposed = recomposed.replaceAll("U\u0308", "Ü");
		}
		if (recomposed.indexOf('\u0327')>=0) {
			recomposed = recomposed.replaceAll("c\u0327", "ç");
			recomposed = recomposed.replaceAll("C\u0327", "Ç");
		}
		if (recomposed.indexOf('\u0303')>=0) {
			recomposed = recomposed.replaceAll("a\u0303", "ã");
			recomposed = recomposed.replaceAll("e\u0303", "ẽ");
			recomposed = recomposed.replaceAll("i\u0303", "ĩ");
			recomposed = recomposed.replaceAll("n\u0303", "ñ");
			recomposed = recomposed.replaceAll("o\u0303", "õ");
			recomposed = recomposed.replaceAll("u\u0303", "ũ");
			recomposed = recomposed.replaceAll("A\u0303", "Ã");
			recomposed = recomposed.replaceAll("E\u0303", "Ẽ");
			recomposed = recomposed.replaceAll("I\u0303", "Ĩ");
			recomposed = recomposed.replaceAll("N\u0303", "Ñ");
			recomposed = recomposed.replaceAll("O\u0303", "Õ");
			recomposed = recomposed.replaceAll("U\u0303", "Ũ");
		}

		return recomposed;
	}

	@Override
	public TalismaneSession getTalismaneSession() {
		return talismaneSession;
	}

	@Override
	public void setTalismaneSession(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
	}

	public void serialize() {

	}

	public static Diacriticizer deserialize(File inFile, TalismaneSession talismaneSession) {
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
			diacriticizer.setTalismaneSession(talismaneSession);

			return diacriticizer;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
}
