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

import java.io.File;
import java.io.Serializable;
import java.util.Set;

/**
 * An interface used to constrain the parsing, based on information found in the training corpus.
 * @author Assaf Urieli
 *
 */
public interface ParsingConstrainer extends ParseConfigurationProcessor, Serializable {
	/**
	 * Get a list of transitions that co-occur with configurations similar to this one
	 * in the training corpus.
	 * @param configuration
	 * @return
	 */
	public Set<Transition> getPossibleTransitions(ParseConfiguration configuration);
	
	/**
	 * Get the transition system used by this constrainer.
	 * @return
	 */
	public TransitionSystem getTransitionSystem();
	
	/**
	 * A file for serializing this constrainer.
	 * @param writer
	 */
	public void setFile(File file);
}
