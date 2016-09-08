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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.model.ComparableEvent;
import opennlp.model.Event;
import opennlp.model.EventStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends TwoPassDataIndexer to take into account real values.
 * @author Assaf Urieli
 *
 */
public class TwoPassRealValueDataIndexer extends TwoPassDataIndexer {
	private static final Logger LOG = LoggerFactory.getLogger(TwoPassRealValueDataIndexer.class);
	float[][] values;

	public TwoPassRealValueDataIndexer(EventStream eventStream, int cutoff)
	throws IOException {
		super(eventStream, cutoff);
	}

	public TwoPassRealValueDataIndexer(EventStream eventStream)
	throws IOException {
		super(eventStream);
	}

	public TwoPassRealValueDataIndexer(EventStream eventStream, int cutoff, boolean sort) throws IOException {
		super(eventStream, cutoff, sort);
	}

	public float[][] getValues() {
		return values;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
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
	
	 @Override
    protected EventStream getFileEventStream(File file) throws IOException {
		  return new RealValueFileEventStream2(file);
	 }
	 
	 protected String toLine(Event ev) {
		  return RealValueFileEventStream2.toLine(ev);
	 }
}
