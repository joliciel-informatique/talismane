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
package com.joliciel.talismane.posTagger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.CorpusEvent;
import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.posTagger.features.PosTaggerContext;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureService;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * A corpus event stream for postagging.
 * @author Assaf Urieli
 *
 */
class PosTagEventStream implements CorpusEventStream {
    private static final Log LOG = LogFactory.getLog(PosTagEventStream.class);

    PosTagAnnotatedCorpusReader corpusReader;
	Set<PosTaggerFeature<?>> posTaggerFeatures;
	PosTaggerFeatureService posTaggerFeatureService;
	PosTaggerService posTaggerService;
	
	PosTagSequence currentSentence;
	PosTagSequence currentHistory;
	int currentIndex;
	
	PosTagEventStream(PosTagAnnotatedCorpusReader corpusReader,
			Set<PosTaggerFeature<?>> posTaggerFeatures) {
		this.corpusReader = corpusReader;
		this.posTaggerFeatures = posTaggerFeatures;
	}

	@Override
	public boolean hasNext() {
		PerformanceMonitor.startTask("PosTagEventStream.hasNext");
		try {
			while (currentSentence==null) {
				if (this.corpusReader.hasNextPosTagSequence()) {
					currentSentence = this.corpusReader.nextPosTagSequence();
					currentIndex = 0;
					currentHistory = posTaggerService.getPosTagSequence(currentSentence.getTokenSequence(), currentSentence.size());
					if (currentIndex == currentSentence.size()) {
						currentSentence = null;
					}
				} else {
					break;
				}
			}
			return currentSentence!=null;
		} finally {
			PerformanceMonitor.endTask("PosTagEventStream.hasNext");
		}
	}

	@Override
	public CorpusEvent next() {
		PerformanceMonitor.startTask("PosTagEventStream.next");
		try {
			CorpusEvent event = null;
			if (this.hasNext()) {
				PosTaggedToken taggedToken = currentSentence.get(currentIndex++);
				String classification = taggedToken.getTag().getCode();
	
				if (LOG.isDebugEnabled())
					LOG.debug("next event, token: " + taggedToken.getToken().getText() + " : " + classification);
				PosTaggerContext context = posTaggerFeatureService.getContext(taggedToken.getToken(), currentHistory);
				
				List<FeatureResult<?>> posTagFeatureResults = new ArrayList<FeatureResult<?>>();
				PerformanceMonitor.startTask("PosTagEventStream.next - check features");
				try {
					for (PosTaggerFeature<?> posTaggerFeature : posTaggerFeatures) {
						PerformanceMonitor.startTask(posTaggerFeature.getGroupName());
						try {
							FeatureResult<?> featureResult = posTaggerFeature.check(context);
							if (featureResult!=null)
								posTagFeatureResults.add(featureResult);
						} finally {
							PerformanceMonitor.endTask(posTaggerFeature.getGroupName());
						}
					}
				} finally {
					PerformanceMonitor.endTask("PosTagEventStream.next - check features");					
				}
				
				if (LOG.isTraceEnabled()) {
					LOG.trace("Token: " + taggedToken.getToken().getText());
					for (FeatureResult<?> result : posTagFeatureResults) {
						LOG.trace(result.toString());
					}
				}			
				event = new CorpusEvent(posTagFeatureResults, classification);
				
				currentHistory.addPosTaggedToken(taggedToken);
				if (currentIndex==currentSentence.size()) {
					currentSentence = null;
				}
			}
			return event;
		} finally {
			PerformanceMonitor.endTask("PosTagEventStream.next");
		}

	}

	public PosTaggerFeatureService getPosTaggerFeatureService() {
		return posTaggerFeatureService;
	}

	public void setPosTaggerFeatureService(
			PosTaggerFeatureService posTaggerFeatureService) {
		this.posTaggerFeatureService = posTaggerFeatureService;
	}

	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	@Override
	public Map<String, Object> getAttributes() {
		Map<String,Object> attributes = new LinkedHashMap<String, Object>();
		attributes.put("eventStream", this.getClass().getSimpleName());		
		attributes.put("corpusReader", corpusReader.getClass().getSimpleName());		
		
		return attributes;
	}

}
