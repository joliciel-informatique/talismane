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
import java.util.TreeMap;

/**
 * A TreeMap with a bounded number of entries.
 * Automatically removes the last entry.
 * Thus assumes the most "important" entry is placed first ("least" in comparison).
 * @author Assaf Urieli
 *
 */
public class BoundedTreeMap<K, V> extends TreeMap<K, V> {
  private static final long serialVersionUID = 1L;
  private int elementsLeft;

  public BoundedTreeMap(int maxSize) {
    super(new NaturalComparator<K>());
    this.elementsLeft = maxSize;
  }

  public BoundedTreeMap(Comparator<? super K> comparator, int maxSize) {
    super(comparator);
    this.elementsLeft = maxSize;
  }

  @Override
  public V put(K key, V value) {
    if (elementsLeft == 0 && size() == 0) {
      // max size was initiated to zero => just return false
      return null;
    }
    
    boolean contains = super.containsKey(key);
    if (contains) {
      // simply replace the former value with a new one
      V oldValue = super.put(key, value);
      return oldValue;
    }
    
    if (elementsLeft > 0) {
      // queue isn't full => add element and decrement elementsLeft
      V oldValue = super.put(key, value);
      elementsLeft--;
      
      return oldValue;
    } else {
      // there is already 1 or more elements => compare to the least
      int compared = super.comparator().compare(key, this.lastKey());
      if (compared<0) {
        // new element is larger than the least in queue => pull the least and add new one to queue
        pollLastEntry();
        V oldValue = super.put(key, value);
        return oldValue;
      } else {
        // new element is less than the least in queue => return null
        return null;
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
