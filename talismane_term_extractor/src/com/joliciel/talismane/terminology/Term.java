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
 * A single term extracted from a corpus.
 * @author Assaf Urieli
 *
 */
public interface Term {
	public String getText();
	
	/**
	 * The heads are any terms containing this term, with exactly one degree
	 * of additional syntactic depth.
	 * e.g. for the term "wine", "bottle of wine" is a head.
	 * @return
	 */
	public Set<Term> getHeads();
	
	/**
	 * The expansions are any terms in which this term is contained,
	 * with exactly 1 degree of additional syntactic depth.
	 * For example, for the term "bottle", "bottle of wine" is an expansion.
	 * However, "bottle of red wine" is not an expansion (2 degrees of depth) -
	 * it is an expansion of "bottle of wine".
	 * @return
	 */
	public Set<Term> getExpansions();
	
	/**
	 * The parents are terms of which this term is an expansion.
	 * For example, for the term "bottle of wine", "bottle" is a parent.
	 * @return
	 */
	public Set<Term> getParents();
	
	public List<Context> getContexts();
	
	public void addHead(Term head);
	public void addExpansion(Term expansion);
	public void addContext(Context context);
	
	/**
	 * The number of times this term appears in the corpus.
	 * @return
	 */
	public int getFrequency();
	
	/**
	 * The number of expansions this term has.
	 * @return
	 */
	public int getExpansionCount();
	
	/**
	 * The number of heads this term has.
	 * @return
	 */
	public int getHeadCount();
	
	/**
	 * The number of lexical words in this term (as opposed to function words).
	 * @return
	 */
	public int getLexicalWordCount();
	public void setLexicalWordCount(int lexicalWordCount);
	
	/**
	 * Has this term been marked as a true term for exporting?
	 * @return
	 */
	public boolean isMarked();
	public void setMarked(boolean marked);
	
	public boolean isNew();
	public void save();
}
