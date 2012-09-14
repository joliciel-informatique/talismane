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

import java.util.ArrayList;

public class TaggedTokenSequenceImpl<T extends TokenTag> extends ArrayList<TaggedToken<T>> implements TaggedTokenSequence<T>, Comparable<TaggedTokenSequenceImpl<T>> {
	private static final long serialVersionUID = -8634022202538586472L;
	private double score = 0;
	private boolean scoreCalculated = false;
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
	
	/**
	 * Combine two sequences into one.
	 * @param sequence1
	 * @param sequence2
	 */
	public TaggedTokenSequenceImpl(TaggedTokenSequence<T> sequence1, TaggedTokenSequence<T> sequence2) {
		super(sequence1.size() + sequence2.size());
		this.addAll(sequence1);
		this.addAll(sequence2);
		this.setScore(sequence1.getScore() + sequence2.getScore());
	}
	
	@Override
	public TaggedToken<T> addTaggedToken(Token token, T tag, double probability) {
		TaggedToken<T> taggedToken = new TaggedTokenImpl<T>(token, tag, probability);
		this.add(taggedToken);
		return taggedToken;
	}

	/* (non-Javadoc)
	 * @see com.joliciel.Talismane.training.LetterSequence#getScore()
	 */
	@Override
	public double getScore() {
		if (!scoreCalculated) {
			for (TaggedToken<T> token : this) {
				score = score + token.getProbLog();
			}
			scoreCalculated = true;
		}
		return score;
	}
	
	public void setScore(double score) {
		this.score = score;
		scoreCalculated = true;
	}

	@Override
	public int compareTo(TaggedTokenSequenceImpl<T> o) {
		if (this.getScore()<o.getScore()) {
			return 1;
		} else if (this.getScore()>o.getScore()) {
			return -1;
		} else {
			return 0;
		}
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
