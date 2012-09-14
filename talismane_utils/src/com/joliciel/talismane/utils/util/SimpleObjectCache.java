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
package com.joliciel.talismane.utils.util;

import java.util.HashMap;
import java.util.Map;

public class SimpleObjectCache implements ObjectCache {

    private static ThreadLocal<Map<Class<? extends Object>,Map<Object,Object>>> cacheHolder = new ThreadLocal<Map<Class<? extends Object>,Map<Object,Object>>>();
    
    public void clearCache() {
        Map<Class<? extends Object>,Map<Object,Object>> cache = cacheHolder.get();
        if (cache!=null) {
            cache.clear();
        }
    }
    
    

    @Override
	public void clearCache(Class<? extends Object> clazz) {
    	Map<Class<? extends Object>,Map<Object,Object>> cache = cacheHolder.get();
    	if (cache!=null) {
    		cache.remove(clazz);
    	}
	}



	private Map<Class<? extends Object>,Map<Object,Object>> getCache() {
        Map<Class<? extends Object>,Map<Object,Object>> cache = cacheHolder.get();
        if (cache == null) {
            cache = new HashMap<Class<? extends Object>, Map<Object,Object>>();
            cacheHolder.set(cache);
        }
        return cache;
    }
    
    public Object getEntity(Class<? extends Object> clazz, Object id) {
        Object entity = null;
        Map<Class<? extends Object>,Map<Object,Object>> cache = this.getCache();
        Map<Object,Object> objectMap = cache.get(clazz);
        if (objectMap!=null)
            entity = objectMap.get(id);
        return entity;
    }

    public void removeEntity(Class<? extends Object> clazz, Object id) {
        Map<Class<? extends Object>,Map<Object,Object>> cache = this.getCache();
        Map<Object,Object> objectMap = cache.get(clazz);
        if (objectMap!=null)
            objectMap.remove(id);
    }

    public void putEntity(Class<? extends Object> clazz, Object id, Object entity) {
        Map<Class<? extends Object>,Map<Object,Object>> cache = this.getCache();
        Map<Object,Object> objectMap = cache.get(clazz);
        if (objectMap==null) {
            objectMap = new HashMap<Object, Object>();
            cache.put(clazz, objectMap);
        }
        objectMap.put(id, entity);
    }


    public Object getOrPutEntity(Class<? extends Object> clazz, Object id, Object entity) {
        Object localEntity = this.getEntity(clazz, id);
        if (localEntity==null) {
        	this.putEntity(clazz, id, entity);
        } else {
        	entity = localEntity;
        }
        return entity;
    }
}
