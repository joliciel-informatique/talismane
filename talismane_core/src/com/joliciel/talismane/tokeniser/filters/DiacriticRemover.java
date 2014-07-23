package com.joliciel.talismane.tokeniser.filters;

import java.text.Normalizer;
import java.text.Normalizer.Form;

import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

public class DiacriticRemover implements TokenSequenceFilter {

	@Override
	public void apply(TokenSequence tokenSequence) {
		for (Token token : tokenSequence) {
			token.setText(Normalizer.normalize(token.getText(), Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", ""));
		}
	}

}
