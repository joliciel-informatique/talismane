///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
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
package com.joliciel.talismane.machineLearning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import com.joliciel.talismane.TalismaneException;

/**
 * An event stream that guarantees that no outcome will appear more than n times
 * more than the minimum outcome.
 * 
 * @author Assaf Urieli
 *
 */
public class OutcomeEqualiserEventStream implements ClassificationEventStream {

  ClassificationEventStream originalEventStream = null;
  List<ClassificationEvent> eventList = null;
  int eventIndex = 0;
  double multiple = 1;

  public OutcomeEqualiserEventStream(ClassificationEventStream originalEventStream, double multiple) {
    super();
    this.originalEventStream = originalEventStream;
    this.multiple = multiple;
  }

  @Override
  public ClassificationEvent next() {
    ClassificationEvent event = eventList.get(eventIndex);
    eventIndex++;
    return event;
  }

  @Override
  public boolean hasNext() throws TalismaneException, IOException {
    this.initialiseStream();
    return (eventIndex < eventList.size());
  }

  void initialiseStream() throws TalismaneException, IOException {
    if (eventList == null) {
      Map<String, List<ClassificationEvent>> eventOutcomeMap = new HashMap<String, List<ClassificationEvent>>();
      while (originalEventStream.hasNext()) {
        ClassificationEvent event = originalEventStream.next();
        List<ClassificationEvent> eventsPerOutcome = eventOutcomeMap.get(event.getClassification());
        if (eventsPerOutcome == null) {
          eventsPerOutcome = new ArrayList<ClassificationEvent>();
          eventOutcomeMap.put(event.getClassification(), eventsPerOutcome);
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

      eventList = new ArrayList<ClassificationEvent>();
      eventList.addAll(eventOutcomeMap.get(minOutcome));

      int maxSize = (int) (minSize * multiple);
      for (String outcome : eventOutcomeMap.keySet()) {
        if (outcome.equals(minOutcome))
          continue;
        List<ClassificationEvent> eventsPerOutcome = eventOutcomeMap.get(outcome);
        if (eventsPerOutcome.size() <= maxSize)
          eventList.addAll(eventsPerOutcome);
        else {
          Set<Integer> usedUp = new TreeSet<Integer>();
          for (int i = 0; i < maxSize; i++) {
            if (i >= eventsPerOutcome.size())
              break;
            int index = random.nextInt(eventsPerOutcome.size());
            while (usedUp.contains(index))
              index = random.nextInt(eventsPerOutcome.size());
            usedUp.add(index);

            ClassificationEvent event = eventsPerOutcome.get(index);
            eventList.add(event);
          } // next randomly selected event
        }
      }
    }
  }

  @Override
  public Map<String, String> getAttributes() {
    return originalEventStream.getAttributes();
  }
}
