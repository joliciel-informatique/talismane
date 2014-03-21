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
package com.joliciel.talismane;

import java.util.Map;

public interface AnnotatedCorpusReader {
	/**
	 * If 0, all sentences will be read - otherwise will only read a certain number of sentences.
	 * @return
	 */
	public int getMaxSentenceCount();
	public void setMaxSentenceCount(int maxSentenceCount);
	
	/**
	 * The index of the first sentence to process.
	 * @return
	 */
	public int getStartSentence();
	public void setStartSentence(int startSentence);
	
	/**
	 * The number of cross-validation segments for this corpus.
	 * -1 means no cross-validation.
	 * @param crossValidationSize
	 */
	public abstract void setCrossValidationSize(int crossValidationSize);
	public abstract int getCrossValidationSize();
	
	/**
	 * Which index to exclude when reading (for training), from 0 to getCrossValidationSize-1.
	 * @param excludeIndex
	 */
	public abstract void setExcludeIndex(int excludeIndex);
	public abstract int getExcludeIndex();
	
	/**
	 * Which index to include when reading (for evaluation), from 0 to getCrossValidationSize-1.
	 * @param includeIndex
	 */
	public abstract void setIncludeIndex(int includeIndex);
	public abstract int getIncludeIndex();
	

	/**
	 * Characteristics describing this corpus reader.
	 * @return
	 */
	public Map<String,String> getCharacteristics();
}
