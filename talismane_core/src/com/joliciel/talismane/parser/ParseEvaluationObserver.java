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

import java.util.List;

import com.joliciel.talismane.posTagger.PosTagSequence;


/**
 * An interface that observes a parsing evaluation while its occurring.
 * @author Assaf Urieli
 *
 */
public interface ParseEvaluationObserver {

	/**
	 * Called before parsing begins
	 */
	public void onParseStart(ParseConfiguration realConfiguration, List<PosTagSequence> posTagSequences);
	
	/**
	 * Called when the next parse configuration has been processed.
	 */
	public void onParseEnd(ParseConfiguration realConfiguration, List<ParseConfiguration> guessedConfigurations);
	
	/**
	 * Called when full evaluation has completed.
	 */
	public void onEvaluationComplete();
}
