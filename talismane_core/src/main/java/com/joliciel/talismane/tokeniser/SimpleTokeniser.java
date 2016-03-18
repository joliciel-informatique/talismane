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

import java.util.List;
import java.util.ArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.Decision;


/**
 * A simplistic implementation of a Tokeniser, using only TokenFilters and default decisions.
 * @author Assaf Urieli
 *
 */
class SimpleTokeniser extends AbstractTokeniser {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(SimpleTokeniser.class);

	@Override
	protected List<TokenisedAtomicTokenSequence> tokeniseInternal(TokenSequence initialSequence, Sentence sentence) {
		List<TokenisedAtomicTokenSequence> sequences = null;

		sequences = new ArrayList<TokenisedAtomicTokenSequence>();
		TokenisedAtomicTokenSequence defaultSequence = this.getTokeniserService().getTokenisedAtomicTokenSequence(sentence, 0);
		for (Token token : initialSequence.listWithWhiteSpace()) {
			Decision tokeniserDecision = this.getMachineLearningService().createDefaultDecision(TokeniserOutcome.SEPARATE.name());
			TaggedToken<TokeniserOutcome> taggedToken = this.getTokeniserService().getTaggedToken(token, tokeniserDecision);
			defaultSequence.add(taggedToken);
		}
		sequences.add(defaultSequence);
		
		return sequences;
	}

	
	
}
