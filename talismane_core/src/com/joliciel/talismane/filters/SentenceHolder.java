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
package com.joliciel.talismane.filters;

import java.util.List;
import java.util.Set;

public interface SentenceHolder extends Sentence {
	/**
	 * Add a sentence boundary to this sentence holder.
	 * @param boundary
	 */
	void addSentenceBoundary(int boundary);
	Set<Integer> getSentenceBoundaries();
	
	/**
	 * Based on the sentence boundaries added, return all the sentences produced by this sentence holder.
	 * If there is any text left over, the last sentence will be marked as not complete.
	 * @param leftOverText an incomplete sentence returned by the previous sentence holder.
	 * @return
	 */
	List<Sentence> getDetectedSentences(Sentence leftOverText);
}
