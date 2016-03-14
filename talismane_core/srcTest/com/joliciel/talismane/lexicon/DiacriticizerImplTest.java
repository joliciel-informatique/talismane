package com.joliciel.talismane.lexicon;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.junit.Test;

public class DiacriticizerImplTest {

	@Test
	public void testDiacriticize(@NonStrict final Lexicon lexicon) {
		
		new NonStrictExpectations() {
			LexicalEntry l1, l2, l3, l4;
			{
				List<LexicalEntry> lexicalEntries = new ArrayList<LexicalEntry>();
				lexicalEntries.add(l1);
				lexicalEntries.add(l2);
				lexicalEntries.add(l3);
				lexicalEntries.add(l4);
				l1.getWord(); returns("mangé");
				l2.getWord(); returns("mange");
				l3.getWord(); returns("mangée");
				l4.getWord(); returns("SARL");
				lexicon.getAllEntries(); returns(lexicalEntries.iterator());
			}
		};
		
		DiacriticizerImpl diacriticizer = new DiacriticizerImpl();
		diacriticizer.addLexicon(lexicon);
		
		Set<String> results = diacriticizer.diacriticize("MANGEE");
		assertEquals(1, results.size());
		assertEquals("mangée", results.iterator().next());
		
		results = diacriticizer.diacriticize("mange");
		assertEquals(1, results.size());
		assertEquals("mange", results.iterator().next());

		results = diacriticizer.diacriticize("Mangé");
		assertEquals(1, results.size());
		assertEquals("mangé", results.iterator().next());

		results = diacriticizer.diacriticize("MANGE");
		assertEquals(2, results.size());
		assertTrue(results.contains("mange"));
		assertTrue(results.contains("mangé"));

		results = diacriticizer.diacriticize("MANGÉ");
		assertEquals(1, results.size());
		assertTrue(results.contains("mangé"));
		
		results = diacriticizer.diacriticize("SARL");
		assertEquals(1, results.size());
		assertEquals("SARL", results.iterator().next());
		
		results = diacriticizer.diacriticize("sarl");
		assertEquals(0, results.size());
	}

}
