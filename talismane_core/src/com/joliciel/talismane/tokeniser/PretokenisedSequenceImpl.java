///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
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
package com.joliciel.talismane.tokeniser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.filters.FilterService;

class PretokenisedSequenceImpl extends AbstractTokenSequence implements PretokenisedSequence {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(PretokenisedSequenceImpl.class);
	private static final long serialVersionUID = 2675309892340757939L;
	
	private FilterService filterService;
	
	public PretokenisedSequenceImpl(FilterService filterService) {
		this(filterService,"");
	}
	
	public PretokenisedSequenceImpl(FilterService filterService, String text) {
		super(filterService.getSentence(text));
		this.filterService = filterService;
		if (text.length()>0)
			this.textProvided = true;
	}

	@Override
	public Token addToken(int start, int end) {
		throw new TalismaneException("Cannot add tokens by index");

	}
	
	@Override
	public TokenSequence cloneTokenSequence() {
		PretokenisedSequenceImpl tokenSequence = new PretokenisedSequenceImpl(filterService, this.getText());
		this.cloneTokenSequence(tokenSequence);
		return tokenSequence;
	}
}
