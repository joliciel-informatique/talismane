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
package com.joliciel.talismane.posTagger.evaluate;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.output.PosTagSequenceProcessor;
import com.typesafe.config.Config;

/**
 * An interface that observes a pos-tagger evaluation while its occurring.
 * 
 * @author Assaf Urieli
 *
 */
public interface PosTagEvaluationObserver {
  /**
   * Called when the next pos-tag sequence has been processed.
   * 
   * @throws TalismaneException
   * @throws IOException
   */
  public void onNextPosTagSequence(PosTagSequence realSequence, List<PosTagSequence> guessedSequences) throws TalismaneException, IOException;

  public void onEvaluationComplete() throws IOException;

  /**
   * Collect the observers specified in the configuration key
   * talismane.core.pos-tagger.evaluate.observers.<br/>
   * <br/>
   * Each processor must implement this interface and must have a constructor
   * matching one of the following signatures:<br/>
   * - ( {@link File} outputDir, {@link TalismaneSession} session)<br/>
   * - ( {@link TalismaneSession} session)<br/>
   * <br/>
   * 
   * @param outDir
   *          directory in which to write the various outputs
   * @param session
   *          to read the configuration
   * @return
   * @throws IOException
   * @throws TalismaneException
   *           if an observer does not implement this interface, or if no
   *           constructor is found with the correct signature
   */
  public static List<PosTagEvaluationObserver> getObservers(File outDir, TalismaneSession session)
      throws IOException, ClassNotFoundException, ReflectiveOperationException, TalismaneException {
    if (outDir != null)
      outDir.mkdirs();

    Config config = session.getConfig();
    Config posTaggerConfig = config.getConfig("talismane.core.pos-tagger");
    Config evalConfig = posTaggerConfig.getConfig("evaluate");

    List<PosTagEvaluationObserver> observers = new ArrayList<>();

    List<PosTagSequenceProcessor> processors = PosTagSequenceProcessor.getProcessors(null, outDir, session);
    for (PosTagSequenceProcessor processor : processors) {
      PosTagSequenceProcessorWrapper wrapper = new PosTagSequenceProcessorWrapper(processor);
      observers.add(wrapper);
    }

    List<String> classes = evalConfig.getStringList("observers");
    if (outDir != null)
      outDir.mkdirs();

    for (String className : classes) {
      @SuppressWarnings("rawtypes")
      Class untypedClass = Class.forName(className);
      if (!PosTagEvaluationObserver.class.isAssignableFrom(untypedClass))
        throw new TalismaneException("Class " + className + " does not implement interface " + PosTagEvaluationObserver.class.getSimpleName());

      @SuppressWarnings("unchecked")
      Class<? extends PosTagEvaluationObserver> clazz = untypedClass;

      Constructor<? extends PosTagEvaluationObserver> cons = null;
      PosTagEvaluationObserver observer = null;

      if (cons == null) {
        try {
          cons = clazz.getConstructor(File.class, TalismaneSession.class);
        } catch (NoSuchMethodException e) {
          // do nothing
        }
        if (cons != null) {
          observer = cons.newInstance(outDir, session);
        }
      }
      if (cons == null) {
        try {
          cons = clazz.getConstructor(TalismaneSession.class);
        } catch (NoSuchMethodException e) {
          // do nothing
        }
        if (cons != null) {
          observer = cons.newInstance(session);
        } else {
          throw new TalismaneException("No constructor found with correct signature for: " + className);
        }
      }

      observers.add(observer);
    }

    return observers;
  }

}
