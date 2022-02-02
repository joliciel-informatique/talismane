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
package com.joliciel.talismane.parser.evaluate;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.output.ParseConfigurationProcessor;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * An interface that observes a parsing evaluation while its occurring.
 * 
 * @author Assaf Urieli
 *
 */
public interface ParseEvaluationObserver {

  /**
   * Called before parsing begins
   */
  public void onParseStart(ParseConfiguration realConfiguration, List<PosTagSequence> posTagSequences);

  /**
   * Called when the next parse configuration has been processed.
   * 
   * @throws TalismaneException
   * @throws IOException
   */
  public void onParseEnd(ParseConfiguration realConfiguration, List<ParseConfiguration> guessedConfigurations) throws TalismaneException, IOException;

  /**
   * Called when full evaluation has completed.
   * 
   * @throws IOException
   */
  public void onEvaluationComplete() throws IOException;

  /**
   * Collect the observers specified in the configuration key
   * talismane.core.[sessionId].parser.evaluate.observers.<br>
   * <br>
   * Each processor must implement this interface and must have a constructor
   * matching one of the following signatures:<br>
   * - ( {@link File} outputDir, {@link String} sessionId)<br>
   * - ( {@link String} sessionId)<br>
   * <br>
   * 
   * @param outDir
   *          directory in which to write the various outputs
   * @return
   * @throws IOException
   * @throws TalismaneException
   *           if an observer does not implement this interface, or if no
   *           constructor is found with the correct signature
   */
  public static List<ParseEvaluationObserver> getObservers(File outDir, String sessionId)
      throws IOException, ClassNotFoundException, ReflectiveOperationException, TalismaneException {
    if (outDir != null)
      outDir.mkdirs();

    Config config = ConfigFactory.load();
    Config parserConfig = config.getConfig("talismane.core." + sessionId + ".parser");
    Config evalConfig = parserConfig.getConfig("evaluate");

    List<ParseEvaluationObserver> observers = new ArrayList<>();

    List<ParseConfigurationProcessor> processors = ParseConfigurationProcessor.getProcessors(null, outDir, sessionId);
    for (ParseConfigurationProcessor processor : processors) {
      ParseConfigurationProcessorWrapper wrapper = new ParseConfigurationProcessorWrapper(processor);
      observers.add(wrapper);
    }

    List<String> classes = evalConfig.getStringList("observers");
    if (outDir != null)
      outDir.mkdirs();

    for (String className : classes) {
      @SuppressWarnings("rawtypes")
      Class untypedClass = Class.forName(className);
      if (!ParseEvaluationObserver.class.isAssignableFrom(untypedClass))
        throw new TalismaneException("Class " + className + " does not implement interface " + ParseEvaluationObserver.class.getSimpleName());

      @SuppressWarnings("unchecked")
      Class<? extends ParseEvaluationObserver> clazz = untypedClass;

      Constructor<? extends ParseEvaluationObserver> cons = null;
      ParseEvaluationObserver observer = null;

      if (cons == null) {
        try {
          cons = clazz.getConstructor(File.class, String.class);
        } catch (NoSuchMethodException e) {
          // do nothing
        }
        if (cons != null) {
          observer = cons.newInstance(outDir, sessionId);
        }
      }
      if (cons == null) {
        try {
          cons = clazz.getConstructor(String.class);
        } catch (NoSuchMethodException e) {
          // do nothing
        }
        if (cons != null) {
          observer = cons.newInstance(sessionId);
        } else {
          throw new TalismaneException("No constructor found with correct signature for: " + className);
        }
      }

      observers.add(observer);
    }

    return observers;
  }
}
