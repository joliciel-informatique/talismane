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

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.corpus.CorpusLine;
import com.joliciel.talismane.corpus.CorpusLine.CorpusElement;
import com.joliciel.talismane.corpus.CorpusLineReader;
import com.joliciel.talismane.posTagger.PosTagRegexBasedCorpusReader;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.tokeniser.TokenRegexBasedCorpusReader;
import com.typesafe.config.Config;

/**
 * A corpus reader that expects one pos-tagged token with dependency info per
 * line, and analyses the line content based on a regex supplied during
 * construction, via a {@link CorpusLineReader}.<br/>
 * 
 * The following placeholders are required:<br/>
 * {@link CorpusElement#INDEX}, {@link CorpusElement#TOKEN},
 * {@link CorpusElement#POSTAG}, {@link CorpusElement#LABEL},
 * {@link CorpusElement#GOVERNOR}. <br/>
 * These are included surrounded by % signs on both sides, and without the
 * prefix "CorpusElement."<br/>
 * 
 * Example (note that the regex is applied to one line, so no endline is
 * necessary):
 * 
 * <pre>
 * %INDEX%\t%TOKEN%\t.*\t%POSTAG%\t.*\t.*\t%NON_PROJ_GOVERNOR%\t%NON_PROJ_LABEL%\t%GOVERNOR%\t%LABEL%
 * </pre>
 * 
 * @author Assaf Urieli
 *
 */
public class ParserRegexBasedCorpusReader extends PosTagRegexBasedCorpusReader implements ParserAnnotatedCorpusReader {
  private static final Logger LOG = LoggerFactory.getLogger(ParserRegexBasedCorpusReader.class);

  private ParseConfiguration configuration = null;

  private final boolean predictTransitions;

  /**
   * In addition to the values read in
   * {@link TokenRegexBasedCorpusReader#TokenRegexBasedCorpusReader(Reader, Config, TalismaneSession)}
   * , reads the following setting from the config:<br/>
   * - predict-transitions: whether or not an attempt should be made to predict
   * transitions<br/>
   * 
   * @throws TalismaneException
   */
  public ParserRegexBasedCorpusReader(Reader reader, Config config, TalismaneSession session) throws IOException, TalismaneException {
    super(reader, config, session);
    this.predictTransitions = config.getBoolean("predict-transitions");
  }

  @Override
  protected CorpusElement[] getRequiredElements() {
    return new CorpusElement[] { CorpusElement.INDEX, CorpusElement.TOKEN, CorpusElement.POSTAG, CorpusElement.LABEL, CorpusElement.GOVERNOR };
  }

