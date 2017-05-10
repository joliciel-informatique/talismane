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
package com.joliciel.talismane.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * Writes the list of transitions that were actually applied, one at a time.
 */
public class TransitionLogWriter implements ParseConfigurationProcessor {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(TransitionLogWriter.class);

  private final Writer csvFileWriter;

  public TransitionLogWriter(File outDir, TalismaneSession session) throws IOException {
    File csvFile = new File(outDir, session.getBaseName() + "_transitions.csv");
    csvFile.delete();
    csvFile.createNewFile();

    this.csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), session.getCsvCharset()));
  }

  @Override
  public void onNextParseConfiguration(ParseConfiguration parseConfiguration) throws TalismaneException, IOException {
    ParseConfiguration currentConfiguration = new ParseConfiguration(parseConfiguration.getPosTagSequence());

    csvFileWriter.write("\n");
    csvFileWriter.write("\t" + this.getTopOfStack(currentConfiguration) + "\t" + this.getTopOfBuffer(currentConfiguration) + "\t" + "\n");
    Set<DependencyArc> dependencies = new HashSet<DependencyArc>();
    for (Transition transition : parseConfiguration.getTransitions()) {
      currentConfiguration = new ParseConfiguration(currentConfiguration);
      transition.apply(currentConfiguration);
      DependencyArc newDep = null;
      if (currentConfiguration.getDependencies().size() > dependencies.size()) {
        for (DependencyArc arc : currentConfiguration.getDependencies()) {
          if (dependencies.contains(arc)) {
            continue;
          } else {
            dependencies.add(arc);
            newDep = arc;
            break;
          }
        }
      }
      String newDepText = "";
      if (newDep != null) {
        newDepText = newDep.getLabel() + "[" + newDep.getHead().getToken().getOriginalText().replace(' ', '_') + "|" + newDep.getHead().getTag().getCode() + ","
            + newDep.getDependent().getToken().getOriginalText().replace(' ', '_') + "|" + newDep.getDependent().getTag().getCode() + "]";
      }
      csvFileWriter.write(
          transition.getCode() + "\t" + this.getTopOfStack(currentConfiguration) + "\t" + this.getTopOfBuffer(currentConfiguration) + "\t" + newDepText + "\n");
    }
    csvFileWriter.flush();
  }

  private String getTopOfStack(ParseConfiguration configuration) {
    StringBuilder sb = new StringBuilder();
    Iterator<PosTaggedToken> stackIterator = configuration.getStack().iterator();
    int i = 0;
    while (stackIterator.hasNext()) {
      if (i == 5) {
        sb.insert(0, "... ");
        break;
      }

      PosTaggedToken token = stackIterator.next();
      sb.insert(0, token.getToken().getOriginalText().replace(' ', '_') + "|" + token.getTag().getCode() + " ");
      i++;
    }
    return sb.toString();
  }

  private String getTopOfBuffer(ParseConfiguration configuration) {
    StringBuilder sb = new StringBuilder();
    Iterator<PosTaggedToken> bufferIterator = configuration.getBuffer().iterator();
    int i = 0;
    while (bufferIterator.hasNext()) {
      if (i == 5) {
        sb.append(" ...");
        break;
      }

      PosTaggedToken token = bufferIterator.next();
      sb.append(" " + token.getToken().getOriginalText().replace(' ', '_') + "|" + token.getTag().getCode());
      i++;

    }
    return sb.toString();
  }

  @Override
  public void onCompleteParse() throws IOException {
    this.csvFileWriter.flush();
  }

  @Override
  public void close() throws IOException {
    this.csvFileWriter.close();
  }

  protected Writer getWriter() {
    return csvFileWriter;
  }

}
