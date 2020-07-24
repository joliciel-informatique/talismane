///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2017 Joliciel Informatique
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
package com.joliciel.talismane.extensions.standoff;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.parser.output.FreemarkerParseWriter;

/**
 * Writes standoff sentences readable by the Brat annotation tool: see
 * http://brat.nlplab.org/
 * 
 * To be useable by Brat, these need to be accompanied in the same directory by
 * annotations written using the {@link StandoffWriter}.
 * 
 * @author Assaf Urieli
 *
 */
public class StandoffSentenceWriter extends FreemarkerParseWriter {

  public StandoffSentenceWriter(File outDir, String sessionId) throws IOException, TalismaneException {
    this(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outDir, TalismaneSession.get(sessionId).getBaseName() + ".txt"), false),
        TalismaneSession.get(sessionId).getOutputCharset())),
        sessionId);
  }

  public StandoffSentenceWriter(Writer writer, String sessionId) throws IOException, TalismaneException {
    super(new BufferedReader(new InputStreamReader(StandoffSentenceWriter.class.getResourceAsStream("standoffSentences.ftl"))), writer, sessionId);
  }
}
