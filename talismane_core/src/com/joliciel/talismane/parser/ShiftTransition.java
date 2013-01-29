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

import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * Shift Buffer[0] over to Stack[0], without creating a new dependency.
 * @author Assaf Urieli.
 *
 */
public class ShiftTransition extends AbstractTransition implements Transition {
	private static final Log LOG = LogFactory.getLog(ShiftTransition.class);
	private static String name = "Shift";
	
	public ShiftTransition() {
		super();
	}

	@Override
	protected void applyInternal(ParseConfiguration configuration) {
		PosTaggedToken element = configuration.getBuffer().removeFirst();
		configuration.getStack().push(element);
	}


	@Override
	public boolean checkPreconditions(ParseConfiguration configuration) {
		if (configuration.getBuffer().isEmpty()) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Cannot apply " + this.toString() + ": buffer is empty");
			}
			return false;
		}
		return true;
	}

	public String getCode() {
		return name;
	}


	@Override
	public boolean doesReduce() {
		return false;
	}
}