  @Override
  protected void processSentence(List<CorpusLine> corpusLines) throws TalismaneException, IOException {
    try {
      super.processSentence(corpusLines);
      PosTaggedToken rootToken = posTagSequence.prependRoot();
      idTokenMap.put(0, rootToken);

      TransitionSystem transitionSystem = session.getTransitionSystem();
      Set<DependencyArc> dependencies = new TreeSet<DependencyArc>();
      for (CorpusLine dataLine : corpusLines) {
        int headIndex = 0;
        if (dataLine.hasElement(CorpusElement.GOVERNOR))
          headIndex = Integer.parseInt(dataLine.getElement(CorpusElement.GOVERNOR));
        PosTaggedToken head = idTokenMap.get(headIndex);
        PosTaggedToken dependent = idTokenMap.get(dataLine.getIndex());

        String dependencyLabel = dataLine.getElement(CorpusElement.LABEL);
        if (transitionSystem.getDependencyLabels().size() > 1) {
          if (dependencyLabel.length() > 0 && !transitionSystem.getDependencyLabels().contains(dependencyLabel)) {
            throw new UnknownDependencyLabelException((this.getCurrentFile() == null ? "" : this.getCurrentFile().getPath()), dataLine.getLineNumber(),
                dependencyLabel);
          }

          String nonProjectiveLabel = dataLine.getElement(CorpusElement.NON_PROJ_LABEL);
          if (nonProjectiveLabel != null && nonProjectiveLabel.length() > 0 && !transitionSystem.getDependencyLabels().contains(nonProjectiveLabel)) {
            throw new UnknownDependencyLabelException((this.getCurrentFile() == null ? "" : this.getCurrentFile().getPath()), dataLine.getLineNumber(),
                nonProjectiveLabel);
          }

        }
        DependencyArc arc = new DependencyArc(head, dependent, dependencyLabel);
        if (LOG.isTraceEnabled())
          LOG.trace(arc.toString());
        dependencies.add(arc);
        if (dataLine.hasElement(CorpusElement.DEP_COMMENT))
          arc.setComment(dataLine.getElement(CorpusElement.DEP_COMMENT));
      }

      configuration = new ParseConfiguration(posTagSequence);
      if (this.predictTransitions) {
        transitionSystem.predictTransitions(configuration, dependencies);
      } else {
        for (DependencyArc arc : dependencies) {
          configuration.addDependency(arc.getHead(), arc.getDependent(), arc.getLabel(), null);
        }
      }

      // Add manual non-projective dependencies,
      // if there are any
      if (this.getCorpusLineReader().hasPlaceholder(CorpusElement.NON_PROJ_GOVERNOR)) {
        Set<DependencyArc> nonProjDeps = new TreeSet<DependencyArc>();
        if (LOG.isTraceEnabled())
          LOG.trace("Non projective dependencies: ");

        for (CorpusLine dataLine : corpusLines) {
          int headIndex = 0;
          if (dataLine.hasElement(CorpusElement.NON_PROJ_GOVERNOR))
            headIndex = Integer.parseInt(dataLine.getElement(CorpusElement.NON_PROJ_GOVERNOR));

          PosTaggedToken head = idTokenMap.get(headIndex);
          PosTaggedToken dependent = idTokenMap.get(dataLine.getIndex());
          DependencyArc nonProjArc = new DependencyArc(head, dependent, dataLine.getElement(CorpusElement.NON_PROJ_LABEL));
          if (LOG.isTraceEnabled())
            LOG.trace(nonProjArc.toString());
          nonProjDeps.add(nonProjArc);
          if (dataLine.hasElement(CorpusElement.DEP_COMMENT))
            nonProjArc.setComment(dataLine.getElement(CorpusElement.DEP_COMMENT));
        }

        for (DependencyArc nonProjArc : nonProjDeps) {
          configuration.addManualNonProjectiveDependency(nonProjArc.getHead(), nonProjArc.getDependent(), nonProjArc.getLabel());
        }
      }
    } catch (TalismaneException e) {
      this.clearSentence();
      throw e;
    }
  }

  /**
   * Returns true if the data line is valid, false otherwise.
   */
  protected boolean checkDataLine(CorpusLine dataLine) {
    return true;
  }

  /**
   * Updates the data line prior to processing. At this point, empty lines may
   * have been added to correspond to empty tokens that were added by filters.
   */
  protected void updateDataLine(List<CorpusLine> dataLines, int index) {
    // nothing to do in the base class
  }

  @Override
  public ParseConfiguration nextConfiguration() throws TalismaneException, IOException {
    ParseConfiguration nextConfiguration = null;
    if (this.hasNextSentence()) {
      nextConfiguration = configuration;
      this.clearSentence();
    }
    return nextConfiguration;
  }

  @Override
  protected void clearSentence() {
    super.clearSentence();
    this.configuration = null;
  }

  @Override
  public Map<String, String> getCharacteristics() {
    Map<String, String> attributes = super.getCharacteristics();
    attributes.put("transitionSystem", session.getTransitionSystem().getClass().getSimpleName());

    return attributes;
  }

  protected String readWord(String rawWord) {
    return session.getCoNLLFormatter().fromCoNLL(rawWord);
  }

  /**
   * Should an attempt be made to the predict the transitions that led to this
   * configuration, or should dependencies simply be added with null
   * transitions.
   */
  public boolean isPredictTransitions() {
    return predictTransitions;
  }
}
