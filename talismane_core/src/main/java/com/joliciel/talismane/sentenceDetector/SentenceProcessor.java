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
package com.joliciel.talismane.sentenceDetector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;

import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.output.FreemarkerTemplateWriter;
import com.joliciel.talismane.utils.ConfigUtils;
import com.typesafe.config.Config;

/**
 * Any class that can process sentences found by a sentence detector.
 * 
 * @author Assaf Urieli
 *
 */
public interface SentenceProcessor {
	/**
	 * Process the next sentence, outputting to the writer provided.
	 */
	public void onNextSentence(Sentence sentence, Writer writer);

	public static SentenceProcessor getProcessor(TalismaneSession session) throws IOException {
		Config config = session.getConfig();
		Reader templateReader = null;
		String configPath = "talismane.core.sentence-detector.template";
		if (config.hasPath(configPath)) {
			templateReader = new BufferedReader(new InputStreamReader(ConfigUtils.getFileFromConfig(config, configPath)));
		} else {
			String sentenceTemplateName = "sentence_template.ftl";
			String path = "output/" + sentenceTemplateName;
			InputStream inputStream = Talismane.class.getResourceAsStream(path);
			if (inputStream == null)
				throw new TalismaneException("Resource not found in classpath: " + path);
			templateReader = new BufferedReader(new InputStreamReader(inputStream));

		}
		FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(templateReader);
		SentenceProcessor sentenceProcessor = templateWriter;
		return sentenceProcessor;
	}
}
