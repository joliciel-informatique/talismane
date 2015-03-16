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

import java.util.ArrayList;

import com.joliciel.talismane.machineLearning.Decision;

public class TaggedTokenSequenceImpl<T extends TokenTag> extends ArrayList<TaggedToken<T>> implements TaggedTokenSequence<T> {
	private static final long serialVersionUID = -8634022202538586472L;
	private String string = null;
	
	public TaggedTokenSequenceImpl() {
		super();
	}
	
	public TaggedTokenSequenceImpl(int initialCapacity) {
		super(initialCapacity);
	}
	
	/**
	 * Create a letter sequence with space to one additional letter at the end
	 * of an existing history.
	 * @param history
	 */
	public TaggedTokenSequenceImpl(TaggedTokenSequence<T> history) {
		super(history.size()+1);
		this.addAll(history);
	}
	
	@Override
	public TaggedToken<T> addTaggedToken(Token token, Decision decision, T tag) {
		TaggedToken<T> taggedToken = new TaggedTokenImpl<T>(token, decision, tag);
		this.add(taggedToken);
		return taggedToken;
	}

	@Override
	public synchronized String toString() {
		if (string==null) {
			StringBuilder builder = new StringBuilder();
			builder.append("Sequence: " );
			for (TaggedToken<T> taggedToken : this) {
				builder.append(taggedToken.toString());
			}
			string = builder.toString();
		}
		return string;
	}

}
