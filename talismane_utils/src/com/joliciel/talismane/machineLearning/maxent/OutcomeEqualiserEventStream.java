///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.machineLearning.maxent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.Set;

import opennlp.model.Event;
import opennlp.model.EventStream;

/**
 * An event stream that guarantees that no outcome will appear more than 
 * n times more than the minimum outcome.
 * @author Assaf Urieli
 *
 */
public class OutcomeEqualiserEventStream implements EventStream {
	
	EventStream originalEventStream = null;
	List<Event> eventList = null;
	int eventIndex = 0;
	double multiple = 1;
	
	public OutcomeEqualiserEventStream(EventStream originalEventStream, double multiple) {
		super();
		this.originalEventStream = originalEventStream;
		this.multiple = multiple;
	}

	@Override
	public Event next() throws IOException {
		Event event = eventList.get(eventIndex);
		eventIndex++;
		return event;
	}

	@Override
	public boolean hasNext() throws IOException {
		this.initialiseStream();
		return (eventIndex<eventList.size());
	}

	void initialiseStream() throws IOException {
		if (eventList==null) {
			Map<String,List<Event>> eventOutcomeMap = new HashMap<String, List<Event>>();
			while (originalEventStream.hasNext())
			{
				Event event = originalEventStream.next();
				List<Event> eventsPerOutcome = eventOutcomeMap.get(event.getOutcome());
				if (eventsPerOutcome==null) {
					eventsPerOutcome = new ArrayList<Event>();
					eventOutcomeMap.put(event.getOutcome(), eventsPerOutcome);
				}
				eventsPerOutcome.add(event);
			}
			
			int minSize = Integer.MAX_VALUE;
			String minOutcome = "";
			for (String outcome : eventOutcomeMap.keySet()) {
				int size = eventOutcomeMap.get(outcome).size();
				if (size < minSize) {
					minSize = size;
					minOutcome = outcome;
				}
			}
			
			Random random = new Random(new Date().getTime());

			eventList = new ArrayList<Event>();
			eventList.addAll(eventOutcomeMap.get(minOutcome));
			
			int maxSize = (int) ((double)minSize * multiple);
			for (String outcome : eventOutcomeMap.keySet()) {
				if (outcome.equals(minOutcome))
					continue;
				List<Event> eventsPerOutcome = eventOutcomeMap.get(outcome);
				if (eventsPerOutcome.size()<=maxSize)
					eventList.addAll(eventsPerOutcome);
				else {
					Set<Integer> usedUp = new TreeSet<Integer>();
					for (int i=0;i<maxSize;i++) {
						if (i>=eventsPerOutcome.size())
							break;
						int index = random.nextInt(eventsPerOutcome.size());
						while (usedUp.contains(index))
							index = random.nextInt(eventsPerOutcome.size());
						usedUp.add(index);
						
						Event event = eventsPerOutcome.get(index);
						eventList.add(event);
					} // next randomly selected event
				}
			}
		}
	}
}
