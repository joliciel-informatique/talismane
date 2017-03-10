package com.joliciel.talismane.lexicon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Scanner;

import org.junit.Test;

public class LexiconFileTest {

	@Test
	public void testLexiconFileLoad() throws Exception {
		File glaffFile = new File("src/test/resources/lexicons/glaff-1.2.1_regex.txt");
		Scanner regexScanner = new Scanner(glaffFile, "UTF-8");
		RegexLexicalEntryReader reader = new RegexLexicalEntryReader(regexScanner);
		File lexFile = new File("src/test/resources/lexicons/glaff-1.2.1_letterD.txt");
		Scanner lexiconScanner = new Scanner(lexFile, "UTF-8");
		LexiconFile lexiconFile = new LexiconFile("glaffD", lexiconScanner, reader);

		lexiconFile.load();

		assertEquals("glaffD", lexiconFile.getName());

		List<LexicalEntry> lexicalEntries = lexiconFile.getEntries("dame");
		boolean foundNoun = false;
		for (LexicalEntry lexicalEntry : lexicalEntries) {
			System.out.println(lexicalEntry);
			if ("N".equals(lexicalEntry.getCategory())) {
				assertEquals("f", lexicalEntry.getGender().get(0));
				assertEquals("s", lexicalEntry.getNumber().get(0));
				foundNoun = true;
			}
		}
		assertTrue(foundNoun);
	}

}
