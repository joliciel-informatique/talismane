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

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A HashMap with a bounded number of entries. Automatically removes the oldest
 * entry.
 * 
 * @author Assaf Urieli
 *
 */
public class BoundedHashMap<K, V> extends HashMap<K, V> {
  private static final long serialVersionUID = 1L;
  private LinkedBlockingDeque<K> deque;
  private int capacity = 0;

  public BoundedHashMap(int capacity) {
    super();
    this.capacity = capacity;
    deque = new LinkedBlockingDeque<K>(capacity);
  }

  @Override
  public V put(K key, V value) {
    if (this.capacity == 0) {
      // max size was initiated to zero => just return false
      return null;
    }

    boolean contains = super.containsKey(key);
    if (contains) {
      // simply replace the former value with a new one
      return super.put(key, value);
    }

    if (deque.remainingCapacity() <= 0) {
      K removeKey = deque.pollLast();
      super.remove(removeKey);
    }

    deque.add(key);
    return super.put(key, value);
  }
}
