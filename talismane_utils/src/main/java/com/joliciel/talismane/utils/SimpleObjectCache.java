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
import java.util.Map;

/**
 * A default implementation for ObjectCache.
 * @author Assaf Urieli
 *
 */
public class SimpleObjectCache implements ObjectCache {
    private Map<Class<? extends Object>,Map<Object,Object>> cache = new HashMap<Class<? extends Object>,Map<Object,Object>>();
    
    public void clearCache() {
        cache.clear();
    }

    @Override
	public void clearCache(Class<? extends Object> clazz) {
    	cache.remove(clazz);
	}

	private Map<Class<? extends Object>,Map<Object,Object>> getCache() {
        return cache;
    }
    
    @SuppressWarnings("unchecked")
	public<T> T getEntity(Class<T> clazz, Object id) {
        T entity = null;
        Map<Class<? extends Object>,Map<Object,Object>> cache = this.getCache();
        Map<Object,Object> objectMap = cache.get(clazz);
        if (objectMap!=null)
            entity = (T) objectMap.get(id);
        return entity;
    }

    public<T> void removeEntity(Class<T> clazz, Object id) {
        Map<Class<? extends Object>,Map<Object,Object>> cache = this.getCache();
        Map<Object,Object> objectMap = cache.get(clazz);
        if (objectMap!=null)
            objectMap.remove(id);
    }

    public<T> void putEntity(Class<T> clazz, Object id, T entity) {
        Map<Class<? extends Object>,Map<Object,Object>> cache = this.getCache();
        Map<Object,Object> objectMap = cache.get(clazz);
        if (objectMap==null) {
            objectMap = new HashMap<Object, Object>();
            cache.put(clazz, objectMap);
        }
        objectMap.put(id, entity);
    }


    @SuppressWarnings("unchecked")
	public<T> T getOrPutEntity(Class<T> clazz, Object id, T entity) {
        Object localEntity = this.getEntity(clazz, id);
        if (localEntity==null) {
        	this.putEntity(clazz, id, entity);
        } else {
        	entity = (T) localEntity;
        }
        return entity;
    }
}
