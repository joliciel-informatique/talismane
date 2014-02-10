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
package com.joliciel.lefff;

import java.util.List;
import java.util.Map;

import com.joliciel.talismane.lexicon.LexicalEntry;

interface LefffDao {
    public LefffEntry loadEntry(int entryId);
	public LefffEntryInternal findEntry(Word word, Lemma lemma, Category category);
   
    public Word loadWord(int wordId);

    public Lemma loadLemma(int lemmaId);

    public Predicate loadPredicate(int predicateId);

    public Attribute loadAttribute(int attributeId);
    public Category loadCategory(int categoryId);
    
    public void saveEntry(LefffEntryInternal entry);

    public void saveAttribute(AttributeInternal attribute);
    public void saveCategory(CategoryInternal category);

    public void saveLemma(LemmaInternal lemma);

    public void savePredicate(PredicateInternal predicate);
    
    public void saveAttributes(LefffEntryInternal entry);

    public void saveWord(WordInternal word);

    public List<Attribute> findAttributes(LefffEntryInternal entry);
    

	public Attribute loadAttribute(String attributeCode, String attributeValue);
	public Category loadCategory(String categoryCode);

	public Lemma loadLemma(String text, int index, String complement);

	public Word loadWord(String text);

	public Predicate loadPredicate(String text);
	public Map<String, List<LexicalEntry>> findEntryMap(List<String> categories);
	
    public void setLefffServiceInternal(LefffServiceInternal lefffServiceInternal);
	
}
