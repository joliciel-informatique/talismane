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
import com.joliciel.talismane.fr.tokeniser.filters.LowercaseFirstWordFrenchFilter;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokeniserService;

public class LowercaseFirstWordFrenchFilterTest {

	@Test
	public void testApply(@NonStrict final PosTaggerLexicon lexiconService, @NonStrict final Sentence sentence) {
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance();
		TokeniserService tokeniserService = locator.getTokeniserServiceLocator().getTokeniserService();
		TalismaneSession.setLexicon(lexiconService);
		new NonStrictExpectations() {
			PosTag posTagCLS;
			{
				sentence.getText(); returns("Je ne sais pas");

				Set<PosTag> posTags = new TreeSet<PosTag>();
				posTags.add(posTagCLS);
				posTagCLS.getCode(); returns("CLS");
				lexiconService.findPossiblePosTags("je"); returns(posTags);
			}
		};
		
		LowercaseFirstWordFrenchFilter filter = new LowercaseFirstWordFrenchFilter();
		TokenSequence tokenSequence = tokeniserService.getTokenSequence(sentence, Pattern.compile(" "));
		filter.apply(tokenSequence);
		
		assertEquals("Je", tokenSequence.get(0).getOriginalText());
		assertEquals("je", tokenSequence.get(0).getText());
	}


	@Test
	public void testApplyNotInLexicon(@NonStrict final PosTaggerLexicon lexiconService, @NonStrict final Sentence sentence) {
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance();
		TokeniserService tokeniserService = locator.getTokeniserServiceLocator().getTokeniserService();
		TalismaneSession.setLexicon(lexiconService);
		
		new NonStrictExpectations() {
			{
				sentence.getText(); returns("TF 1 va");
				
				Set<PosTag> posTags = new TreeSet<PosTag>();
				lexiconService.findPossiblePosTags("tF"); returns(posTags);
			}
		};
		
		LowercaseFirstWordFrenchFilter filter = new LowercaseFirstWordFrenchFilter();
		TokenSequence tokenSequence = tokeniserService.getTokenSequence(sentence, Pattern.compile(" "));
		filter.apply(tokenSequence);
		
		assertEquals("TF", tokenSequence.get(0).getOriginalText());
		assertEquals("TF", tokenSequence.get(0).getText());
	}
}
