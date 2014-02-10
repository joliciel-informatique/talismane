///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;

/**
 * The default implementation of PersistentList
 * @author Assaf Urieli
 *
 * @param <E>
 */
public class PersistentListImpl<E> implements PersistentList<E> {
    private List<E> itemsAdded;
    private List<E> itemsRemoved;
    private List<E> items;
    
    public PersistentListImpl() {
        itemsAdded = new ArrayList<E>();
        itemsRemoved = new ArrayList<E>();
        items = new ArrayList<E>();
    }

    public boolean addFromDB(E element) {
        return items.add(element);
    }
    

    public boolean addAllFromDB(Collection<? extends E> c) {
        return items.addAll(c);
    }

    public List<E> getItemsAdded() {
        return this.itemsAdded;
    }

    public List<E> getItemsRemoved() {
        return this.itemsRemoved;
    }

    
    public boolean isDirty() {
        return this.itemsAdded.size()>0 || this.itemsRemoved.size()>0;
    }

    public boolean add(E o) {
        boolean success = items.add(o);
        if (success) itemsAdded.add(o);
        return success;
    }

    public void add(int index, E element) {
        items.add(index, element);
        itemsAdded.add(element);
    }

    public boolean addAll(Collection<? extends E> c) {
        boolean success = true;
        for (E e : c) {
            if (!this.add(e))
                success = false;
        }
        return success;
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        boolean success = items.addAll(index, c);
        if (success) itemsAdded.addAll(c);
        return success;
    }

    public void clear() {
        itemsRemoved.addAll(this.items);
        itemsRemoved.removeAll(this.itemsAdded);
        this.itemsAdded.clear();
        this.items.clear();
    }

    public boolean contains(Object o) {
        return this.items.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return this.items.containsAll(c);
    }

    public E get(int index) {
        return this.items.get(index);
    }

    public int indexOf(Object o) {
        return this.items.indexOf(o);
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public Iterator<E> iterator() {
        return this.items.iterator();
    }

    public int lastIndexOf(Object o) {
        return this.items.lastIndexOf(o);
    }

    public ListIterator<E> listIterator() {
        return this.items.listIterator();
    }

    public ListIterator<E> listIterator(int index) {
        return this.items.listIterator(index);
    }

    public E remove(int index) {
        E o = this.items.remove(index);
        this.itemsRemoved.add(o);
        return o;
    }

    @SuppressWarnings("unchecked")
    public boolean remove(Object o) {
        boolean success = this.items.remove(o);
        
        if (success) {
            E element = (E) o;
            this.itemsRemoved.add(element);
        }
        return success;
    }

    public boolean removeAll(Collection<?> c) {
        boolean success = true;
        for  (Object o : c) {
            if (!this.remove(o))
                success = false;
        }
        return success;
    }

    public boolean retainAll(Collection<?> c) {
        List<E> toRemove = new ArrayList<E>();
        for (E element : this.items) {
            if (!c.contains(element))
                toRemove.add(element);
        }
        boolean success = true;
        for (E elementToRemove : toRemove) {
            if (!this.remove(elementToRemove))
                success = false;
        }
        return success;
    }

    public E set(int index, E element) {
        return this.items.set(index, element);
    }

    public int size() {
        return this.items.size();
    }

    public List<E> subList(int fromIndex, int toIndex) {
        return this.items.subList(fromIndex, toIndex);
    }

    public Object[] toArray() {
        return this.items.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return this.items.toArray(a);
    }

    

}
