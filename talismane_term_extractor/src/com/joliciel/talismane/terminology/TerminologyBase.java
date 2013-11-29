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
package com.joliciel.talismane.terminology;

import java.util.List;
import java.util.Set;

/**
 * A collection of "terms" extracted from a given corpus.
 * @author Assaf Urieli
 *
 */
public interface TerminologyBase {
	/**
	 * Get a set of terms appearing in the corpus above a certain frequency threshold.
	 * @param frequencyThreshold
	 * @return
	 */
	public List<Term> getTermsByFrequency(int frequencyThreshold);
	
	/**
	 * Get a set of terms matching a certain search string.
	 * @param searchText
	 * @return
	 */
	public List<Term> getTermsByText(String searchText);
	
	/**
	 * Get all terms that have been "marked" as true terms.
	 * @return
	 */
	public List<Term> getMarkedTerms();
	
	public List<Term> getTerms(final int frequencyThreshold,
			final String searchText, final boolean marked);

	public List<Term> getTerms(final int frequencyThreshold,
			final String searchText, final boolean marked, final boolean markedExpansions);

	/**
	 * Get a term corresponding to a particular string.
	 * @param text
	 * @return
	 */
	public Term getTerm(String text);
	
	public Context getContext(String fileName, int lineNumber, int columnNumber);
	
	/**
	 * Store the term in the datastore.
	 * @param term
	 */
	public void storeTerm(Term term);
	
	public void storeContext(Context context);
	
	public void commit();
	
	/**
	 * Load the parents of a given term from the datastore.
	 * @param term
	 * @return
	 */
	public Set<Term> getParents(Term term);
	
	
	/**
	 * Load the heads of a given term from the datastore.
	 * @param term
	 * @return
	 */
	public Set<Term> getHeads(Term term);
	
	/**
	 * Load the expansions of a given term from the datastore.
	 * @param term
	 * @return
	 */
	public Set<Term> getExpansions(Term term);
	
	/**
	 * Load the contexts of a given term from the datastore.
	 * @param term
	 * @return
	 */
	public List<Context> getContexts(Term term);
}
