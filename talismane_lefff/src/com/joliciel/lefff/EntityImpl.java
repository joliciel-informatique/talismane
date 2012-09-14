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
package com.joliciel.lefff;

import java.io.Serializable;

abstract class EntityImpl implements EntityInternal, Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 3078156188378416329L;
	private int id;
 
    public boolean isNew() {
        return (id==0);
    }

    public final  int getId() {
        return id;
    }

    public final void setId(int id) {
        this.id = id;
    }

    public final void save() {
        this.saveInternal();
    }
    
    public abstract void saveInternal();

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EntityImpl other = (EntityImpl) obj;
		if (id != other.id)
			return false;
		return true;
	}
    
    
}
