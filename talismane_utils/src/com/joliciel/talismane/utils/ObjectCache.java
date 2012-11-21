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
package com.joliciel.talismane.utils;

/**
 * A cache for storing objects so they don't have to be reloaded multiple times from a datastore.
 * @author Assaf Urieli
 *
 */
public interface ObjectCache {
	/**
	 * Retrieve from the cache the entity corresponding to a given Class and id.
	 * @param clazz
	 * @param id
	 * @return the entity if in the cache, or null otherwise.
	 */
    public Object getEntity(Class<? extends Object> clazz, Object id);
    
    /**
     * Put the entity in the cache corresponding to a given Class and id.
     * @param clazz
     * @param id
     * @param entity
     */
    public void putEntity(Class<? extends Object> clazz, Object id, Object entity);
    
    /**
     * Remove the entity from the cache corresponding to a given Class and id.
     * @param clazz
     * @param id
     */
    public void removeEntity(Class<? extends Object> clazz, Object id);
    
    /**
     * If the Class and id provided already have an entity in the cache, return it,
     * otherwise put the entity provided in the cache and return it.
     * @param clazz
     * @param id
     * @param entity
     * @return
     */
    public Object getOrPutEntity(Class<? extends Object> clazz, Object id, Object entity);
    
    /**
     * Clear all entities out of the cache.
     */
    public void clearCache();
    
    /**
     * Clear all entities out of the cache corresponding to a particular Class only.
     * @param clazz
     */
    public void clearCache(Class<? extends Object> clazz);
}
