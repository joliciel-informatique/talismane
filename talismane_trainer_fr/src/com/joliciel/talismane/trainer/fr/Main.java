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

import java.io.FileInputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.TalismaneException;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		Map<String, String> argMap = TalismaneConfig.convertArgs(args);

		String logConfigPath = argMap.get("logConfigFile");
		if (logConfigPath!=null) {
			argMap.remove("logConfigFile");
			Properties props = new Properties();
			props.load(new FileInputStream(logConfigPath));
			PropertyConfigurator.configure(props);
		}
		
		String trainerName = argMap.get("trainer");
		argMap.remove("trainer");
		
		if (trainerName.equalsIgnoreCase("sentenceDetector")) {
			@SuppressWarnings("unused")
			SentenceDetectorTrainer maxentRunner = new SentenceDetectorTrainer(argMap);
		} else if (trainerName.equalsIgnoreCase("tokeniser")) {
			@SuppressWarnings("unused")
			TokeniserTrainer maxentRunner = new TokeniserTrainer(argMap);
		} else if (trainerName.equalsIgnoreCase("posTagger")) {
			@SuppressWarnings("unused")
			PosTaggerTrainer maxentRunner = new PosTaggerTrainer(argMap);
		} else if (trainerName.equalsIgnoreCase("parser")) {
			@SuppressWarnings("unused")
			ParserTrainer maxentRunner = new ParserTrainer(argMap);
		} else {
			throw new TalismaneException("Unknown trainer: " + trainerName);
		}
	}
}
