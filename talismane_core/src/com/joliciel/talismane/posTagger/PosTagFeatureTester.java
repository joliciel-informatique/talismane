package com.joliciel.talismane.posTagger;

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
import com.joliciel.talismane.posTagger.features.PosTaggerContext;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureService;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.PerformanceMonitor;

class PosTagFeatureTester implements PosTagSequenceProcessor {
    private static final Log LOG = LogFactory.getLog(PosTagFeatureTester.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(PosTagFeatureTester.class);

    private Set<PosTaggerFeature<?>> posTaggerFeatures;
    private PosTaggerFeatureService posTaggerFeatureService;
    private PosTaggerService posTaggerService;
    private FeatureService featureService;

	private Set<String> testWords;
	
	private Map<String, Map<String,List<String>>> featureResultMap = new TreeMap<String, Map<String,List<String>>>();
	private File file;
	
	
	public PosTagFeatureTester(Set<PosTaggerFeature<?>> posTaggerFeatures,
			Set<String> testWords, File file) {
		super();
		this.posTaggerFeatures = posTaggerFeatures;
		this.testWords = testWords;
		this.file = file;
	}

	@Override
	public void onNextPosTagSequence(PosTagSequence posTagSequence, Writer writer) {
		PosTagSequence currentHistory = posTaggerService.getPosTagSequence(posTagSequence.getTokenSequence(), posTagSequence.size());

		for (PosTaggedToken posTaggedToken : posTagSequence) {
			if (testWords.contains(posTaggedToken.getToken().getText().toLowerCase())) {
				StringBuilder sb = new StringBuilder();
				boolean foundToken = false;
				for (PosTaggedToken taggedToken : posTagSequence) {
					if (taggedToken.equals(posTaggedToken)) {
						sb.append(" [" + taggedToken.getToken().getOriginalText().replace(' ','_') + "/" + taggedToken.getTag().toString() + "]");
						foundToken = true;
					} else if (foundToken) {
						sb.append(" " + taggedToken.getToken().getOriginalText().replace(' ','_'));
					} else {
						sb.append(" " + taggedToken.getToken().getOriginalText().replace(' ','_') + "/" + taggedToken.getTag().toString());
					}
				}
				LOG.debug(sb.toString());
				
				String classification = posTaggedToken.getTag().getCode();
				PosTaggerContext context = posTaggerFeatureService.getContext(posTaggedToken.getToken(), currentHistory);
				List<FeatureResult<?>> posTagFeatureResults = new ArrayList<FeatureResult<?>>();
				MONITOR.startTask("check features");
				try {
					for (PosTaggerFeature<?> posTaggerFeature : posTaggerFeatures) {
						MONITOR.startTask(posTaggerFeature.getCollectionName());
						try {
							RuntimeEnvironment env = featureService.getRuntimeEnvironment();
							FeatureResult<?> featureResult = posTaggerFeature.check(context, env);
							if (featureResult!=null)
								posTagFeatureResults.add(featureResult);
						} finally {
							MONITOR.endTask();
						}
					}
				} finally {
					MONITOR.endTask();					
				}
				
				if (LOG.isTraceEnabled()) {
					LOG.trace("Token: " + posTaggedToken.getToken().getText());
					for (FeatureResult<?> result : posTagFeatureResults) {
						LOG.trace(result.toString());
					}
				}
				
				for (FeatureResult<?> featureResult : posTagFeatureResults) {
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
			}
			
			currentHistory.addPosTaggedToken(posTaggedToken);			
		}
	}


	public Set<String> getTestWords() {
		return testWords;
	}

	public void setTestWords(Set<String> testWords) {
		this.testWords = testWords;
	}

	public Set<PosTaggerFeature<?>> getPosTaggerFeatures() {
		return posTaggerFeatures;
	}

	public void setPosTaggerFeatures(Set<PosTaggerFeature<?>> posTaggerFeatures) {
		this.posTaggerFeatures = posTaggerFeatures;
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

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}

	@Override
	public void onCompleteAnalysis() {
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
					writer.write("PosTag: " + classification + "\t" + classificationMap.get(classification).size() + "\n");
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

}
