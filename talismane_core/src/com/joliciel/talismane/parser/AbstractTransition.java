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

public abstract class AbstractTransition implements Transition {
	private static final Log LOG = LogFactory.getLog(AbstractTransition.class);
	private ParserServiceInternal parserServiceInternal;
	
	private double probLog = 0;
	private boolean probLogCalculated = false;
	private double probability = 0;

	@Override
	public void apply(ParseConfiguration configuration) {
		if (this.checkPreconditions(configuration)) {
			if (LOG.isTraceEnabled())
				LOG.trace("Applying " + this.getName());
			this.applyInternal(configuration);
			configuration.getTransitions().add(this);
		}
		else
			throw new InvalidTransitionException(this, configuration);
	}

	protected abstract void applyInternal(ParseConfiguration configuration);


	@Override
	public double getProbability() {
		return this.probability;
	}

	@Override
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


	public ParserServiceInternal getParserServiceInternal() {
		return parserServiceInternal;
	}

	public void setParserServiceInternal(ParserServiceInternal parserServiceInternal) {
		this.parserServiceInternal = parserServiceInternal;
	}
}
