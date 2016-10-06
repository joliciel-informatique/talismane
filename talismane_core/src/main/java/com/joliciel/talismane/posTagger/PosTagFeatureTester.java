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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.utils.LogUtils;

public class PosTagFeatureTester implements PosTagSequenceProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(PosTagFeatureTester.class);

	private final Set<PosTaggerFeature<?>> posTaggerFeatures;

	private final Set<String> testWords;

	private final Map<String, Map<String, List<String>>> featureResultMap = new TreeMap<String, Map<String, List<String>>>();
	private final File file;

	/**
	 * A feature tester, which outputs results of applying features to the items
	 * encountered in a given corpus.
	 * 
	 * @param posTaggerFeatures
	 *            the features to test
	 * @param testWords
	 *            limit the test to certain words only
	 * @param file
	 *            the file where the test results should be written
	 */
	public PosTagFeatureTester(Set<PosTaggerFeature<?>> posTaggerFeatures, Set<String> testWords, File file) {
		this.posTaggerFeatures = posTaggerFeatures;
		this.testWords = testWords;
		this.file = file;
	}

	@Override
	public void onNextPosTagSequence(PosTagSequence posTagSequence, Writer writer) {
		PosTagSequence currentHistory = new PosTagSequence(posTagSequence.getTokenSequence());

		for (PosTaggedToken posTaggedToken : posTagSequence) {
			if (testWords.contains(posTaggedToken.getToken().getText().toLowerCase())) {
				StringBuilder sb = new StringBuilder();
				boolean foundToken = false;
				for (PosTaggedToken taggedToken : posTagSequence) {
					if (taggedToken.equals(posTaggedToken)) {
						sb.append(" [" + taggedToken.getToken().getOriginalText().replace(' ', '_') + "/" + taggedToken.getTag().toString() + "]");
						foundToken = true;
					} else if (foundToken) {
						sb.append(" " + taggedToken.getToken().getOriginalText().replace(' ', '_'));
					} else {
						sb.append(" " + taggedToken.getToken().getOriginalText().replace(' ', '_') + "/" + taggedToken.getTag().toString());
					}
				}
				LOG.debug(sb.toString());

				String classification = posTaggedToken.getTag().getCode();
				PosTaggerContext context = new PosTaggerContextImpl(posTaggedToken.getToken(), currentHistory);
				List<FeatureResult<?>> posTagFeatureResults = new ArrayList<FeatureResult<?>>();
				for (PosTaggerFeature<?> posTaggerFeature : posTaggerFeatures) {
					RuntimeEnvironment env = new RuntimeEnvironment();
					FeatureResult<?> featureResult = posTaggerFeature.check(context, env);
					if (featureResult != null)
						posTagFeatureResults.add(featureResult);
				}

				if (LOG.isTraceEnabled()) {
					LOG.trace("Token: " + posTaggedToken.getToken().getText());
					for (FeatureResult<?> result : posTagFeatureResults) {
						LOG.trace(result.toString());
					}
				}

				for (FeatureResult<?> featureResult : posTagFeatureResults) {
					Map<String, List<String>> classificationMap = featureResultMap.get(featureResult.toString());
					if (classificationMap == null) {
						classificationMap = new TreeMap<String, List<String>>();
						featureResultMap.put(featureResult.toString(), classificationMap);
					}
					List<String> sentences = classificationMap.get(classification);
					if (sentences == null) {
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

	public Set<PosTaggerFeature<?>> getPosTaggerFeatures() {
		return posTaggerFeatures;
	}

	@Override
	public void onCompleteAnalysis() {
		try {
			Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), "UTF8"));
			for (String featureResult : this.featureResultMap.keySet()) {
				writer.write("###################\n");
				writer.write(featureResult + "\n");
				int totalCount = 0;
				Map<String, List<String>> classificationMap = featureResultMap.get(featureResult);
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
