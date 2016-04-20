///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Safety Data -CFH
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
package com.joliciel.talismane.regex;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import com.joliciel.talismane.regex.bytecode.CompiledRegex;
import com.joliciel.talismane.regex.compiler.Compiler;
import com.joliciel.talismane.regex.parser.Parser;
import com.joliciel.talismane.resources.WordList;
import com.joliciel.talismane.resources.WordListFinder;

/**
 * @author Lucas Satabin
 *
 */
public class RegexTest {

	@Test
	public void testEmpty() {
		Parser parser = new Parser("");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match = compiled.findFirstIn("");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 0);

		match = compiled.findFirstIn("non empty string");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 0);

	}

	@Test
	public void testCharacters() {
		Parser parser = new Parser("abc");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match = compiled.findFirstIn("abc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("abcdef");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("yxzabcdef");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 3);
		Assert.assertEquals(match.end(), 6);

		match = compiled.findFirstIn("ab");

		Assert.assertNull(match);

	}

	@Test
	public void testDot() {
		Parser parser = new Parser("a.c");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match = compiled.findFirstIn("abc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("acc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("abdc");

		Assert.assertNull(match);
	}

	@Test
	public void testEscaped() {
		Parser parser = new Parser("a\\.c");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match = compiled.findFirstIn("a.c");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("abc");

		Assert.assertNull(match);

	}

	@Test
	public void testAlternative() {
		Parser parser = new Parser("abc|def");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match = compiled.findFirstIn("abc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("def");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("abcdef");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("defabc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("abf");

		Assert.assertNull(match);
	}

	@Test
	public void testAlternativeCapture() {
		Parser parser = new Parser("(abc)|(de)(f)|(deg)");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());

		for (int i = 0; i < 1000; i++) {
			Match match = compiled.findFirstIn("ababc");

			Assert.assertNotNull(match);
			Assert.assertEquals(match.group(1), "abc");
			Assert.assertNull(match.group(2));
			Assert.assertNull(match.group(3));
			Assert.assertNull(match.group(4));

			match = compiled.findFirstIn("abdef");

			Assert.assertNotNull(match);
			Assert.assertEquals(match.group(2), "de");
			Assert.assertEquals(match.group(3), "f");
			Assert.assertNull(match.group(1));
			Assert.assertNull(match.group(4));

			match = compiled.findFirstIn("abdeg");

			Assert.assertNotNull(match);
			Assert.assertEquals(match.group(4), "deg");
			Assert.assertNull(match.group(1));
			Assert.assertNull(match.group(2));
			Assert.assertNull(match.group(3));
		}

	}

	@Test
	public void testAlternativeCaptureNative() {
		Pattern pattern = Pattern.compile("(abc)|(de)(f)|(deg)");

		for (int i = 0; i < 1000; i++) {
			java.util.regex.Matcher match = pattern.matcher("ababc");
			match.find();

			Assert.assertNotNull(match);
			Assert.assertEquals(match.group(1), "abc");
			Assert.assertNull(match.group(2));
			Assert.assertNull(match.group(3));
			Assert.assertNull(match.group(4));

			match = pattern.matcher("abdef");
			match.find();

			Assert.assertNotNull(match);
			Assert.assertEquals(match.group(2), "de");
			Assert.assertEquals(match.group(3), "f");
			Assert.assertNull(match.group(1));
			Assert.assertNull(match.group(4));

			match = pattern.matcher("abdeg");
			match.find();

			Assert.assertNotNull(match);
			Assert.assertEquals(match.group(4), "deg");
			Assert.assertNull(match.group(1));
			Assert.assertNull(match.group(2));
			Assert.assertNull(match.group(3));
		}

	}

	@Test
	public void testStar() {
		Parser parser = new Parser("ab*c");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match = compiled.findFirstIn("abc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("ac");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 2);

		match = compiled.findFirstIn("abbbbbbbbbbbbbbbbbbbbbbbc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 25);

		match = compiled.findFirstIn("abdc");

		Assert.assertNull(match);

		match = compiled.findFirstIn("abac");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 2);
		Assert.assertEquals(match.end(), 4);
	}

	@Test
	public void testPlus() {
		Parser parser = new Parser("ab+c");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match = compiled.findFirstIn("abc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("ac");

		Assert.assertNull(match);

		match = compiled.findFirstIn("abbbbbbbbbbbbbbbbbbbbbbbc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 25);

		match = compiled.findFirstIn("abdc");

		Assert.assertNull(match);

		match = compiled.findFirstIn("abac");

		Assert.assertNull(match);
	}

	@Test
	public void testOption() {
		Parser parser = new Parser("ab?c");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match = compiled.findFirstIn("abc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("ac");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 2);

		match = compiled.findFirstIn("abbbbbbbbbbbbbbbbbbbbbbbc");

		Assert.assertNull(match);

		match = compiled.findFirstIn("abdc");

		Assert.assertNull(match);

		match = compiled.findFirstIn("abac");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 2);
		Assert.assertEquals(match.end(), 4);
	}

	@Test
	public void testCharClass() {
		Parser parser = new Parser("[a-z0-9_]");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match;
		for (char c = 'a'; c <= 'z'; c++) {
			match = compiled.findFirstIn("" + c);

			Assert.assertNotNull(match);
			Assert.assertEquals(match.start(), 0);
			Assert.assertEquals(match.end(), 1);
		}
		for (char c = '0'; c <= '9'; c++) {
			match = compiled.findFirstIn("" + c);

			Assert.assertNotNull(match);
			Assert.assertEquals(match.start(), 0);
			Assert.assertEquals(match.end(), 1);
		}

		match = compiled.findFirstIn("_");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		for (char c = 'A'; c <= 'Z'; c++) {
			match = compiled.findFirstIn("" + c);

			Assert.assertNull(match);
		}
	}

	@Test
	public void testBoundedRepetitionMinMax() {
		Parser parser = new Parser("ab{2,6}c");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match = compiled.findFirstIn("abbc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 4);

		match = compiled.findFirstIn("abbbc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 5);

		match = compiled.findFirstIn("abbbbbbc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 8);

		match = compiled.findFirstIn("abbbbbbbc");

		Assert.assertNull(match);

		match = compiled.findFirstIn("abc");

		Assert.assertNull(match);

		match = compiled.findFirstIn("ac");

		Assert.assertNull(match);
	}

	@Test
	public void testBoundedRepetitionMin() {
		Parser parser = new Parser("ab{2,}c");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match = compiled.findFirstIn("abbc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 4);

		match = compiled.findFirstIn("abbbc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 5);

		match = compiled.findFirstIn("abbbbbbc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 8);

		match = compiled.findFirstIn("abbbbbbbc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 9);

		match = compiled.findFirstIn("abc");

		Assert.assertNull(match);

		match = compiled.findFirstIn("ac");

		Assert.assertNull(match);
	}

	@Test
	public void testBoundedRepetitionMax() {
		Parser parser = new Parser("ab{,6}c");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match = compiled.findFirstIn("abbc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 4);

		match = compiled.findFirstIn("abbbc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 5);

		match = compiled.findFirstIn("abbbbbbc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 8);

		match = compiled.findFirstIn("abbbbbbbc");

		Assert.assertNull(match);

		match = compiled.findFirstIn("abc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("ac");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 2);
	}

	@Test
	public void testBoundedRepetitionExact() {
		Parser parser = new Parser("ab{6}c");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match = compiled.findFirstIn("abbc");

		Assert.assertNull(match);

		match = compiled.findFirstIn("abbbc");

		Assert.assertNull(match);

		match = compiled.findFirstIn("abbbbbbc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 8);

		match = compiled.findFirstIn("abbbbbbbc");

		Assert.assertNull(match);

		match = compiled.findFirstIn("abc");

		Assert.assertNull(match);

		match = compiled.findFirstIn("ac");

		Assert.assertNull(match);
	}

	@Test
	public void testNumberAlias() {
		Parser parser = new Parser("\\d");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match;
		for (char c = 'a'; c <= 'z'; c++) {
			match = compiled.findFirstIn("" + c);

			Assert.assertNull(match);
		}
		for (char c = '0'; c <= '9'; c++) {
			match = compiled.findFirstIn("" + c);

			Assert.assertNotNull(match);
			Assert.assertEquals(match.start(), 0);
			Assert.assertEquals(match.end(), 1);
		}
	}

	@Test
	public void testNumberWordCharacterAlias() {
		Parser parser = new Parser("\\w");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match;

		for (char c = 'a'; c <= 'z'; c++) {
			match = compiled.findFirstIn("" + c);

			Assert.assertNotNull(match);

			Assert.assertEquals(match.start(), 0);
			Assert.assertEquals(match.end(), 1);
		}

		for (char c = 'A'; c <= 'Z'; c++) {
			match = compiled.findFirstIn("" + c);

			Assert.assertNotNull(match);

			Assert.assertEquals(match.start(), 0);
			Assert.assertEquals(match.end(), 1);
		}

		for (char c = '0'; c <= '9'; c++) {
			match = compiled.findFirstIn("" + c);

			Assert.assertNotNull(match);
			Assert.assertEquals(match.start(), 0);
			Assert.assertEquals(match.end(), 1);
		}

		match = compiled.findFirstIn("_");
		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("-");
		Assert.assertNull(match);
	}

	@Test
	public void testSpaceAlias() {
		Parser parser = new Parser("\\s");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match;

		match = compiled.findFirstIn(" ");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("\t");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("\n");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("\r");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("\f");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("\u000b");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("");

		Assert.assertNull(match);

		match = compiled.findFirstIn("a");

		Assert.assertNull(match);

	}

	@Test
	public void testCapture1() {
		Parser parser = new Parser("a(b*)c");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match = compiled.findFirstIn("abbc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 4);
		Assert.assertEquals(match.group(1), "bb");

		match = compiled.findFirstIn("ac");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 2);
		Assert.assertEquals(match.group(1), "");
	}

	@Test
	public void testCapture2() {
		Parser parser = new Parser("a(b|d|e)c");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match = compiled.findFirstIn("abc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);
		Assert.assertEquals(match.group(1), "b");

		match = compiled.findFirstIn("adc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);
		Assert.assertEquals(match.group(1), "d");

		match = compiled.findFirstIn("aec");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);
		Assert.assertEquals(match.group(1), "e");

		match = compiled.findFirstIn("acc");

		Assert.assertNull(match);
	}

	@Test
	public void testCapture3() {
		Parser parser = new Parser("a([\\d\\s](b|d|e))c");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match = compiled.findFirstIn("a5bc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 4);
		Assert.assertEquals(match.group(1), "5b");
		Assert.assertEquals(match.group(2), "b");

	}

	@Test
	public void testNamedClass() {
		CompiledRegex compiled = new Compiler(null).compile("\\p{Punct}");
		Match match = compiled.findFirstIn("a, bc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 1);
		Assert.assertEquals(match.end(), 2);
	}

	@Test
	public void testWordBoundary1() {
		CompiledRegex compiled = new Compiler(null).compile("\\btest");
		Match match = compiled.findFirstIn("test");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 4);

		match = compiled.findFirstIn("a test");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 2);
		Assert.assertEquals(match.end(), 6);

		match = compiled.findFirstIn("ttest");

		Assert.assertNull(match);

	}

	@Test
	public void testWordBoundary2() {
		CompiledRegex compiled = new Compiler(null).compile("test\\b");
		Match match = compiled.findFirstIn("test");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 4);

		match = compiled.findFirstIn("a test");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 2);
		Assert.assertEquals(match.end(), 6);

		match = compiled.findFirstIn("test 2");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 4);

		match = compiled.findFirstIn("a test 2");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 2);
		Assert.assertEquals(match.end(), 6);

		match = compiled.findFirstIn("testt");

		Assert.assertNull(match);

	}

	@Test
	public void testWordBoundary3() {
		CompiledRegex compiled = new Compiler(null).compile("\\btest\\b");
		Match match = compiled.findFirstIn("test");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 4);

		match = compiled.findFirstIn("test 2");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 4);

		match = compiled.findFirstIn("testt");

		Assert.assertNull(match);

	}

	/**
	 * Words are any alphanumeric character.
	 */
	@Test
	public void testWordBoundary4() {
		Parser parser = new Parser("\\b8\\b");
		CompiledRegex compiled = new Compiler(null).compile(parser.parse());
		Match match = compiled.findFirstIn("8");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("a 8");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 2);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("8 a");
		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("a 8 a");
		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 2);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("8a");
		Assert.assertNull(match);

		match = compiled.findFirstIn("a8");
		Assert.assertNull(match);

	}

	@Test
	public void testCaseSensitivity1() {
		CompiledRegex compiled = new Compiler(null).compile("a");
		Match match = compiled.findFirstIn("a");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("A");

		Assert.assertNull(match);
	}

	@Test
	public void testCaseSensitivity2() {
		CompiledRegex compiled = new Compiler(Flags.CASE_INSENSITIVE, null).compile("a");
		Match match = compiled.findFirstIn("a");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("A");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("à");

		Assert.assertNull(match);

		match = compiled.findFirstIn("À");

		Assert.assertNull(match);
	}

	@Test
	public void testCaseSensitivity3() {
		CompiledRegex compiled = new Compiler(Flags.CASE_INSENSITIVE, null).compile("à");
		Match match = compiled.findFirstIn("à");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("À");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("A");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("a");

		Assert.assertNull(match);
	}

	@Test
	public void testDiacriticsSensitivity1() {
		CompiledRegex compiled = new Compiler(null).compile("é");
		Match match = compiled.findFirstIn("é");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("e");

		Assert.assertNull(match);

		match = compiled.findFirstIn("É");

		Assert.assertNull(match);

		match = compiled.findFirstIn("E");

		Assert.assertNull(match);
	}

	@Test
	public void testDiacriticsSensitivity2() {
		CompiledRegex compiled = new Compiler(Flags.DIACRITICS_INSENSITIVE, null).compile("é");
		Match match = compiled.findFirstIn("é");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("e");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("É");

		Assert.assertNull(match);

		match = compiled.findFirstIn("E");

		Assert.assertNull(match);
	}

	@Test
	public void testDiacriticsSensitivity3() {
		CompiledRegex compiled = new Compiler(Flags.DIACRITICS_INSENSITIVE | Flags.CASE_INSENSITIVE, null).compile("é");
		Match match = compiled.findFirstIn("é");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("e");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("É");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);

		match = compiled.findFirstIn("E");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 1);
	}

	@Test
	public void testWordList() {

		WordListFinder wordListFinder = new WordListFinder();
		wordListFinder.addWordList(new WordList("TestList", Arrays.asList("abc", "def")));

		CompiledRegex compiled = new Compiler(wordListFinder).compile("\\p{WordList(TestList)}");
		Match match = compiled.findFirstIn("abc");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("def");

		Assert.assertNotNull(match);
		Assert.assertEquals(match.start(), 0);
		Assert.assertEquals(match.end(), 3);

		match = compiled.findFirstIn("abd");

		Assert.assertNull(match);

	}

}
