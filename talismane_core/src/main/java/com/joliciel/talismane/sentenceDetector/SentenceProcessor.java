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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.rawText.Sentence;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Any class that can process sentences found by a sentence detector.
 * 
 * @author Assaf Urieli
 *
 */
public interface SentenceProcessor extends Closeable {
  /**
   * Process the next sentence.
   * 
   * @throws IOException
   */
  public void onNextSentence(Sentence sentence) throws IOException;

  /**
   * Collect the processors specified in the configuration key
   * talismane.core.[sessionId].sentence-detector.output.processors.<br>
   * <br>
   * Each processor must implement this interface and must have a constructor
   * matching one of the following signatures:<br>
   * - ( {@link File} outputDir, {@link String} sessionId)<br>
   * - ( {@link String} sessionId)<br>
   * <br>
   * Optionally, it can have a constructor with the following signature:<br>
   * - ( {@link Writer} writer, {@link String} sessionId)<br>
   * If a writer is provided here, then the first processor with the above
   * constructor will be given the writer.
   * 
   * @param writer
   *          if specified, will be used for the first processor in the list
   *          with a writer in the constructor
   * @param outDir
   *          directory in which to write the various outputs
   * @return
   * @throws IOException
   * @throws TalismaneException
   *           if a processor does not implement this interface, or if no
   *           constructor is found with the correct signature
   */
  public static List<SentenceProcessor> getProcessors(Writer writer, File outDir, String sessionId)
      throws IOException, ReflectiveOperationException, ClassNotFoundException, TalismaneException {
    Config config = ConfigFactory.load();
    Config myConfig = config.getConfig("talismane.core." + sessionId + ".sentence-detector");

    List<SentenceProcessor> processors = new ArrayList<>();
    List<String> classes = myConfig.getStringList("output.processors");
    if (outDir != null)
      outDir.mkdirs();

    Writer firstProcessorWriter = writer;
    for (String className : classes) {
      @SuppressWarnings("rawtypes")
      Class untypedClass = Class.forName(className);
      if (!SentenceProcessor.class.isAssignableFrom(untypedClass))
        throw new TalismaneException("Class " + className + " does not implement interface " + SentenceProcessor.class.getSimpleName());

      @SuppressWarnings("unchecked")
      Class<? extends SentenceProcessor> clazz = untypedClass;

      Constructor<? extends SentenceProcessor> cons = null;
      SentenceProcessor processor = null;
      if (firstProcessorWriter != null) {
        try {
          cons = clazz.getConstructor(Writer.class, String.class);
        } catch (NoSuchMethodException e) {
          // do nothing
        }
        if (cons != null) {
          processor = cons.newInstance(firstProcessorWriter, sessionId);
          firstProcessorWriter = null;
        }
      }
      if (cons == null) {
        try {
          cons = clazz.getConstructor(File.class, String.class);
        } catch (NoSuchMethodException e) {
          // do nothing
        }
        if (cons != null) {
          processor = cons.newInstance(outDir, sessionId);
        }
      }
      if (cons == null) {
        try {
          cons = clazz.getConstructor(String.class);
        } catch (NoSuchMethodException e) {
          // do nothing
        }
        if (cons != null) {
          processor = cons.newInstance(sessionId);
        } else {
          throw new TalismaneException("No constructor found with correct signature for: " + className);
        }
      }

      processors.add(processor);
    }

    return processors;
  }
}
