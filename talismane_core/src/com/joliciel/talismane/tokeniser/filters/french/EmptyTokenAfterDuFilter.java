///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.talismane.tokeniser.filters.french;

import java.util.List;
import java.util.ArrayList;

import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;

/**
 * Always insert an empty token after "du", "des", "au" or "aux".
 * This will either get a proper postag (P+D), or will get a null postag (D+null).
 * @author Assaf Urieli
 *
 */
public class EmptyTokenAfterDuFilter implements TokenFilter {
	private boolean createIfMissing = true;
	
	public EmptyTokenAfterDuFilter() {
		super();
	}

	public EmptyTokenAfterDuFilter(boolean createIfMissing) {
		super();
		this.createIfMissing = createIfMissing;
	}

	@Override
	public void apply(TokenSequence tokenSequence) {
		List<Token> tokensToAddEmpties = new ArrayList<Token>();
		for (Token token : tokenSequence) {
			if (token.getText().equals("du")||token.getText().equals("des")||token.getText().equals("au")||token.getText().equals("aux")
					||token.getText().endsWith(" du")||token.getText().endsWith(" des")||token.getText().endsWith(" au")||token.getText().endsWith(" aux")) {
				tokensToAddEmpties.add(token);
			}
		}
		
		for (Token token : tokensToAddEmpties) {
			Token nextToken = null;
			Token emptyToken = null;
			if (token.getIndexWithWhiteSpace()+1 < tokenSequence.listWithWhiteSpace().size())
				nextToken = tokenSequence.listWithWhiteSpace().get(token.getIndexWithWhiteSpace()+1);
			
			if (nextToken!=null && nextToken.getOriginalText().length()==0)
				emptyToken = nextToken;

			if (emptyToken==null && createIfMissing)
				emptyToken = tokenSequence.addEmptyToken(token.getEndIndex());
			
			if (emptyToken!=null) {
				String text = "";
				if (token.getText().endsWith("du"))
					text = "du";
				else if (token.getText().endsWith("des"))
					text = "des";
				else if (token.getText().endsWith("au"))
					text = "au";
				else if (token.getText().endsWith("aux"))
					text = "aux";
				
				emptyToken.setText("[[" + text+ "]]");
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
