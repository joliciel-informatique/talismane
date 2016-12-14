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
package com.joliciel.talismane.sentenceAnnotators;

import java.util.List;
import java.util.Map;

import com.joliciel.talismane.Annotator;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.tokeniser.TokenAttribute;

/**
 * An annotator applied to a single sentence, after processing (e.g. no
 * duplicate white space, etc.). It can add the following annotations:<br/>
 * <ul>
 * <li>{@link TokenPlaceholder} for a deterministic token boundary.</li>
 * <li>{@link TokenAttribute} for an arbitrary attribute added to any enclosed
 * tokens.</li>
 * </ul>
 * 
 * @author Assaf Urieli
 *
 */
public interface SentenceAnnotator extends Annotator<Sentence> {
	/**
	 * Load the annotator's state using information extracted from a descriptor.
	 * 
	 * @param parameters
	 *            a series of name/value parameters
	 * @param tabs
	 *            a list of unnamed parameters, whose placement determines their
	 *            meaning
	 * @throws SentenceAnnotatorLoadException
	 *             if any loading error occurs related to the parameters or tabs
	 */
	public void load(Map<String, String> parameters, List<String> tabs) throws SentenceAnnotatorLoadException;

	/**
	 * Returns true if this TokenFilter should be excluded from the list of
	 * TokenFilters for the current configuration. This will typically be set
	 * during the load method, based on context specific considerations.
	 */
	public boolean isExcluded();
}
