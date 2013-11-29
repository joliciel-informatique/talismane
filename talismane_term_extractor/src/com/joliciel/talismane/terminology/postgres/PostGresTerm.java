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
package com.joliciel.talismane.terminology.postgres;

import java.util.Set;

import com.joliciel.talismane.terminology.Term;
import com.joliciel.talismane.terminology.TerminologyBase;
import com.joliciel.talismane.utils.PersistentSet;

interface PostGresTerm extends Term {
	public boolean isNew();
	public abstract void setFrequency(int frequency);

	public abstract void setText(String text);
	public int getTextId();
	public void setTextId(int textId);

	public abstract void setId(int id);

	public abstract int getId();
	
	public PersistentSet<Term> getHeadSet();
	public PersistentSet<Term> getExpansionSet();
	public Set<Term> getParentsInternal();
	public void setParentsInternal(Set<Term> parents);
	
	public abstract void setTerminologyBase(TerminologyBase terminologyBase);
	public abstract TerminologyBase getTerminologyBase();
	
	public boolean isDirty();
	public void setDirty(boolean dirty);
	public void setExpansionCount(int expansionCount);
	public void setHeadCount(int headCount);

}
