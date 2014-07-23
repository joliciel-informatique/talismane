package com.joliciel.talismane.tokeniser.filters;

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

public class LowercaseFilter implements TokenSequenceFilter, NeedsTalismaneSession {
	TalismaneSession talismaneSession;
	
	@Override
	public void apply(TokenSequence tokenSequence) {
		for (Token token : tokenSequence) {
			token.setText(token.getText().toLowerCase(talismaneSession.getLocale()));
		}
	}

	public TalismaneSession getTalismaneSession() {
		return talismaneSession;
	}

	public void setTalismaneSession(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
	}

	

}
