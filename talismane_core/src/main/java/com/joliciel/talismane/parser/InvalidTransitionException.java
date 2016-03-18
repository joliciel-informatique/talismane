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
package com.joliciel.talismane.parser;

import com.joliciel.talismane.TalismaneException;

public class InvalidTransitionException extends TalismaneException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -590792696834580845L;
	private Transition transition;
	private ParseConfiguration configuration;
	
	public InvalidTransitionException(Transition transition,
			ParseConfiguration configuration) {
		super(transition.getCode() + ": " + configuration.toString());
		this.transition = transition;
		this.configuration = configuration;
	}
	
	public InvalidTransitionException(Transition transition,
			ParseConfiguration configuration, String message) {
		super(message);
		this.transition = transition;
		this.configuration = configuration;
	}

	public Transition getTransition() {
		return transition;
	}

	public void setTransition(Transition transition) {
		this.transition = transition;
	}

	public ParseConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(ParseConfiguration configuration) {
		this.configuration = configuration;
	}
	
	
}
