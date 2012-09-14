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
package com.joliciel.talismane.tokeniser;

import java.util.List;
import java.util.ArrayList;

public class TaggedTokenImpl<T extends TokenTag> implements TaggedToken<T> {
	private Token token = null;
	private T tag = null;
	private double probLog = 0;
	private boolean probLogCalculated = false;
	private double probability = 0;
	private List<String> taggers = new ArrayList<String>();
	
	public TaggedTokenImpl(Token token, T tag, double probability) {
		this.token = token;
		this.tag = tag;
		this.setProbability(probability);
	}
	
	public Token getToken() {
		return token;
	}

	public T getTag() {
		return tag;
	}

	public void setTag(T tag) {
		this.tag = tag;
	}

	public double getProbability() {
		return probability;
	}

	public void setProbability(double probability) {
		this.probability = probability;
		this.probLogCalculated = false;
	}

	@Override
	public double getProbLog() {
		if (!this.probLogCalculated) {
			this.probLog = Math.log(this.getProbability());
			this.probLogCalculated = true;
		}
		return this.probLog;
	}

	@Override
	public int compareTo(TaggedToken<T> o) {
		if (this.equals(o))
			return 0;
		if (!this.getToken().equals(o.getToken())) {
			return this.getToken().compareTo(o.getToken());
		}
		if (this.getTag().equals(o.getTag()))
			return 0;
		if (this.getProbability()>=o.getProbability())
			return -1;
		return 1;
	}

	public void addTagger(String tagger) {
		this.taggers.add(tagger);
	}

	public List<String> getTaggers() {
		return taggers;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
		result = prime * result + ((token == null) ? 0 : token.hashCode());
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
		TaggedTokenImpl other = (TaggedTokenImpl) obj;
		if (tag == null) {
			if (other.tag != null)
				return false;
		} else if (!tag.equals(other.tag))
			return false;
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
			return false;
		return true;
	}
	
	
}
