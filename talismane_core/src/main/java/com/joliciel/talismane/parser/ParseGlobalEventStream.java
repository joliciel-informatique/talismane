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
package com.joliciel.talismane.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.RankingEvent;
import com.joliciel.talismane.machineLearning.RankingEventStream;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * A ranking event stream for parse configurations.
 * @author Assaf Urieli
 *
 */
class ParseGlobalEventStream implements RankingEventStream<PosTagSequence> {
    private static final Logger LOG = LoggerFactory.getLogger(ParseGlobalEventStream.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(ParseGlobalEventStream.class);

    ParserAnnotatedCorpusReader corpusReader;
    Set<ParseConfigurationFeature<?>> parseFeatures;
    
	ParseConfiguration targetConfiguration;
	
	ParserServiceInternal parserServiceInternal;
	MachineLearningService machineLearningService;
	FeatureService featureService;
	
	ParseGlobalEventStream(ParserAnnotatedCorpusReader corpusReader, Set<ParseConfigurationFeature<?>> parseFeatures) {
		this.corpusReader = corpusReader;
		this.parseFeatures = parseFeatures;
	}

	@Override
	public boolean hasNext() {
		MONITOR.startTask("hasNext");
		try {
			while (targetConfiguration==null) {
				if (this.corpusReader.hasNextConfiguration()) {
					targetConfiguration = this.corpusReader.nextConfiguration();
				} else {
					break;
				}
			}
			
			if (targetConfiguration==null) {
				LOG.debug("Event stream reading complete");
			}
			return targetConfiguration!=null;
		} finally {
			MONITOR.endTask();
		}
	}

	@Override
	public RankingEvent<PosTagSequence> next() {
		MONITOR.startTask("next");
		try {
			RankingEvent<PosTagSequence> event = null;
			if (this.hasNext()) {
				LOG.debug("next event, configuration: " + targetConfiguration.toString());
				ParseConfiguration currentConfiguration = parserServiceInternal.getInitialConfiguration(targetConfiguration.getPosTagSequence());

				// don't add features for initial configuration, since these don't get added when analysing
				// targetConfiguration.getIncrementalFeatureResults().add(this.analyseFeatures(currentConfiguration));
				
				for (Transition transition : targetConfiguration.getTransitions()) {
					currentConfiguration = parserServiceInternal.getConfiguration(currentConfiguration);
					transition.apply(currentConfiguration);
					targetConfiguration.getIncrementalFeatureResults().add(this.analyseFeatures(currentConfiguration));
				}
				
				event = this.machineLearningService.getRankingEvent(targetConfiguration.getPosTagSequence(), targetConfiguration);
				targetConfiguration = null;
			}
			return event;
		} finally {
			MONITOR.endTask();
		}
	}
	
	List<FeatureResult<?>> analyseFeatures(ParseConfiguration configuration) {
		List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();
		for (ParseConfigurationFeature<?> parseFeature : parseFeatures) {
			MONITOR.startTask(parseFeature.getName());
			try {
				RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
				FeatureResult<?> featureResult = parseFeature.check(configuration, env);
				if (featureResult!=null) {
					featureResults.add(featureResult);
					if (LOG.isTraceEnabled()) {
						LOG.trace(featureResult.toString());
					}
				}	
			} finally {
				MONITOR.endTask();
			}
		}
		return featureResults;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String,String> attributes = new LinkedHashMap<String, String>();
		attributes.put("eventStream", this.getClass().getSimpleName());		
		attributes.put("corpusReader", corpusReader.getClass().getSimpleName());		
		
		attributes.putAll(corpusReader.getCharacteristics());
		return attributes;
	}

	public ParserServiceInternal getParserServiceInternal() {
		return parserServiceInternal;
	}

	public void setParserServiceInternal(ParserServiceInternal parserServiceInternal) {
		this.parserServiceInternal = parserServiceInternal;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}

	@Override
	public void rewind() {
		this.corpusReader.rewind();
	}

	
}
