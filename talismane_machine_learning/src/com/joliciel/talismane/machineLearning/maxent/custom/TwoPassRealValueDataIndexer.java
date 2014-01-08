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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opennlp.model.AbstractDataIndexer;
import opennlp.model.ComparableEvent;
import opennlp.model.Event;
import opennlp.model.EventStream;
import opennlp.model.FileEventStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Extends TwoPassDataIndexer to take into account real values.
 * @author Assaf Urieli
 *
 */
public class TwoPassRealValueDataIndexer extends AbstractDataIndexer {
    private static final Log LOG = LogFactory.getLog(TwoPassRealValueDataIndexer.class);
    float[][] values;


    /**
     * One argument constructor for DataIndexer which calls the two argument
     * constructor assuming no cutoff.
     *
     * @param eventStream An Event[] which contains the a list of all the Events
     *               seen in the training data.
     */
    public TwoPassRealValueDataIndexer(EventStream eventStream) throws IOException {
      this(eventStream, 0);
    }

    public TwoPassRealValueDataIndexer(EventStream eventStream, int cutoff) throws IOException {
      this(eventStream,cutoff,true);
    }
    /**
     * Two argument constructor for DataIndexer.
     *
     * @param eventStream An Event[] which contains the a list of all the Events
     *               seen in the training data.
     * @param cutoff The minimum number of times a predicate must have been
     *               observed in order to be included in the model.
     */
    public TwoPassRealValueDataIndexer(EventStream eventStream, int cutoff, boolean sort) throws IOException {
      Map<String,Integer> predicateIndex = new HashMap<String,Integer>();
      List<ComparableEvent> eventsToCompare;

      System.out.println("Indexing events using cutoff of " + cutoff + "\n");

      System.out.print("\tComputing event counts...  ");
      try {
        File tmp = File.createTempFile("events", null);
        tmp.deleteOnExit();
        Writer osw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmp),"UTF8"));
        int numEvents = computeEventCounts(eventStream, osw, predicateIndex, cutoff);
        System.out.println("done. " + numEvents + " events");

        System.out.print("\tIndexing...  ");

        FileEventStream fes = new FileEventStream(tmp);
        try {
          eventsToCompare = index(numEvents, fes, predicateIndex);
        } finally {
          fes.close();
        }
        // done with predicates
        predicateIndex = null;
        tmp.delete();
        System.out.println("done.");

        if (sort) { 
          System.out.print("Sorting and merging events... ");
        }
        else {
          System.out.print("Collecting events... ");
        }
        sortAndMerge(eventsToCompare,sort);
        System.out.println("Done indexing.");
      }
      catch(IOException e) {
        System.err.println(e);
      }
    }

    /**
        * Reads events from <tt>eventStream</tt> into a linked list.  The
        * predicates associated with each event are counted and any which
        * occur at least <tt>cutoff</tt> times are added to the
        * <tt>predicatesInOut</tt> map along with a unique integer index.
        *
        * @param eventStream an <code>EventStream</code> value
        * @param eventStore a writer to which the events are written to for later processing.
        * @param predicatesInOut a <code>TObjectIntHashMap</code> value
        * @param cutoff an <code>int</code> value
        */
    private int computeEventCounts(EventStream eventStream, Writer eventStore, Map<String,Integer> predicatesInOut, int cutoff) throws IOException {
      Map<String,Integer> counter = new HashMap<String,Integer>();
      int eventCount = 0;
      Set<String> predicateSet = new HashSet<String>();
      while (eventStream.hasNext()) {
        Event ev = eventStream.next();
        eventCount++;
        eventStore.write(FileEventStream.toLine(ev));
        String[] ec = ev.getContext();
        update(ec,predicateSet,counter,cutoff);
      }
      predCounts = new int[predicateSet.size()];
      int index = 0;
      for (Iterator<String> pi=predicateSet.iterator();pi.hasNext();index++) {
        String predicate = pi.next();
        predCounts[index] = counter.get(predicate);
        predicatesInOut.put(predicate,index);
      }
      eventStore.close();
      return eventCount;
    }

    @Override
    public float[][] getValues() {
            return values;
    }
    
    @Override
    protected int sortAndMerge(List eventsToCompare,boolean sort) {
            int numUniqueEvents = super.sortAndMerge(eventsToCompare,sort);
            values = new float[numUniqueEvents][];
            int numEvents = eventsToCompare.size();
            for (int i = 0, j = 0; i < numEvents; i++) {
                    ComparableEvent evt = (ComparableEvent) eventsToCompare.get(i);
                    if (null == evt) {
                            continue; // this was a dupe, skip over it.
                    }
                    values[j++] = evt.values;
            }
            return numUniqueEvents;
    }

    protected List index(int numEvents, EventStream es, Map<String,Integer> predicateIndex) throws IOException {
            Map<String,Integer> omap = new HashMap<String,Integer>();
            int outcomeCount = 0;
            List eventsToCompare = new ArrayList(numEvents);
            List<Integer> indexedContext = new ArrayList<Integer>();
            while (es.hasNext()) {
                    Event ev = es.next();
                    String[] econtext = ev.getContext();
                    ComparableEvent ce;

                    int ocID;
                    String oc = ev.getOutcome();

                    if (omap.containsKey(oc)) {
                            ocID = omap.get(oc);
                    }
                    else {
                            ocID = outcomeCount++;
                            omap.put(oc, ocID);
                    }

                    for (int i = 0; i < econtext.length; i++) {
                            String pred = econtext[i];
                            if (predicateIndex.containsKey(pred)) {
                                    indexedContext.add(predicateIndex.get(pred));
                            }
                    }

                    // drop events with no active features
                    if (indexedContext.size() > 0) {
                            int[] cons = new int[indexedContext.size()];
                            for (int ci=0;ci<cons.length;ci++) {
                                    cons[ci] = indexedContext.get(ci);
                            }
                            ce = new ComparableEvent(ocID, cons, ev.getValues());
                            eventsToCompare.add(ce);
                    }
                    else {
                            LOG.debug("Dropped event " + ev.getOutcome() + ":" + Arrays.asList(ev.getContext()));
                    }
                    // recycle the TIntArrayList
                    indexedContext.clear();
            }
            outcomeLabels = toIndexedStringArray(omap);
            predLabels = toIndexedStringArray(predicateIndex);
            return eventsToCompare;
     }
    
     protected EventStream getFileEventStream(File file) throws IOException {
              return new RealValueFileEventStream2(file);
     }
     
     protected String toLine(Event ev) {
              return RealValueFileEventStream2.toLine(ev);
     }
}