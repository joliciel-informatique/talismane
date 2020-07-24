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
package com.joliciel.talismane.parser.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.joliciel.talismane.Talismane;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.Transition;
import com.joliciel.talismane.parser.evaluate.ParseEvaluationObserver;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.typesafe.config.Config;

/**
 * Writes the list of transitions that were actually applied, one at a time.
 */
public class TransitionLogWriter implements ParseConfigurationProcessor, ParseEvaluationObserver {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(TransitionLogWriter.class);

  private final Writer writer;

  private final Set<String> errorLabels;

  public TransitionLogWriter(File outDir, String sessionId) throws IOException {
    File csvFile = new File(outDir, TalismaneSession.get(sessionId).getBaseName() + "_transitions.csv");
    csvFile.delete();
    csvFile.createNewFile();

    this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), TalismaneSession.get(sessionId).getCsvCharset()));

    Config config = ConfigFactory.load();
    Config parserConfig = config.getConfig("talismane.core." + sessionId + ".parser");
    Config evalConfig = parserConfig.getConfig("evaluate");
    this.errorLabels = new HashSet<>(evalConfig.getStringList("error-labels"));
  }

  @Override
  public void onNextParseConfiguration(ParseConfiguration parseConfiguration) throws TalismaneException, IOException {
    ParseConfiguration currentConfiguration = new ParseConfiguration(parseConfiguration.getPosTagSequence());

    writer.write("\n");
    writer.write("\t" + this.getTopOfStack(currentConfiguration) + "\t" + this.getTopOfBuffer(currentConfiguration) + "\t" + "\n");
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
      writer.write(
          transition.getCode() + "\t" + this.getTopOfStack(currentConfiguration) + "\t" + this.getTopOfBuffer(currentConfiguration) + "\t" + newDepText + "\n");
    }
    writer.flush();
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
    this.writer.flush();
  }

  @Override
  public void close() throws IOException {
    this.writer.close();
  }

  protected Writer getWriter() {
    return writer;
  }

  @Override
  public void onParseStart(ParseConfiguration realConfiguration, List<PosTagSequence> posTagSequences) {
  }

  @Override
  public void onParseEnd(ParseConfiguration refConfiguration, List<ParseConfiguration> guessedConfigurations) throws TalismaneException, IOException {
    boolean includeMe = true;
    if (errorLabels != null && errorLabels.size() > 0) {
      includeMe = false;
      int i = 0;
      ParseConfiguration guessConfiguration = guessedConfigurations.get(0);

      Set<PosTaggedToken> refTokensToExplain = new HashSet<PosTaggedToken>();
      Set<PosTaggedToken> guessTokensToExplain = new HashSet<PosTaggedToken>();
      Set<PosTaggedToken> refTokensToHighlight = new HashSet<PosTaggedToken>();
      Set<PosTaggedToken> guessTokensToHighlight = new HashSet<PosTaggedToken>();
      for (PosTaggedToken refToken : refConfiguration.getPosTagSequence()) {
        if (i != 0) {
          DependencyArc refArc = refConfiguration.getGoverningDependency(refToken);
          if (refArc != null) {
            PosTaggedToken guessToken = guessConfiguration.getPosTagSequence().get(i);
            if (errorLabels.contains(refArc.getLabel())) {
              DependencyArc guessArc = guessConfiguration.getGoverningDependency(guessToken);
              if (guessArc == null || !refArc.getLabel().equals(guessArc.getLabel()) || (refArc.getHead() == null && guessArc.getHead() != null)
                  || (refArc.getHead() != null && guessArc.getHead() == null) || refArc.getHead().getIndex() != guessArc.getHead().getIndex()) {
                refTokensToExplain.add(refToken);
                if (refArc.getHead() != null)
                  refTokensToHighlight.add(refArc.getHead());
                guessTokensToExplain.add(guessToken);
                if (guessArc != null && guessArc.getHead() != null)
                  guessTokensToHighlight.add(guessArc.getHead());
                includeMe = true;
              }
            }
          } // have refArc
        }
        i++;
      }

      StringBuilder refBuilder = new StringBuilder();
      for (PosTaggedToken refToken : refConfiguration.getPosTagSequence()) {
        if (refTokensToExplain.contains(refToken)) {
          DependencyArc refArc = refConfiguration.getGoverningDependency(refToken);
          if (refArc == null)
            refBuilder.append(
                "#" + refToken.getToken().getOriginalText().replace(' ', '_') + "|" + refToken.getTag().getCode() + "|" + refToken.getIndex() + "|Gov0|null# ");
          else
            refBuilder.append("#" + refToken.getToken().getOriginalText().replace(' ', '_') + "|" + refToken.getTag().getCode() + "|" + refToken.getIndex()
                + "|Gov" + (refArc.getHead() == null ? 0 : refArc.getHead().getIndex()) + "|" + refArc.getLabel() + "# ");
        } else if (refTokensToHighlight.contains(refToken)) {
          refBuilder
              .append("#" + refToken.getToken().getOriginalText().replace(' ', '_') + "|" + refToken.getTag().getCode() + "|" + refToken.getIndex() + "# ");
        } else {
          refBuilder.append(refToken.getToken().getOriginalText().replace(' ', '_') + "|" + refToken.getTag().getCode() + "|" + refToken.getIndex() + " ");
        }
      }
      StringBuilder guessBuilder = new StringBuilder();
      for (PosTaggedToken guessToken : guessConfiguration.getPosTagSequence()) {
        if (guessTokensToExplain.contains(guessToken)) {
          DependencyArc guessArc = guessConfiguration.getGoverningDependency(guessToken);
          if (guessArc == null)
            guessBuilder.append("#" + guessToken.getToken().getOriginalText().replace(' ', '_') + "|" + guessToken.getTag().getCode() + "|"
                + guessToken.getIndex() + "|Gov0|null# ");
          else
            guessBuilder.append("#" + guessToken.getToken().getOriginalText().replace(' ', '_') + "|" + guessToken.getTag().getCode() + "|"
                + guessToken.getIndex() + "|Gov" + (guessArc.getHead() == null ? 0 : guessArc.getHead().getIndex()) + "|" + guessArc.getLabel() + "# ");
        } else if (guessTokensToHighlight.contains(guessToken)) {
          guessBuilder.append(
              "#" + guessToken.getToken().getOriginalText().replace(' ', '_') + "|" + guessToken.getTag().getCode() + "|" + guessToken.getIndex() + "# ");
        } else {
          guessBuilder
              .append(guessToken.getToken().getOriginalText().replace(' ', '_') + "|" + guessToken.getTag().getCode() + "|" + guessToken.getIndex() + " ");
        }
      }
      if (includeMe) {
        writer.write("\n");
        writer.write(refBuilder.toString() + "\n");
        writer.write(guessBuilder.toString() + "\n");
      }
    }
    if (includeMe)
      this.onNextParseConfiguration(guessedConfigurations.get(0));
  }

  @Override
  public void onEvaluationComplete() throws IOException {
    this.onCompleteParse();
  }

}
