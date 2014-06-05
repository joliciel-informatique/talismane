package com.joliciel.talismane.fr.tokeniser.filters;

import static org.junit.Assert.*;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.junit.Test;

import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.fr.tokeniser.filters.UpperCaseSeriesFrenchFilter;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokeniserService;

public class UpperCaseSeriesFilterTest {

	@Test
	public void testApply(@NonStrict final PosTaggerLexicon lexiconService, @NonStrict final Sentence sentence) {
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance("");
		TokeniserService tokeniserService = locator.getTokeniserServiceLocator().getTokeniserService();
		TalismaneSession talismaneSession = locator.getTalismaneService().getTalismaneSession();
		talismaneSession.setLexicon(lexiconService);
		
		new NonStrictExpectations() {
			PosTag posTagNC;
			{
				sentence.getText(); returns("Le PERE NOEL ne viendra pas.");

				Set<PosTag> posTags = new TreeSet<PosTag>();
				posTags.add(posTagNC);
				posTagNC.getCode(); returns("NC");
				lexiconService.findPossiblePosTags("père"); returns(posTags);
				lexiconService.findPossiblePosTags("noël"); returns(posTags);
			}
		};
		
		UpperCaseSeriesFrenchFilter filter = new UpperCaseSeriesFrenchFilter();
		filter.setTalismaneSession(talismaneSession);
		TokenSequence tokenSequence = tokeniserService.getTokenSequence(sentence, Pattern.compile("[ .]"));
		filter.apply(tokenSequence);
		
		assertEquals("PERE", tokenSequence.get(1).getOriginalText());
		assertEquals("père", tokenSequence.get(1).getText());
		assertEquals("NOEL", tokenSequence.get(2).getOriginalText());
		assertEquals("noël", tokenSequence.get(2).getText());
	}
	

	@Test
	public void testApplyWordNotInLexicon(@NonStrict final PosTaggerLexicon lexiconService, @NonStrict final Sentence sentence) {
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance("");
		TokeniserService tokeniserService = locator.getTokeniserServiceLocator().getTokeniserService();
		TalismaneSession talismaneSession = locator.getTalismaneService().getTalismaneSession();
		talismaneSession.setLexicon(lexiconService);
		
		new NonStrictExpectations() {
			PosTag posTagNC;
			{
				sentence.getText(); returns("Le PERE NOEL ne viendra pas.");

				Set<PosTag> posTags = new TreeSet<PosTag>();
				posTags.add(posTagNC);
				posTagNC.getCode(); returns("NC");
				lexiconService.findPossiblePosTags("père"); returns(posTags);
			}
		};
		
		UpperCaseSeriesFrenchFilter filter = new UpperCaseSeriesFrenchFilter();
		filter.setTalismaneSession(talismaneSession);
		TokenSequence tokenSequence = tokeniserService.getTokenSequence(sentence, Pattern.compile("[ .]"));
		filter.apply(tokenSequence);
		
		assertEquals("PERE", tokenSequence.get(1).getOriginalText());
		assertEquals("père", tokenSequence.get(1).getText());
		assertEquals("NOEL", tokenSequence.get(2).getOriginalText());
		assertEquals("Noel", tokenSequence.get(2).getText());
	}
	

	@Test
	public void testApplyNumberInMiddle(@NonStrict final PosTaggerLexicon lexiconService, @NonStrict final Sentence sentence) {
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance("");
		TokeniserService tokeniserService = locator.getTokeniserServiceLocator().getTokeniserService();
		TalismaneSession talismaneSession = locator.getTalismaneService().getTalismaneSession();
		talismaneSession.setLexicon(lexiconService);
		
		new NonStrictExpectations() {
			PosTag posTagNC;
			{
				sentence.getText(); returns("Le PERE 2 NOEL ne viendra pas.");

				Set<PosTag> posTags = new TreeSet<PosTag>();
				posTags.add(posTagNC);
				posTagNC.getCode(); returns("NC");
				lexiconService.findPossiblePosTags("père"); returns(posTags);
				lexiconService.findPossiblePosTags("noël"); returns(posTags);
			}
		};
		
		UpperCaseSeriesFrenchFilter filter = new UpperCaseSeriesFrenchFilter();
		filter.setTalismaneSession(talismaneSession);
		TokenSequence tokenSequence = tokeniserService.getTokenSequence(sentence, Pattern.compile("[ .]"));
		filter.apply(tokenSequence);
		
		assertEquals("PERE", tokenSequence.get(1).getOriginalText());
		assertEquals("père", tokenSequence.get(1).getText());
		assertEquals("NOEL", tokenSequence.get(3).getOriginalText());
		assertEquals("noël", tokenSequence.get(3).getText());
	}
	

	@Test
	public void testApplyLowercaseInMiddle(@NonStrict final PosTaggerLexicon lexiconService, @NonStrict final Sentence sentence) {
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance("");
		TokeniserService tokeniserService = locator.getTokeniserServiceLocator().getTokeniserService();
		TalismaneSession talismaneSession = locator.getTalismaneService().getTalismaneSession();
		talismaneSession.setLexicon(lexiconService);
		
		new NonStrictExpectations() {
			PosTag posTagNC;
			{
				sentence.getText(); returns("Le PERE de NOEL ne viendra pas.");

				Set<PosTag> posTags = new TreeSet<PosTag>();
				posTags.add(posTagNC);
				posTagNC.getCode(); returns("NC");
				lexiconService.findPossiblePosTags("père"); returns(posTags);
				lexiconService.findPossiblePosTags("noël"); returns(posTags);
			}
		};
		
		UpperCaseSeriesFrenchFilter filter = new UpperCaseSeriesFrenchFilter();
		filter.setTalismaneSession(talismaneSession);
		TokenSequence tokenSequence = tokeniserService.getTokenSequence(sentence, Pattern.compile("[ .]"));
		filter.apply(tokenSequence);
		
		assertEquals("PERE", tokenSequence.get(1).getOriginalText());
		assertEquals("PERE", tokenSequence.get(1).getText());
		assertEquals("NOEL", tokenSequence.get(3).getOriginalText());
		assertEquals("NOEL", tokenSequence.get(3).getText());
	}
	
	@Test
	public void testApplyWithSARL(@NonStrict final PosTaggerLexicon lexiconService, @NonStrict final Sentence sentence) {
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance("");
		TokeniserService tokeniserService = locator.getTokeniserServiceLocator().getTokeniserService();
		TalismaneSession talismaneSession = locator.getTalismaneService().getTalismaneSession();
		talismaneSession.setLexicon(lexiconService);
		
		new NonStrictExpectations() {
			PosTag posTagNC;
			{
				sentence.getText(); returns("L'entreprise CPE SARL ne viendra pas.");

				Set<PosTag> posTags = new TreeSet<PosTag>();
				posTags.add(posTagNC);
				posTagNC.getCode(); returns("NC");
				lexiconService.findPossiblePosTags("CPE"); returns(posTags);
				lexiconService.findPossiblePosTags("SARL"); returns(posTags);
			}
		};
		
		UpperCaseSeriesFrenchFilter filter = new UpperCaseSeriesFrenchFilter();
		filter.setTalismaneSession(talismaneSession);
		TokenSequence tokenSequence = tokeniserService.getTokenSequence(sentence, Pattern.compile("[ .]"));
		filter.apply(tokenSequence);
		
		assertEquals("CPE", tokenSequence.get(1).getOriginalText());
		assertEquals("CPE", tokenSequence.get(1).getText());
		assertEquals("SARL", tokenSequence.get(2).getOriginalText());
		assertEquals("SARL", tokenSequence.get(2).getText());
	}
}
