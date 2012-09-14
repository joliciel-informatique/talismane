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

public class LowercaseFirstWordFrenchFilterTest {

	@Test
	public void testApply(@NonStrict final PosTaggerLexiconService lexiconService) {
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance();
		TokeniserService tokeniserService = locator.getTokeniserServiceLocator().getTokeniserService();
		TalismaneSession.setLexiconService(lexiconService);
		new NonStrictExpectations() {
			PosTag posTagCLS;
			{
				Set<PosTag> posTags = new TreeSet<PosTag>();
				posTags.add(posTagCLS);
				posTagCLS.getCode(); returns("CLS");
				lexiconService.findPossiblePosTags("je"); returns(posTags);
			}
		};
		
		LowercaseFirstWordFrenchFilter filter = new LowercaseFirstWordFrenchFilter();
		TokenSequence tokenSequence = tokeniserService.getTokenSequence("Je ne sais pas", Pattern.compile(" "));
		filter.apply(tokenSequence);
		
		assertEquals("Je", tokenSequence.get(0).getOriginalText());
		assertEquals("je", tokenSequence.get(0).getText());
	}


	@Test
	public void testApplyNotInLexicon(@NonStrict final PosTaggerLexiconService lexiconService) {
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance();
		TokeniserService tokeniserService = locator.getTokeniserServiceLocator().getTokeniserService();
		TalismaneSession.setLexiconService(lexiconService);
		
		new NonStrictExpectations() {
			{
				Set<PosTag> posTags = new TreeSet<PosTag>();
				lexiconService.findPossiblePosTags("tF"); returns(posTags);
			}
		};
		
		LowercaseFirstWordFrenchFilter filter = new LowercaseFirstWordFrenchFilter();
		TokenSequence tokenSequence = tokeniserService.getTokenSequence("TF 1 va", Pattern.compile(" "));
		filter.apply(tokenSequence);
		
		assertEquals("TF", tokenSequence.get(0).getOriginalText());
		assertEquals("TF", tokenSequence.get(0).getText());
	}
}
