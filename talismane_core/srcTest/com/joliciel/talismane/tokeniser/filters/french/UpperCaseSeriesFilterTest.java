package com.joliciel.talismane.tokeniser.filters.french;

import static org.junit.Assert.*;

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

public class UpperCaseSeriesFilterTest {

	@Test
	public void testApply(@NonStrict final PosTaggerLexiconService lexiconService) {
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance();
		TokeniserService tokeniserService = locator.getTokeniserServiceLocator().getTokeniserService();
		TalismaneSession.setLexiconService(lexiconService);
		
		new NonStrictExpectations() {
			PosTag posTagNC;
			{
				Set<PosTag> posTags = new TreeSet<PosTag>();
				posTags.add(posTagNC);
				posTagNC.getCode(); returns("NC");
				lexiconService.findPossiblePosTags("père"); returns(posTags);
				lexiconService.findPossiblePosTags("noël"); returns(posTags);
			}
		};
		
		UpperCaseSeriesFilter filter = new UpperCaseSeriesFilter();
		TokenSequence tokenSequence = tokeniserService.getTokenSequence("Le PERE NOEL ne viendra pas.", Pattern.compile("[ .]"));
		filter.apply(tokenSequence);
		
		assertEquals("PERE", tokenSequence.get(1).getOriginalText());
		assertEquals("père", tokenSequence.get(1).getText());
		assertEquals("NOEL", tokenSequence.get(2).getOriginalText());
		assertEquals("noël", tokenSequence.get(2).getText());
	}
	

	@Test
	public void testApplyWordNotInLexicon(@NonStrict final PosTaggerLexiconService lexiconService) {
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance();
		TokeniserService tokeniserService = locator.getTokeniserServiceLocator().getTokeniserService();
		TalismaneSession.setLexiconService(lexiconService);
		
		new NonStrictExpectations() {
			PosTag posTagNC;
			{
				Set<PosTag> posTags = new TreeSet<PosTag>();
				posTags.add(posTagNC);
				posTagNC.getCode(); returns("NC");
				lexiconService.findPossiblePosTags("père"); returns(posTags);
			}
		};
		
		UpperCaseSeriesFilter filter = new UpperCaseSeriesFilter();
		TokenSequence tokenSequence = tokeniserService.getTokenSequence("Le PERE NOEL ne viendra pas.", Pattern.compile("[ .]"));
		filter.apply(tokenSequence);
		
		assertEquals("PERE", tokenSequence.get(1).getOriginalText());
		assertEquals("père", tokenSequence.get(1).getText());
		assertEquals("NOEL", tokenSequence.get(2).getOriginalText());
		assertEquals("Noel", tokenSequence.get(2).getText());
	}
	

	@Test
	public void testApplyNumberInMiddle(@NonStrict final PosTaggerLexiconService lexiconService) {
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance();
		TokeniserService tokeniserService = locator.getTokeniserServiceLocator().getTokeniserService();
		TalismaneSession.setLexiconService(lexiconService);
		
		new NonStrictExpectations() {
			PosTag posTagNC;
			{
				Set<PosTag> posTags = new TreeSet<PosTag>();
				posTags.add(posTagNC);
				posTagNC.getCode(); returns("NC");
				lexiconService.findPossiblePosTags("père"); returns(posTags);
				lexiconService.findPossiblePosTags("noël"); returns(posTags);
			}
		};
		
		UpperCaseSeriesFilter filter = new UpperCaseSeriesFilter();
		TokenSequence tokenSequence = tokeniserService.getTokenSequence("Le PERE 2 NOEL ne viendra pas.", Pattern.compile("[ .]"));
		filter.apply(tokenSequence);
		
		assertEquals("PERE", tokenSequence.get(1).getOriginalText());
		assertEquals("père", tokenSequence.get(1).getText());
		assertEquals("NOEL", tokenSequence.get(3).getOriginalText());
		assertEquals("noël", tokenSequence.get(3).getText());
	}
	

	@Test
	public void testApplyLowercaseInMiddle(@NonStrict final PosTaggerLexiconService lexiconService) {
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance();
		TokeniserService tokeniserService = locator.getTokeniserServiceLocator().getTokeniserService();
		TalismaneSession.setLexiconService(lexiconService);
		
		new NonStrictExpectations() {
			PosTag posTagNC;
			{
				Set<PosTag> posTags = new TreeSet<PosTag>();
				posTags.add(posTagNC);
				posTagNC.getCode(); returns("NC");
				lexiconService.findPossiblePosTags("père"); returns(posTags);
				lexiconService.findPossiblePosTags("noël"); returns(posTags);
			}
		};
		
		UpperCaseSeriesFilter filter = new UpperCaseSeriesFilter();
		TokenSequence tokenSequence = tokeniserService.getTokenSequence("Le PERE de NOEL ne viendra pas.", Pattern.compile("[ .]"));
		filter.apply(tokenSequence);
		
		assertEquals("PERE", tokenSequence.get(1).getOriginalText());
		assertEquals("PERE", tokenSequence.get(1).getText());
		assertEquals("NOEL", tokenSequence.get(3).getOriginalText());
		assertEquals("NOEL", tokenSequence.get(3).getText());
	}
	
	@Test
	public void testApplyWithSARL(@NonStrict final PosTaggerLexiconService lexiconService) {
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance();
		TokeniserService tokeniserService = locator.getTokeniserServiceLocator().getTokeniserService();
		TalismaneSession.setLexiconService(lexiconService);
		
		new NonStrictExpectations() {
			PosTag posTagNC;
			{
				Set<PosTag> posTags = new TreeSet<PosTag>();
				posTags.add(posTagNC);
				posTagNC.getCode(); returns("NC");
				lexiconService.findPossiblePosTags("CPE"); returns(posTags);
				lexiconService.findPossiblePosTags("SARL"); returns(posTags);
			}
		};
		
		UpperCaseSeriesFilter filter = new UpperCaseSeriesFilter();
		TokenSequence tokenSequence = tokeniserService.getTokenSequence("L'entreprise CPE SARL ne viendra pas.", Pattern.compile("[ .]"));
		filter.apply(tokenSequence);
		
		assertEquals("CPE", tokenSequence.get(1).getOriginalText());
		assertEquals("CPE", tokenSequence.get(1).getText());
		assertEquals("SARL", tokenSequence.get(2).getOriginalText());
		assertEquals("SARL", tokenSequence.get(2).getText());
	}
}
