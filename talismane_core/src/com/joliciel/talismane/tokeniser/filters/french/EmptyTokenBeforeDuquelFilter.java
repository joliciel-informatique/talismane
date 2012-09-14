package com.joliciel.talismane.tokeniser.filters.french;

import java.util.List;
import java.util.ArrayList;

import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;

/**
 * Always insert an empty token before "auquel", "duquel", "auxquels", "desquels", "auxquelles", "desquelles".
 * The surface text for the two tokens will become "à/de" + "lequel/lesquels/lesquelles".
 * @author Assaf Urieli
 *
 */
public class EmptyTokenBeforeDuquelFilter implements TokenFilter {
	private boolean createIfMissing = true;

	public EmptyTokenBeforeDuquelFilter() {
		super();
	}

	public EmptyTokenBeforeDuquelFilter(boolean createIfMissing) {
		super();
		this.createIfMissing = createIfMissing;
	}

	@Override
	public void apply(TokenSequence tokenSequence) {
		List<Token> tokensToAddEmpties = new ArrayList<Token>();
		for (Token token : tokenSequence) {
			if (token.getText().equals("duquel")
					||token.getText().equals("auquel")
					||token.getText().equals("desquels")
					||token.getText().equals("auxquels")
					||token.getText().equals("desquelles")
					||token.getText().equals("auxquelles")
				) {
				tokensToAddEmpties.add(token);
			}
		}
		
		for (Token token : tokensToAddEmpties) {
			Token previousToken = null;
			Token emptyToken = null;
			
			//TODO: in the case of "auquel", empty token is placed before space, and not after space
			if (token.getIndexWithWhiteSpace()-1 > 0)
				previousToken = tokenSequence.listWithWhiteSpace().get(token.getIndexWithWhiteSpace()-1);
			
			if (previousToken!=null && previousToken.getOriginalText().length()==0)
				emptyToken = previousToken;
				
			if (emptyToken==null && createIfMissing)
				emptyToken = tokenSequence.addEmptyToken(token.getStartIndex());
			
			if (emptyToken!=null) {
				if (token.getText().equals("duquel")) {
					token.setText("lequel");
					emptyToken.setText("de");
				} else if (token.getText().equals("auquel")) {
					token.setText("lequel");
					emptyToken.setText("à");
				} else if (token.getText().equals("desquels")) {
					token.setText("lesquels");
					emptyToken.setText("de");
				} else if (token.getText().equals("auxquels")) {
					token.setText("lesquels");
					emptyToken.setText("à");
				} else if (token.getText().equals("desquelles")) {
					token.setText("lesquelles");
					emptyToken.setText("de");
				} else if (token.getText().equals("auxquelles")) {
					token.setText("lesquelles");
					emptyToken.setText("à");
				}
			}
		}
		tokenSequence.finalise();
	}

	public boolean isCreateIfMissing() {
		return createIfMissing;
	}

	public void setCreateIfMissing(boolean createIfMissing) {
		this.createIfMissing = createIfMissing;
	}

}
