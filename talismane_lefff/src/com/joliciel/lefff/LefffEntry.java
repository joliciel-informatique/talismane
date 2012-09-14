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
package com.joliciel.lefff;

import java.io.Serializable;
import java.util.List;

import com.joliciel.talismane.posTagger.LexicalEntry;

interface LefffEntry extends Entity, Serializable, LexicalEntry {
	public Word getLefffWord();
	public Lemma getLefffLemma();
	public Predicate getLefffPredicate();
	public Attribute getLefffMorphology();
	public Category getLefffCategory();
	
	public int getWordId();
	public int getLemmaId();
	public int getPredicateId();
	public int getMorphologyId();
	public int getCategoryId();
	
	public int getLexicalWeight();
	
	public void setWordId(int wordId);
	public void setLemmaId(int lemmaId);
	public void setPredicateId(int predicateId);
	public void setMorphologyId(int morphologyId);
	public void setCategoryId(int categoryId);
	
	public void setLexicalWeight(int lexicalWeight);

	
	public List<Attribute> getAttributes();
}
