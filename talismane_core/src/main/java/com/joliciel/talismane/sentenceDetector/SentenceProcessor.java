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
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.output.FreemarkerTemplateWriter;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.utils.ConfigUtils;
import com.typesafe.config.Config;

/**
 * Any class that can process sentences found by a sentence detector.
 * 
 * @author Assaf Urieli
 *
 */
public interface SentenceProcessor extends Closeable {
	/**
	 * Process the next sentence.
	 */
	public void onNextSentence(Sentence sentence);

	/**
	 * 
	 * @param writer
	 *            if provided, the main processor will write to this writer, if
	 *            null, the outDir will be used instead
	 * @param outDir
	 * @param session
	 * @return
	 * @throws IOException
	 */
	public static List<SentenceProcessor> getProcessors(Writer writer, File outDir, TalismaneSession session) throws IOException {
		List<SentenceProcessor> sentenceProcessors = new ArrayList<>();
		Config config = session.getConfig();
		Reader templateReader = null;
		String configPath = "talismane.core.sentence-detector.output.template";
		if (config.hasPath(configPath)) {
			templateReader = new BufferedReader(new InputStreamReader(ConfigUtils.getFileFromConfig(config, configPath)));
		} else {
			String sentenceTemplateName = "sentence_template.ftl";
			String path = "output/" + sentenceTemplateName;
			InputStream inputStream = Talismane.class.getResourceAsStream(path);
			if (inputStream == null)
				throw new IOException("Resource not found in classpath: " + path);
			templateReader = new BufferedReader(new InputStreamReader(inputStream));

		}
		if (writer == null) {
			File file = new File(outDir, session.getBaseName() + "_sent.txt");
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), session.getOutputCharset()));
		}
		FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(templateReader, writer);
		SentenceProcessor sentenceProcessor = templateWriter;
		sentenceProcessors.add(sentenceProcessor);
		return sentenceProcessors;
	}
}
