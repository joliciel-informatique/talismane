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
package com.joliciel.talismane.tokeniser;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.typesafe.config.Config;

/**
 * Any class that can process token sequences generated by the tokeniser.
 * 
 * @author Assaf Urieli
 *
 */
public interface TokenSequenceProcessor extends Closeable {
  /**
   * Process the next token sequence.
   * 
   * @throws IOException
   */
  public void onNextTokenSequence(TokenSequence tokenSequence) throws IOException;

  /**
   * Collect the processors specified in the configuration key
   * talismane.core.tokeniser.output.processors.<br/>
   * <br/>
   * Each processor must implement this interface and must have a constructor
   * matching one of the following signatures:<br/>
   * - ( {@link File} outputDir, {@link TalismaneSession} session)<br/>
   * - ( {@link TalismaneSession} session)<br/>
   * <br/>
   * Optionally, it can have a constructor with the following signature:<br/>
   * - ( {@link Writer} writer, {@link TalismaneSession} session)<br/>
   * If a writer is provided here, then the first processor with the above
   * constructor will be given the writer.
   * 
   * @param writer
   *          if specified, will be used for the first processor in the list
   *          with a writer in the constructor
   * @param outDir
   *          directory in which to write the various outputs
   * @param session
   *          to read the configuration
   * @return
   * @throws IOException
   * @throws TalismaneException
   *           if a processor does not implement this interface, or if no
   *           constructor is found with the correct signature
   */
  public static List<TokenSequenceProcessor> getProcessors(Writer writer, File outDir, TalismaneSession session)
      throws IOException, ReflectiveOperationException, ClassNotFoundException, TalismaneException {
    Config config = session.getConfig();
    Config myConfig = config.getConfig("talismane.core.tokeniser");

    List<TokenSequenceProcessor> processors = new ArrayList<>();
    List<String> classes = myConfig.getStringList("output.processors");
    if (outDir != null)
      outDir.mkdirs();

    Writer firstProcessorWriter = writer;
    for (String className : classes) {
      @SuppressWarnings("rawtypes")
      Class untypedClass = Class.forName(className);
      if (!TokenSequenceProcessor.class.isAssignableFrom(untypedClass))
        throw new TalismaneException("Class " + className + " does not implement interface " + TokenSequenceProcessor.class.getSimpleName());

      @SuppressWarnings("unchecked")
      Class<? extends TokenSequenceProcessor> clazz = untypedClass;

      Constructor<? extends TokenSequenceProcessor> cons = null;
      TokenSequenceProcessor processor = null;
      if (firstProcessorWriter != null) {
        try {
          cons = clazz.getConstructor(Writer.class, TalismaneSession.class);
        } catch (NoSuchMethodException e) {
          // do nothing
        }
        if (cons != null) {
          processor = cons.newInstance(firstProcessorWriter, session);
          firstProcessorWriter = null;
        }
      }
      if (cons == null) {
        try {
          cons = clazz.getConstructor(File.class, TalismaneSession.class);
        } catch (NoSuchMethodException e) {
          // do nothing
        }
        if (cons != null) {
          processor = cons.newInstance(outDir, session);
        }
      }
      if (cons == null) {
        try {
          cons = clazz.getConstructor(TalismaneSession.class);
        } catch (NoSuchMethodException e) {
          // do nothing
        }
        if (cons != null) {
          processor = cons.newInstance(session);
        } else {
          throw new TalismaneException("No constructor found with correct signature for: " + className);
        }
      }

      processors.add(processor);
    }

    return processors;
  }
}
