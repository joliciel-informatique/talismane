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
package com.joliciel.talismane.terminology;

import java.util.List;
import java.util.Set;

/**
 * A collection of "terms" extracted from a given corpus.
 * @author Assaf Urieli
 *
 */
public interface TerminologyBase {
	public List<Term> findTerms(final int frequencyThreshold,
			final String searchText, final int maxLexicalWords,
			final Boolean marked, final Boolean markedExpansions);

	/**
	 * Get a term corresponding to a particular string.
	 */
	public Term findTerm(String text);
	
	public Context findContext(Term term, String fileName, int lineNumber, int columnNumber);
	
	/**
	 * Store the term in the datastore.
	 */
	public void storeTerm(Term term);
	
	public void storeContext(Context context);
	
	public void commit();
	
	/**
	 * Load the parents of a given term from the datastore.
	 */
	public Set<Term> getParents(Term term);
	
	
	/**
	 * Load the heads of a given term from the datastore.
	 */
	public Set<Term> getHeads(Term term);
	
	/**
	 * Load the expansions of a given term from the datastore.
	 */
	public Set<Term> getExpansions(Term term);
	
	/**
	 * Load the contexts of a given term from the datastore.
	 */
	public List<Context> getContexts(Term term);
}
