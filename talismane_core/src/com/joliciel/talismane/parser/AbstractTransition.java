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
package com.joliciel.talismane.parser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.Decision;

public abstract class AbstractTransition implements Transition {
	private static final Log LOG = LogFactory.getLog(AbstractTransition.class);
	private Decision<Transition> decision;

	@Override
	public void apply(ParseConfiguration configuration) {
		if (this.checkPreconditions(configuration)) {
			if (LOG.isTraceEnabled())
				LOG.trace("Applying " + this.getCode());
			this.applyInternal(configuration);
			configuration.getTransitions().add(this);
		}
		else
			throw new InvalidTransitionException(this, configuration);
	}

	protected abstract void applyInternal(ParseConfiguration configuration);

	public Decision<Transition> getDecision() {
		return decision;
	}

	public void setDecision(Decision<Transition> decision) {
		this.decision = decision;
	}
	
	@Override
	public String toString() {
		return this.getCode();
	}

	@Override
	public int compareTo(Transition o) {
		return this.getCode().compareTo(o.getCode());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.getCode() == null) ? 0 : this.getCode().hashCode());
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
		AbstractTransition other = (AbstractTransition) obj;
		if (this.getCode() == null) {
			if (other.getCode() != null)
				return false;
		} else if (!this.getCode().equals(other.getCode()))
			return false;
		return true;
	}
	
	
}
