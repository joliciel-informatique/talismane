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
package com.joliciel.talismane.posTagger;

import java.util.Set;

import com.joliciel.talismane.tokeniser.TaggedTokenImpl;
import com.joliciel.talismane.tokeniser.Token;

class PosTaggedTokenImpl extends TaggedTokenImpl<PosTag> implements PosTaggedToken {
	private  PosTaggerLexiconService lexiconService;
	Set<LexicalEntry> entries = null;
	public PosTaggedTokenImpl(Token token, PosTag tag, double probability) {
		super(token, tag, probability);
	}

	@Override
	public Set<LexicalEntry> getLexicalEntries() {
		if (entries==null) {
			entries = this.lexiconService.findLexicalEntries(this.getToken().getText(), this.getTag());
		}
		return entries;
	}

	public PosTaggerLexiconService getLexiconService() {
		return lexiconService;
	}

	public void setLexiconService(PosTaggerLexiconService lexiconService) {
		this.lexiconService = lexiconService;
	}

	@Override
	public String toString() {
		return this.getToken().getText() + "|" + this.getTag() + "|" + this.getToken().getIndex() + "| prob=" + this.getProbability();
	}

	@Override
	public LexicalEntry getLexicalEntry() {
		Set<LexicalEntry> lexicalEntries = this.getLexicalEntries();
		LexicalEntry bestLexicalEntry = null;
		if (lexicalEntries.size()>0) {
			bestLexicalEntry = lexicalEntries.iterator().next();
		}
		return bestLexicalEntry;
	}


}
