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

import java.util.HashSet;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A HashSet with a bounded number of entries. Automatically removes the oldest
 * entry.
 * 
 * @author Assaf Urieli
 *
 */
public class BoundedHashSet<E> extends HashSet<E> {
  private static final long serialVersionUID = 1L;
  private LinkedBlockingDeque<E> deque;
  private int capacity = 0;

  public BoundedHashSet(int capacity) {
    super();
    this.capacity = capacity;
    deque = new LinkedBlockingDeque<E>(capacity);
  }

  @Override
  public boolean add(E e) {
    if (this.capacity == 0) {
      // max size was initiated to zero => just return false
      return false;
    }

    boolean contains = super.contains(e);
    if (contains) {
      return false;
    }

    if (deque.remainingCapacity() <= 0) {
      E removeElement = deque.pollLast();
      super.remove(removeElement);
    }

    boolean added = super.add(e);
    if (added)
      deque.add(e);
    return added;
  }
}
