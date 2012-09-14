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
package com.joliciel.talismane.posTagger;

/**
 * An enum to indicate whether a PosTag is an open class, a closed class,
 * or an open class which only accepts entries outside of the lexicon.
 * @author Assaf Urieli
 *
 */
public enum PosTagOpenClassIndicator {
	OPEN(0),
	CLOSED(1);
	
	int id;
	
	private PosTagOpenClassIndicator(int id) {
		this.id = id;
	}
	
	public static PosTagOpenClassIndicator forId(int id) throws IllegalArgumentException  {
        for (PosTagOpenClassIndicator posTagClosedClassType : PosTagOpenClassIndicator.values()) {
            if (posTagClosedClassType.getId()==id)
                return posTagClosedClassType;
        }
        throw new IllegalArgumentException("No PosTagClosedClassType found for id " + id);
    }

	public int getId() {
		return id;
	}
}
