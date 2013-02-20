///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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
package com.joliciel.talismane.trainer.fr;

import java.util.HashMap;
import java.util.Map;

import com.joliciel.talismane.TalismaneException;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		Map<String, String> argMap = new HashMap<String, String>();
		
		for (String arg : args) {
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos+1);
			argMap.put(argName, argValue);
		}
		
		String trainerName = argMap.get("trainer");
		argMap.remove("trainer");
		
		if (trainerName.equalsIgnoreCase("sentenceDetector")) {
			@SuppressWarnings("unused")
			SentenceDetectorMaxentRunner maxentRunner = new SentenceDetectorMaxentRunner(argMap);
		} else if (trainerName.equalsIgnoreCase("tokeniser")) {
			@SuppressWarnings("unused")
			TokeniserMaxentRunner maxentRunner = new TokeniserMaxentRunner(argMap);
		} else if (trainerName.equalsIgnoreCase("posTagger")) {
			@SuppressWarnings("unused")
			PosTaggerMaxentRunner maxentRunner = new PosTaggerMaxentRunner(argMap);
		} else if (trainerName.equalsIgnoreCase("parser")) {
			@SuppressWarnings("unused")
			ParserMaxentRunner maxentRunner = new ParserMaxentRunner(argMap);
		} else {
			throw new TalismaneException("Unknown trainer: " + trainerName);
		}
	}
}
