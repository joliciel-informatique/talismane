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
import java.util.List;

/**
 * A List representing dependent objects stored in the database.
 * @author Assaf Urieli
 */
public interface PersistentList<E> extends List<E> {
    /***
     * To be used whenever populating the list initially from the database.
     * Makes it possible to differentiate between previously persisted objects and new ones being added.
     * @param object
     */
    public boolean addFromDB(E element);
    
    /***
     * Add a whole collection at a time.
     * @param c
     */
    public boolean addAllFromDB(Collection<? extends E> c);
    
    /**
     * Which items have been added that need to be persisted.
     * @return
     */
    public List<E> getItemsAdded();
    
    /**
     * Which items have been removed that need to be persisted.
     * @return
     */
    public List<E> getItemsRemoved();
    
    /**
     * Have items been added or removed from this list?
     * @return
     */
    public boolean isDirty();
}
