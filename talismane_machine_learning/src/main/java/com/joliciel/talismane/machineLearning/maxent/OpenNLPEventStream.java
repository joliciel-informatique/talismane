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
package com.joliciel.talismane.machineLearning.maxent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.ClassificationEvent;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;

import opennlp.model.Event;
import opennlp.model.EventStream;

/**
 * A wrapper for a Talismane CorpusEventStream to make it compatible with
 * OpenNLP Maxent.
 * 
 * @author Assaf Urieli
 *
 */
class OpenNLPEventStream implements EventStream {
	private static final Logger LOG = LoggerFactory.getLogger(OpenNLPEventStream.class);
	private ClassificationEventStream corpusEventStream;

	public OpenNLPEventStream(ClassificationEventStream corpusEventStream) {
		super();
		this.corpusEventStream = corpusEventStream;
	}

	@Override
	public Event next() throws IOException {
		try {
			Event event = null;
			if (this.corpusEventStream.hasNext()) {
				ClassificationEvent corpusEvent = this.corpusEventStream.next();

				List<String> contextList = new ArrayList<String>();
				List<Float> weightList = new ArrayList<Float>();
				OpenNLPDecisionMaker.prepareData(corpusEvent.getFeatureResults(), contextList, weightList);

				String[] contexts = new String[contextList.size()];
				float[] weights = new float[weightList.size()];

				int i = 0;
				for (String context : contextList) {
					contexts[i++] = context;
				}
				i = 0;
				for (Float weight : weightList) {
					weights[i++] = weight;
				}

				event = new Event(corpusEvent.getClassification(), contexts, weights);
			}
			return event;
		} catch (TalismaneException e) {
			LOG.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean hasNext() throws IOException {
		try {
			return this.corpusEventStream.hasNext();
		} catch (TalismaneException e) {
			LOG.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

}
