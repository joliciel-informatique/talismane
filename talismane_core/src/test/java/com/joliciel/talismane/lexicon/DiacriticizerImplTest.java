package com.joliciel.talismane.lexicon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.joliciel.talismane.LanguageImplementation;
import com.joliciel.talismane.TalismaneSession;

import mockit.NonStrict;
import mockit.NonStrictExpectations;

public class DiacriticizerImplTest {

	@Test
	public void testDiacriticize(@NonStrict final Lexicon lexicon, @NonStrict final TalismaneSession talismaneSession,
			@NonStrict final LanguageImplementation languageImplementation) {

		new NonStrictExpectations() {
			LexicalEntry l1, l2, l3, l4, l5, l6, l7, l8, l9;
			{
				Map<String, String> lowercasePreferences = new HashMap<>();
				lowercasePreferences.put("A", "à");

				languageImplementation.getLowercasePreferences();
				returns(lowercasePreferences);
				talismaneSession.getImplementation();
				returns(languageImplementation);
				talismaneSession.getLocale();
				returns(Locale.FRENCH);

				List<LexicalEntry> lexicalEntries = new ArrayList<LexicalEntry>();
				lexicalEntries.add(l1);
				lexicalEntries.add(l2);
				lexicalEntries.add(l3);
				lexicalEntries.add(l4);
				lexicalEntries.add(l5);
				lexicalEntries.add(l6);
				lexicalEntries.add(l7);
				lexicalEntries.add(l8);
				lexicalEntries.add(l9);

				l1.getWord();
				returns("mangé");
				l2.getWord();
				returns("mange");
				l3.getWord();
				returns("mangée");
				l4.getWord();
				returns("SARL");
				l5.getWord();
				returns("bala");
				l6.getWord();
				returns("bàlà");
				l7.getWord();
				returns("bàla");
				l8.getWord();
				returns("a");
				l9.getWord();
				returns("à");
				lexicon.getAllEntries();
				returns(lexicalEntries.iterator());
			}
		};

		DiacriticizerImpl diacriticizer = new DiacriticizerImpl();
		diacriticizer.setTalismaneSession(talismaneSession);
		diacriticizer.addLexicon(lexicon);

		Set<String> results = diacriticizer.diacriticize("MANGEE");
		assertEquals(1, results.size());
		assertEquals("mangée", results.iterator().next());

		// all lower-case letters are kept as is
		results = diacriticizer.diacriticize("mange");
		assertEquals(1, results.size());
		assertEquals("mange", results.iterator().next());

		// accented letters are kept as is
		results = diacriticizer.diacriticize("Mangé");
		assertEquals(1, results.size());
		assertEquals("mangé", results.iterator().next());

		results = diacriticizer.diacriticize("MANGE");
		assertEquals(2, results.size());
		List<String> resultList = new ArrayList<String>(results);
		assertEquals("mange", resultList.get(0));
		assertEquals("mangé", resultList.get(1));

		// accented letters are kept as is even if the accent is uppercase
		results = diacriticizer.diacriticize("MANGÉ");
		assertEquals(1, results.size());
		assertTrue(results.contains("mangé"));

		results = diacriticizer.diacriticize("SARL");
		assertEquals(1, results.size());
		assertEquals("SARL", results.iterator().next());

		results = diacriticizer.diacriticize("sarl");
		assertEquals(0, results.size());

		// ordered by default from fewer accents to more accents
		results = diacriticizer.diacriticize("BALA");
		assertEquals(3, results.size());
		resultList = new ArrayList<String>(results);
		assertEquals("bala", resultList.get(0));
		assertEquals("bàla", resultList.get(1));
		assertEquals("bàlà", resultList.get(2));

		// here "à" will come first because we added an exception
		results = diacriticizer.diacriticize("A");
		assertEquals(2, results.size());
		resultList = new ArrayList<String>(results);
		assertEquals("à", resultList.get(0));
		assertEquals("a", resultList.get(1));

		// test with combining diacritics
		results = diacriticizer.diacriticize("mange\u0301");
		assertEquals(1, results.size());
		assertTrue(results.contains("mangé"));
	}

}
