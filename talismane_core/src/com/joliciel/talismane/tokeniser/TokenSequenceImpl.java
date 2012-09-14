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
package com.joliciel.talismane.tokeniser;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;

class TokenSequenceImpl extends AbstractTokenSequence implements TokenSequence {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(TokenSequenceImpl.class);
	private static final long serialVersionUID = 2675309892340757939L;
	
	public TokenSequenceImpl(String sentence, Pattern separatorPattern, TokeniserServiceInternal tokeniserServiceInternal) {
		this(sentence);
		this.setTokeniserServiceInternal(tokeniserServiceInternal);
		
		Matcher matcher = separatorPattern.matcher(sentence);
		int currentPos = 0;
		while (matcher.find()) {
			if (matcher.start()>currentPos)
				this.addToken(currentPos, matcher.start());
			Token separator = this.addToken(matcher.start(), matcher.end());
			separator.setSeparator(true);
			currentPos = matcher.end();
		}
		if (currentPos<sentence.length())
			this.addToken(currentPos, sentence.length());
		
		this.finalise();
	}

	public TokenSequenceImpl(String sentence, TokeniserDecisionTagSequence tokeniserDecisionTagSequence) {
		this(sentence);
		this.tokeniserDecisionTagSequence = tokeniserDecisionTagSequence;
	}
	
	public TokenSequenceImpl(String sentence) {
		super(sentence);
	}


	@Override
	public void add(int index, Token element) {
		throw new TalismaneException("Cannot add tokens directly, only by index");
	}

	@Override
	public synchronized boolean addAll(Collection<? extends Token> c) {
		throw new TalismaneException("Cannot add tokens directly, only by index");
	}

	@Override
	public synchronized boolean addAll(int index, Collection<? extends Token> c) {
		throw new TalismaneException("Cannot add tokens directly, only by index");
	}

	public boolean add(Token token) {
		throw new TalismaneException("Cannot add tokens directly, only by index");
	}
	
}
