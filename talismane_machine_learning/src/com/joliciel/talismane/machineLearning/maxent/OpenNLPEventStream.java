///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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

import com.joliciel.talismane.machineLearning.CorpusEvent;
import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.features.FeatureResult;

import opennlp.model.Event;
import opennlp.model.EventStream;

/**
 * A wrapper for a Talismane CorpusEventStream
 * to make it compatible with OpenNLP Maxent.
 * @author Assaf Urieli
 *
 */
class OpenNLPEventStream implements EventStream {
	private CorpusEventStream corpusEventStream;
	
	public OpenNLPEventStream(CorpusEventStream corpusEventStream) {
		super();
		this.corpusEventStream = corpusEventStream;
	}

	@Override
	public Event next() throws IOException {
		Event event = null;
		if (this.corpusEventStream.hasNext()) {
			CorpusEvent corpusEvent = this.corpusEventStream.next();
			String[] featureNames = new String[corpusEvent.getFeatureResults().size()];
			float[] weights = new float[corpusEvent.getFeatureResults().size()];
			int i = 0;
			for (FeatureResult<?> result : corpusEvent.getFeatureResults()) {
				featureNames[i] = result.getTrainingName();
				float weight = 1;
				if (result.getOutcome() instanceof Double)
				{
					@SuppressWarnings("unchecked")
					FeatureResult<Double> doubleResult = (FeatureResult<Double>) result;
					weight = doubleResult.getOutcome().floatValue();
				}
				weights[i]  = weight;
				i++;
			}
			
			event = new Event(corpusEvent.getClassification(), featureNames, weights);
		}
		return event;
	}

	@Override
	public boolean hasNext() throws IOException {
		return this.corpusEventStream.hasNext();
	}

}
