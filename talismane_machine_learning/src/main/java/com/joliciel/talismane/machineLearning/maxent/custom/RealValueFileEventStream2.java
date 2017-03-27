/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.joliciel.talismane.machineLearning.maxent.custom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.utils.LogUtils;

import opennlp.maxent.GIS;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.model.AbstractEventStream;
import opennlp.model.AbstractModel;
import opennlp.model.Event;
import opennlp.model.EventStream;
import opennlp.model.FileEventStream;

/**
 * Class for using a file of events as an event stream. The format of the file
 * is one event perline with each line consisting of outcome followed by
 * contexts (space delimited).
 * 
 * @author Tom Morton
 * @author Assaf Urieli for Joliciel updates
 *
 */
public class RealValueFileEventStream2 extends AbstractEventStream {
  private static final Logger LOG = LoggerFactory.getLogger(RealValueFileEventStream2.class);

  BufferedReader reader;
  String line;

  /**
   * Creates a new file event stream from the specified file name.
   * 
   * @param fileName
   *            the name fo the file containing the events.
   * @throws IOException
   *             When the specified file can not be read.
   */
  public RealValueFileEventStream2(String fileName, String encoding) throws IOException {
    if (encoding == null) {
      reader = new BufferedReader(new FileReader(fileName));
    } else {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), encoding));
    }
  }

  public RealValueFileEventStream2(String fileName) throws IOException {
    this(fileName, null);
  }

  /**
   * Creates a new file event stream from the specified file.
   * 
   * @param file
   *            the file containing the events.
   * @throws IOException
   *             When the specified file can not be read.
   */
  public RealValueFileEventStream2(File file) throws IOException {
    reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
  }

  @Override
  public boolean hasNext() {
    try {
      return (null != (line = reader.readLine()));
    } catch (IOException e) {
      LogUtils.logError(LOG, e);
      return (false);
    }
  }

  @Override
  public Event next() {
    StringTokenizer st = new StringTokenizer(line);
    String outcome = st.nextToken();
    if (outcome.equals("&null;"))
      outcome = "";
    else if (outcome.equals("&space;"))
      outcome = " ";

    int count = st.countTokens();
    // Assaf update: read real values from file
    boolean hasValues = line.contains("=");
    String[] context = new String[count];
    float[] values = null;
    if (hasValues)
      values = new float[count];
    for (int ci = 0; ci < count; ci++) {
      String token = st.nextToken();
      if (hasValues) {
        int equalsPos = token.lastIndexOf('=');
        if (equalsPos < 0) {
          LOG.error("Missing value");
          LOG.error("Line: " + line);
          LOG.error("Token: " + token);
          throw new RuntimeException("Missing value, on token \"" + token + "\"");
        }
        context[ci] = token.substring(0, equalsPos);
        values[ci] = Float.parseFloat(token.substring(equalsPos + 1));
      } else {
        context[ci] = token;
      }
    }
    Event event = null;
    if (hasValues)
      event = new Event(outcome, context, values);
    else
      event = new Event(outcome, context);
    return event;
  }

  /**
   * Generates a string representing the specified event.
   * 
   * @param event
   *            The event for which a string representation is needed.
   * @return A string representing the specified event.
   */
  public static String toLine(Event event) {
    StringBuffer sb = new StringBuffer();
    String outcome = event.getOutcome();
    if (outcome.length() == 0)
      outcome = "&null;";
    else if (outcome.equals(" "))
      outcome = "&space;";
    sb.append(outcome);
    String[] context = event.getContext();
    // Assaf: write real values to file
    float[] values = event.getValues();
    for (int ci = 0, cl = context.length; ci < cl; ci++) {
      sb.append(" " + context[ci]);
      if (values != null)
        sb.append("=" + values[ci]);
    }
    sb.append(System.getProperty("line.separator"));
    return sb.toString();
  }

  /**
   * Trains and writes a model based on the events in the specified event
   * file. the name of the model created is based on the event file name.
   * 
   * @param args
   *            eventfile [iterations cuttoff]
   * @throws IOException
   *             when the eventfile can not be read or the model file can not
   *             be written.
   */
  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      LOG.error("Usage: FileEventStream eventfile [iterations cutoff]");
      System.exit(1);
    }
    int ai = 0;
    String eventFile = args[ai++];
    EventStream es = new FileEventStream(eventFile);
    int iterations = 100;
    int cutoff = 5;
    if (ai < args.length) {
      iterations = Integer.parseInt(args[ai++]);
      cutoff = Integer.parseInt(args[ai++]);
    }
    AbstractModel model = GIS.trainModel(es, iterations, cutoff);
    new SuffixSensitiveGISModelWriter(model, new File(eventFile + ".bin.gz")).persist();
  }
}
