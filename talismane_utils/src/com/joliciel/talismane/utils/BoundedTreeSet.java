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
package com.joliciel.talismane.utils;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * A treeset with a bounded number of elements.
 * Automatically removes the last element.
 * Thus assumes the most "important" element is placed first ("least" in comparison).
 * @author Assaf Urieli
 *
 * @param <E>
 */
public class BoundedTreeSet<E> extends TreeSet<E> {
	private static final long serialVersionUID = 234375156025262002L;
	private int elementsLeft;

    public BoundedTreeSet(int maxSize) {
        super(new NaturalComparator<E>());
        this.elementsLeft = maxSize;
    }

    public BoundedTreeSet(int maxSize, Comparator<E> comparator) {
        super(comparator);
        this.elementsLeft = maxSize;
    }


    /**
     * @return true if element was added, false otherwise
     * */
    @Override
    public boolean add(E e) {
        if (elementsLeft == 0 && size() == 0) {
            // max size was initiated to zero => just return false
            return false;
        } else if (elementsLeft > 0) {
            // queue isn't full => add element and decrement elementsLeft
            boolean added = super.add(e);
            if (added) {
                elementsLeft--;
            }
            return added;
        } else {
            // there is already 1 or more elements => compare to the least
            int compared = super.comparator().compare(e, this.last());
            if (compared<0) {
                // new element is larger than the least in queue => pull the least and add new one to queue
                pollLast();
                super.add(e);
                return true;
            } else {
                // new element is less than the least in queue => return false
                return false;
            }
        }
    }

    private static class NaturalComparator<T> implements Comparator<T> {
        @Override
        public int compare(T o1, T o2) {
        	@SuppressWarnings("unchecked")
			Comparable<T> c1 = (Comparable<T>) o1;
           	return c1.compareTo(o2);
        }
    }
}
