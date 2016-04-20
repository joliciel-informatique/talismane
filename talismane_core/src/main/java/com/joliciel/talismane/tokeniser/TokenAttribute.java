///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
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
package com.joliciel.talismane.tokeniser;

import java.io.Externalizable;

/**
 * An attribute that can be tagged onto a token.
 * 
 * @author Assaf Urieli
 *
 * @param <T>
 *            the content type of this attribute, must implement hashcode and
 *            equals
 */
public abstract class TokenAttribute<T> implements Externalizable {
	protected String key;
	protected T value;

	/**
	 * For deserialization only.
	 */
	TokenAttribute() {
	}

	public TokenAttribute(String key, T value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * The attribute's key
	 * 
	 * @return
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get this attribute's value.
	 */
	public T getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("rawtypes")
		TokenAttribute other = (TokenAttribute) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TokenAttribute [key=" + key + ", value=" + value + "]";
	}

}