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
 * Create a dependency where Buffer[0] depends on Stack[0], remove Buffer[0], and put Stack[0] in its place.
 * @author Assaf Urieli
 *
 */
public class RightArcTransition extends AbstractTransition implements Transition {
	private static final Log LOG = LogFactory.getLog(RightArcTransition.class);
	private String label;
	private String name;
	
	public RightArcTransition(String label) {
		super();
		this.label = label;
	}

	@Override
	protected void applyInternal(ParseConfiguration configuration) {
		PosTaggedToken head = configuration.getStack().pop();
		PosTaggedToken dependent = configuration.getBuffer().pollFirst();
		configuration.getBuffer().addFirst(head);
		configuration.addDependency(head, dependent, label, this);
	}

	@Override
	public boolean checkPreconditions(ParseConfiguration configuration) {
		if (configuration.getBuffer().isEmpty() || configuration.getStack().isEmpty()) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Cannot apply " + this.toString() + ": buffer or stack is empty");
			}
			return false;
		}
		return true;
	}

	@Override
	public String getCode() {
		if (this.name==null) {
			this.name = "RightArc";
			if (this.label!=null && this.label.length()>0)
				this.name += "[" + this.label + "]";
		}
		
		return this.name;
	}


	@Override
	public boolean doesReduce() {
		return true;
	}
}
