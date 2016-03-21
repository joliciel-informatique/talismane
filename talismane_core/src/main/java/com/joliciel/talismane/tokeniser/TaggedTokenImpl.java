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
package com.joliciel.talismane.tokeniser;

import java.text.DecimalFormat;

import com.joliciel.talismane.machineLearning.Decision;

public class TaggedTokenImpl<T extends TokenTag> implements TaggedToken<T> {
	private static final DecimalFormat df = new DecimalFormat("0.0000");
	private Token token = null;
	private T tag = null;
	private Decision decision = null;
	
	protected TaggedTokenImpl(TaggedTokenImpl<T> taggedTokenToClone) {
		this.token = taggedTokenToClone.token;
		this.tag = taggedTokenToClone.tag;
		this.decision = taggedTokenToClone.decision;
	}
	
	public TaggedTokenImpl(Token token, Decision decision, T tag) {
		this.token = token;
		this.decision = decision;
		this.tag = tag;
	}
	
	public Token getToken() {
		return token;
	}
	
	public void setToken(Token token) {
		this.token = token;
	}

	public T getTag() {
		return tag;
	}

	public void setTag(T tag) {
		this.tag = tag;
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
		if (this.getDecision().getProbability()>=o.getDecision().getProbability())
			return -1;
		return 1;
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

	public Decision getDecision() {
		return decision;
	}
	
	@Override
	public double getProbability() {
		double prob = 1;
		if (decision!=null)
			prob = decision.getProbability();
		return prob;
	}

	@Override
	public String toString() {
		return this.getToken().getText() + "|" + this.getTag() + "|" + this.getToken().getIndex() + "| prob=" + df.format(this.getDecision().getProbability());
	}

}
