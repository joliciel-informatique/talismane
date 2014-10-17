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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.PerformanceMonitor;

class ParseFeatureTester implements ParseConfigurationProcessor {
    private static final Log LOG = LogFactory.getLog(ParseFeatureTester.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(ParseConfigurationProcessor.class);

    private Set<ParseConfigurationFeature<?>> parseFeatures;
    private ParserServiceInternal parserServiceInternal;
    private FeatureService featureService;
	
	private Map<String, Map<String,List<String>>> featureResultMap = new TreeMap<String, Map<String,List<String>>>();
	private File file;
	
	public ParseFeatureTester(Set<ParseConfigurationFeature<?>> parseFeatures,
			File file) {
		super();
		this.parseFeatures = parseFeatures;
		this.file = file;
	}

	@Override
	public void onNextParseConfiguration(ParseConfiguration parseConfiguration, Writer writer) {

		ParseConfiguration currentConfiguration = parserServiceInternal.getInitialConfiguration(parseConfiguration.getPosTagSequence());
		
		for (Transition transition : parseConfiguration.getTransitions()) {
			StringBuilder sb = new StringBuilder();
			for (PosTaggedToken taggedToken : currentConfiguration.getPosTagSequence()) {
				if (taggedToken.equals(currentConfiguration.getStack().getFirst())) {
					sb.append(" #[" + taggedToken.getToken().getOriginalText().replace(' ','_') + "/" + taggedToken.getTag().toString() + "]#");
				} else if (taggedToken.equals(currentConfiguration.getBuffer().getFirst())) {
					sb.append(" #[" + taggedToken.getToken().getOriginalText().replace(' ','_') + "/" + taggedToken.getTag().toString() + "]#");
				} else {
					sb.append(" " + taggedToken.getToken().getOriginalText().replace(' ','_') + "/" + taggedToken.getTag().toString());
				}
			}
			
			sb.append(" ## Line: " + parseConfiguration.getSentence().getStartLineNumber());
			
			if (LOG.isTraceEnabled())
				LOG.trace(sb.toString());
			
			List<FeatureResult<?>> parseFeatureResults = new ArrayList<FeatureResult<?>>();
			for (ParseConfigurationFeature<?> parseFeature : parseFeatures) {
				MONITOR.startTask(parseFeature.getName());
				try {
					RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
					FeatureResult<?> featureResult = parseFeature.check(currentConfiguration, env);
					if (featureResult!=null) {
						parseFeatureResults.add(featureResult);
						if (LOG.isTraceEnabled()) {
							LOG.trace(featureResult.toString());
						}
					}	
				} finally {
					MONITOR.endTask();
				}
			}
			
			String classification = transition.getCode();
			
			for (FeatureResult<?> featureResult : parseFeatureResults) {
				Map<String,List<String>> classificationMap = featureResultMap.get(featureResult.toString());
				if (classificationMap==null) {
					classificationMap = new TreeMap<String, List<String>>();
					featureResultMap.put(featureResult.toString(), classificationMap);
				}
				List<String> sentences = classificationMap.get(classification);
				if (sentences==null) {
					sentences = new ArrayList<String>();
					classificationMap.put(classification, sentences);
				}
				sentences.add(sb.toString());
			}
			
			// apply the transition and up the index
			currentConfiguration = parserServiceInternal.getConfiguration(currentConfiguration);
			transition.apply(currentConfiguration);
		}
	}

	@Override
	public void onCompleteParse() {
		try {
			Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false),"UTF8"));
			for (String featureResult : this.featureResultMap.keySet()) {
				writer.write("###################\n");
				writer.write(featureResult + "\n");
				int totalCount = 0;
				Map<String,List<String>> classificationMap = featureResultMap.get(featureResult);
				for (String classification : classificationMap.keySet()) {
					totalCount += classificationMap.get(classification).size();
				}
				writer.write("Total count: " + totalCount + "\n");
				for (String classification : classificationMap.keySet()) {
					writer.write(classification + " count:" + classificationMap.get(classification).size() + "\n");
				}
				for (String classification : classificationMap.keySet()) {
					writer.write("Transition: " + classification + "\t" + classificationMap.get(classification).size() + "\n");
					for (String sentence : classificationMap.get(classification)) {
						writer.write(sentence + "\n");
					}
				}
				writer.flush();
			}
			writer.close();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	public ParserServiceInternal getParserServiceInternal() {
		return parserServiceInternal;
	}

	public void setParserServiceInternal(ParserServiceInternal parserServiceInternal) {
		this.parserServiceInternal = parserServiceInternal;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}
}
