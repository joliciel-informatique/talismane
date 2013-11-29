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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.joliciel.talismane.terminology.Context;
import com.joliciel.talismane.terminology.Term;
import com.joliciel.talismane.terminology.TerminologyBase;
import com.joliciel.talismane.utils.PersistentSet;
import com.joliciel.talismane.utils.PersistentSetImpl;

public class PostGresTermImpl implements PostGresTerm, Serializable, Comparable<Term> {
	private static final long serialVersionUID = -5945499181086667962L;
	
	private int id;
	private int textId;
	private String text;
	private PersistentSet<Term> heads;
	private PersistentSet<Term> expansions;
	private Set<Term> parents;
	private List<Context> contexts = null;
	private int frequency = 0;
	private int expansionCount = 0;
	private int headCount = 0;
	private boolean marked = false;
	private TerminologyBase terminologyBase;
	private boolean dirty = true;
	
	protected PostGresTermImpl() {
		// for frameworks & DAO
	}
	
	public PostGresTermImpl(String text) {
		super();
		this.text = text;
	}
	
	public String getText() {
		return text;
	}
	public Set<Term> getHeads() {
		return this.getHeadSet();
	}
	public PersistentSet<Term> getHeadSet() {
		if (this.heads==null) {
			this.heads = new PersistentSetImpl<Term>(new TreeSet<Term>());
			if (!this.isNew())
				this.heads.addAllFromDB(this.terminologyBase.getHeads(this));
		}
		return heads;
	}
	
	public Set<Term> getExpansions() {
		return this.getExpansionSet();
	}
	

	public PersistentSet<Term> getExpansionSet() {
		if (this.expansions==null) {
			this.expansions = new PersistentSetImpl<Term>(new TreeSet<Term>());
			if (!this.isNew())
				this.expansions.addAllFromDB(this.terminologyBase.getExpansions(this));
		}
		return expansions;
	}
	

	@Override
	public Set<Term> getParents() {
		if (this.parents==null) {
			this.parents = this.terminologyBase.getParents(this);
		}
		return parents;
	}	

	@Override
	public Set<Term> getParentsInternal() {
		return this.parents;
	}
	
	
	@Override
	public void setParentsInternal(Set<Term> parents) {
		this.parents = parents;
	}

	public List<Context> getContexts() {
		if (this.contexts==null) {
			if (this.isNew()) {
				this.contexts = new ArrayList<Context>();
			} else {
				this.contexts = this.terminologyBase.getContexts(this);
			}
		}
		return contexts;
	}

	@Override
	public int compareTo(Term o) {
		return (this.text.compareTo(o.getText()));
	}

	@Override
	public void addHead(Term head) {
		boolean success = this.getHeads().add(head);
		if (success) this.setHeadCount(this.getHeadCount()+1);
	}

	@Override
	public void addExpansion(Term expansion) {
		boolean success = this.getExpansions().add(expansion);
		if (success) this.setExpansionCount(this.getExpansionCount()+1);
	}

	@Override
	public void addContext(Context context) {
		this.getContexts().add(context);
		context.setTerm(this);
		this.setFrequency(this.getFrequency()+1);
	}

	public int getFrequency() {
		return frequency;
	}

	@Override
	public void setText(String text) {
		this.text = text;
	}

	@Override
	public void setFrequency(int frequency) {
		if (this.frequency!=frequency) {
			this.frequency = frequency;
			this.dirty = true;
		}
	}

	@Override
	public String toString() {
		return "Term [text=" + text + "]";
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	@Override
	public boolean isNew() {
		return this.id == 0;
	}

	@Override
	public TerminologyBase getTerminologyBase() {
		return terminologyBase;
	}

	@Override
	public void setTerminologyBase(TerminologyBase terminologyBase) {
		this.terminologyBase = terminologyBase;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public boolean isMarked() {
		return marked;
	}

	public void setMarked(boolean marked) {
		if (this.marked!=marked) {
			this.marked = marked;
			this.dirty = true;
		}
	}

	public int getExpansionCount() {
		return expansionCount;
	}

	public void setExpansionCount(int expansionCount) {
		if (this.expansionCount!=expansionCount) {
			this.expansionCount = expansionCount;
			this.dirty = true;
		}
	}
	
	public int getHeadCount() {
		return headCount;
	}

	public void setHeadCount(int headCount) {
		if (this.headCount!=headCount) {
			this.headCount = headCount;
			this.dirty = true;
		}
	}


	@Override
	public void save() {
		this.terminologyBase.storeTerm(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PostGresTermImpl other = (PostGresTermImpl) obj;
		if (id != other.id)
			return false;
		return true;
	}

	public int getTextId() {
		return textId;
	}

	public void setTextId(int textId) {
		if (this.textId!=textId) {
			this.textId = textId;
			this.dirty = true;
		}
	}

}
