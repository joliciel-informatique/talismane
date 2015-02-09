///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Joliciel Informatique
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
package com.joliciel.talismane.utils;

import java.util.ArrayList;
import java.util.Collection;

public class ArrayListNoNulls<E> extends ArrayList<E> {
	private static final long serialVersionUID = 1L;

	public ArrayListNoNulls() {
		super();
	}

	public ArrayListNoNulls(Collection<? extends E> c) {
		super(c);
	}

	public ArrayListNoNulls(int initialCapacity) {
		super(initialCapacity);
	}

	@Override
	public boolean add(E element) {
		if (element==null)
			throw new IllegalArgumentException("Cannot add null to " + ArrayListNoNulls.class.getSimpleName());
		return super.add(element);
	}

	@Override
	public void add(int index, E element) {
		if (element==null)
			throw new IllegalArgumentException("Cannot add null to " + ArrayListNoNulls.class.getSimpleName());
		super.add(index, element);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		for (E element : c)
			if (element==null)
				throw new IllegalArgumentException("Cannot add null to " + ArrayListNoNulls.class.getSimpleName());
		return super.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		for (E element : c)
			if (element==null)
				throw new IllegalArgumentException("Cannot add null to " + ArrayListNoNulls.class.getSimpleName());
		return super.addAll(index, c);
	}

	
}
