package com.joliciel.talismane.lexicon;

import com.joliciel.talismane.TalismaneTest;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DiacriticizerTest extends TalismaneTest {

  @Test
  public void testDiacriticize() {

    final Lexicon lexicon = mock(Lexicon.class);
    final LexicalEntry l1 = mock(LexicalEntry.class);
    final LexicalEntry l2 = mock(LexicalEntry.class);
    final LexicalEntry l3 = mock(LexicalEntry.class);
    final LexicalEntry l4 = mock(LexicalEntry.class);
    final LexicalEntry l5 = mock(LexicalEntry.class);
    final LexicalEntry l6 = mock(LexicalEntry.class);
    final LexicalEntry l7 = mock(LexicalEntry.class);
    final LexicalEntry l8 = mock(LexicalEntry.class);
    final LexicalEntry l9 = mock(LexicalEntry.class);
    List<LexicalEntry> lexicalEntries = Arrays.asList(l1, l2, l3, l4, l5, l6, l7, l8, l9);
    

    when(l1.getWord()).thenReturn("mangé");
    when(l2.getWord()).thenReturn("mange");
    when(l3.getWord()).thenReturn("mangée");
    when(l4.getWord()).thenReturn("SARL");
    when(l5.getWord()).thenReturn("bala");
    when(l6.getWord()).thenReturn("bàlà");
    when(l7.getWord()).thenReturn("bàla");
    when(l8.getWord()).thenReturn("a");
    when(l9.getWord()).thenReturn("à");
    
    when(lexicon.getAllEntries()).thenReturn(lexicalEntries.iterator());

    Diacriticizer diacriticizer = new Diacriticizer();
    diacriticizer.setLocale(Locale.FRENCH);

    Map<String, String> lowercasePreferences = new HashMap<>();
    lowercasePreferences.put("A", "à");

    diacriticizer.setLowercasePreferences(lowercasePreferences);

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

  @Test
  public void testUnattachedCombiningDiacritic() {
    final Lexicon lexicon = mock(Lexicon.class);
    final LexicalEntry l1 = mock(LexicalEntry.class);

    List<LexicalEntry> lexicalEntries = Arrays.asList(l1);

    when(l1.getWord()).thenReturn("mangé");
    
    when(lexicon.getAllEntries()).thenReturn(lexicalEntries.iterator());

    Diacriticizer diacriticizer = new Diacriticizer();
    diacriticizer.setLocale(Locale.FRENCH);
    diacriticizer.addLexicon(lexicon);

    Set<String> results = diacriticizer.diacriticize("mangé\u0301");
    assertEquals(1, results.size());
    assertEquals("mangé", results.iterator().next());
  }

}
