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
package com.joliciel.talismane.parser;

import com.joliciel.talismane.posTagger.PosTaggedToken;

class DependencyArcImpl implements DependencyArc {
	private PosTaggedToken head;
	private PosTaggedToken dependent;
	private String label;

	public DependencyArcImpl(PosTaggedToken head, PosTaggedToken dependent,
			String label) {
		super();
		this.head = head;
		this.dependent = dependent;
		this.label = label;
	}

	@Override
	public PosTaggedToken getHead() {
		return this.head;
	}

	@Override
	public PosTaggedToken getDependent() {
		return this.dependent;
	}

	@Override
	public String getLabel() {
		return this.label;
	}

	@Override
	public int hashCode() {
		final int prime = 2;
		int result = 1;
		result = prime * result
				+ ((dependent == null) ? 0 : dependent.hashCode());
		result = prime * result + ((head == null) ? 0 : head.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
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
		DependencyArcImpl other = (DependencyArcImpl) obj;
		if (dependent == null) {
			if (other.dependent != null)
				return false;
		} else if (!dependent.equals(other.dependent))
			return false;
		if (head == null) {
			if (other.head != null)
				return false;
		} else if (!head.equals(other.head))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Dep[" + head + "," + label + ","+ dependent + "]";
	}

	@Override
	public int compareTo(DependencyArc o) {
		return this.getDependent().getToken().getIndex() - o.getDependent().getToken().getIndex();
	}

}
