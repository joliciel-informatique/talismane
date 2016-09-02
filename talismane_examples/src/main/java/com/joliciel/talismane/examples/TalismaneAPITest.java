///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Joliciel Informatique
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
package com.joliciel.talismane.examples;

import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.parser.DependencyNode;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.Parser;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * A class showing how to analyse a sentence using the Talismane API and an
 * existing language pack.<br/>
 * 
 * Usage (barring the classpath, which must include Talismane jars):<br/>
 * 
 * <pre>
 * java -Dconfig.file=[languagePackConfigFile] com.joliciel.talismane.examples.TalismaneAPITest
 * </pre>
 */
public class TalismaneAPITest {

	public static void main(String[] args) throws Exception {
		String text = "Les amoureux qui se bécotent sur les bancs publics ont des petites gueules bien sympathiques.";

		// arbitrary session id
		String sessionId = "";

		// load the Talismane configuration
		TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance(sessionId);
		TalismaneService talismaneService = talismaneServiceLocator.getTalismaneService();

		Config conf = ConfigFactory.load();
		TalismaneConfig talismaneConfig = talismaneService.getTalismaneConfig(conf);

		// tokenise the text
		Tokeniser tokeniser = talismaneConfig.getTokeniser();
		TokenSequence tokenSequence = tokeniser.tokeniseText(text);

		// pos-tag the token sequence
		PosTagger posTagger = talismaneConfig.getPosTagger();
		PosTagSequence posTagSequence = posTagger.tagSentence(tokenSequence);
		System.out.println(posTagSequence);

		// parse the pos-tag sequence
		Parser parser = talismaneConfig.getParser();
		ParseConfiguration parseConfiguration = parser.parseSentence(posTagSequence);
		DependencyNode dependencyNode = parseConfiguration.getParseTree();
		System.out.println(dependencyNode);
	}

}
