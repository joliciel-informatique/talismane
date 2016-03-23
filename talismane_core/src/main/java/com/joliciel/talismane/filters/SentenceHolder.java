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
package com.joliciel.talismane.filters;

import java.util.List;
import java.util.Set;

import com.joliciel.talismane.tokeniser.TokenAttribute;

public interface SentenceHolder extends Sentence {
	/**
	 * Add a sentence boundary to this sentence holder.
	 */
	public void addSentenceBoundary(int boundary);

	public Set<Integer> getSentenceBoundaries();

	/**
	 * Based on the sentence boundaries added, return all the sentences produced
	 * by this sentence holder. If there is any text left over, the last
	 * sentence will be marked as not complete. After this is called,
	 * {@link #getOriginalTextSegments()} will only return leftover original
	 * text segments that have not yet been assigned to sentences in the current
	 * list.
	 * 
	 * @param leftOverText
	 *            an incomplete sentence returned by the previous sentence
	 *            holder.
	 */
	public List<Sentence> getDetectedSentences(Sentence leftOverText);

	/**
	 * Indicate that a tag starts at this position.
	 */
	public <T> void addTagStart(String attribute, TokenAttribute<T> value, int position);

	/**
	 * Indicate that a tag ends at this position.
	 */
	public <T> void addTagEnd(String attribute, TokenAttribute<T> value, int position);
}
