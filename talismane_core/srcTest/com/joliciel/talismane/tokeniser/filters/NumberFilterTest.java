package com.joliciel.talismane.tokeniser.filters;

import static org.junit.Assert.*;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.junit.Test;

import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTaggerLexiconService;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokeniserService;

public class NumberFilterTest {

	@Test
	public void testApply(@NonStrict final PosTaggerLexiconService lexiconService) {
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance();
		TokeniserService tokeniserService = locator.getTokeniserServiceLocator().getTokeniserService();
		
		new NonStrictExpectations() {
			PosTag posTagCLS;
			{
				Set<PosTag> posTags = new TreeSet<PosTag>();
				posTags.add(posTagCLS);
				posTagCLS.getCode(); returns("CLS");
				lexiconService.findPossiblePosTags("je"); returns(posTags);
			}
		};
		
		TalismaneSession.setLocale(Locale.FRENCH);
		
		NumberFilter filter = new NumberFilter();
		TokenSequence tokenSequence = tokeniserService.getTokenSequence("23 345 23,4 21.345 21.345,57 1789 1.789 3,45e-6 +9,34 4e12", Pattern.compile(" "));
		filter.apply(tokenSequence);
		
		int i = 0;
		
		assertEquals("23", tokenSequence.get(i).getOriginalText());
		assertEquals("31", tokenSequence.get(i).getText());
		
		assertEquals("345", tokenSequence.get(++i).getOriginalText());
		assertEquals("999", tokenSequence.get(i).getText());
		
		assertEquals("23,4", tokenSequence.get(++i).getOriginalText());
		assertEquals("9,99", tokenSequence.get(i).getText());
		
		assertEquals("21.345", tokenSequence.get(++i).getOriginalText());
		assertEquals("999", tokenSequence.get(i).getText());
		
		assertEquals("21.345,57", tokenSequence.get(++i).getOriginalText());
		assertEquals("9,99", tokenSequence.get(i).getText());
		
		assertEquals("1789", tokenSequence.get(++i).getOriginalText());
		assertEquals("1999", tokenSequence.get(i).getText());
		
		assertEquals("1.789", tokenSequence.get(++i).getOriginalText());
		assertEquals("999", tokenSequence.get(i).getText());
		
		assertEquals("3,45e-6", tokenSequence.get(++i).getOriginalText());
		assertEquals("9,99", tokenSequence.get(i).getText());
		
		assertEquals("+9,34", tokenSequence.get(++i).getOriginalText());
		assertEquals("9,99", tokenSequence.get(i).getText());

		assertEquals("4e12", tokenSequence.get(++i).getOriginalText());
		assertEquals("9,99", tokenSequence.get(i).getText());
	}
}
