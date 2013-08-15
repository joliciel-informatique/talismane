package com.joliciel.talismane.machineLearning.perceptron;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.AbstractMachineLearningModel;
import com.joliciel.talismane.machineLearning.FeatureWeightVector;
import com.joliciel.talismane.machineLearning.MachineLearningAlgorithm;
import com.joliciel.talismane.machineLearning.RankingModel;

public class PerceptronRankingModel extends AbstractMachineLearningModel implements RankingModel {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(PerceptronClassificationModel.class);
	PerceptronRankingModelParameters params = null;
	
	PerceptronRankingModel() { }
	
	public PerceptronRankingModel(PerceptronRankingModelParameters params,
			Map<String, List<String>> descriptors) {
		this.params = params;
		this.setDescriptors(descriptors);
		this.addDependency("FeatureWeightVector", params);
	}
	

	@Override
	public void onLoadComplete() {
		super.onLoadComplete();
		this.params = (PerceptronRankingModelParameters) this.getDependencies().get("FeatureWeightVector");
	}
	
	@Override
	public MachineLearningAlgorithm getAlgorithm() {
		return MachineLearningAlgorithm.PerceptronRanking;
	}

	@Override
	public void loadModelFromStream(InputStream inputStream) {
		// nothing to do
	}

	@Override
	public void writeModelToStream(OutputStream outputStream) {
		// nothing to do
	}

	@Override
	public boolean loadDataFromStream(InputStream inputStream, ZipEntry zipEntry) {
		return false;
	}

	@Override
	public void writeDataToStream(ZipOutputStream zos) {
		// nothing to do
	}

	@Override
	public FeatureWeightVector getFeatureWeightVector() {
		return this.params;
	}

	@Override
	protected void persistOtherEntries(ZipOutputStream zos) throws IOException {
		// nothing to do
	}
}
