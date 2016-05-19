package com.joliciel.talismane.machineLearning.linearsvm;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TObjectIntProcedure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.MachineLearningServiceLocator;
import com.joliciel.talismane.utils.BoundedTreeSet;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.WeightedOutcome;
import com.joliciel.talismane.utils.WeightedOutcomeAscendingComparator;

import de.bwaldvogel.liblinear.Model;

public class LinearSVMModelAnalyser {
	private static final Logger LOG = LoggerFactory.getLogger(LinearSVMModelAnalyser.class);
	private static final CSVFormatter CSV = new CSVFormatter(5);
	private int featureCount = 200;
	
	/**
	 */
	public void analyse(File modelFile, File outDir) throws Exception {
		outDir.mkdirs();
		
		String baseName = modelFile.getName();
		if (baseName.indexOf('.') > 0)
			baseName = baseName.substring(0, baseName.lastIndexOf('.'));

		File outFile = new File(outDir, baseName + ".features.csv");
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile, false),"UTF8"));
		try {
			MachineLearningServiceLocator locator = MachineLearningServiceLocator.getInstance();
			MachineLearningService machineLearningService = locator.getMachineLearningService();
			ZipInputStream zis = new ZipInputStream(new FileInputStream(modelFile));
			
			ClassificationModel classificationModel = machineLearningService.getClassificationModel(zis);
			if (classificationModel instanceof LinearSVMOneVsRestModel) {
				LinearSVMOneVsRestModel linearSVMModel = (LinearSVMOneVsRestModel) classificationModel;
				int outcomeIndex = 0;
				
				final TIntObjectMap<String> featureNameMap = new TIntObjectHashMap<String>();
				linearSVMModel.getFeatureIndexMap().forEachEntry(new TObjectIntProcedure<String>() {
					@Override
					public boolean execute(String name, int index) {
						featureNameMap.put(index,name);
						return true;
					}
				});
				
				for (Model model : linearSVMModel.getModels()) {
					String outcome = linearSVMModel.getOutcomes().get(outcomeIndex++);
					LOG.info("Analysing model for " + outcome);
					int myLabel = 0;
					for (int j=0; j<model.getLabels().length; j++)
						if (model.getLabels()[j]==1) myLabel=j;
					
					boolean inverseWeights = false;
					if (myLabel!=0)
						inverseWeights = true;
					
					int i = 0;
					BoundedTreeSet<WeightedOutcome<Integer>> bestFeatures = new BoundedTreeSet<WeightedOutcome<Integer>>(featureCount);
					BoundedTreeSet<WeightedOutcome<Integer>> worstFeatures = new BoundedTreeSet<WeightedOutcome<Integer>>(featureCount, new WeightedOutcomeAscendingComparator<Integer>());
					for (int featureIndex=0; featureIndex<model.getNrFeature(); featureIndex++) {
						double featureWeight = model.getFeatureWeights()[i++];
						if (inverseWeights)
							featureWeight = 0-featureWeight;
						
						WeightedOutcome<Integer> featureAndWeight = new WeightedOutcome<Integer>(featureIndex+1, featureWeight);
						bestFeatures.add(featureAndWeight);
						worstFeatures.add(featureAndWeight);
					}
					
					
					List<WeightedOutcome<Integer>> worstFeatureList = new ArrayList<WeightedOutcome<Integer>>(worstFeatures);
					
					writer.append(CSV.format("####Outcome") + CSV.format(outcome) + "\n");
					i=0;
					for (WeightedOutcome<Integer> goodFeature : bestFeatures) {
						String goodFeatureName = featureNameMap.get(goodFeature.getOutcome());
						WeightedOutcome<Integer> badFeature = worstFeatureList.get(i++);
						String badFeatureName = featureNameMap.get(badFeature.getOutcome());
						writer.append(CSV.format(goodFeatureName) + CSV.format(goodFeature.getWeight()) + CSV.format(badFeatureName) + CSV.format(badFeature.getWeight()) + "\n");
					}
					writer.append("\n");
					writer.flush();
				}
			} else {
				throw new RuntimeException("Unsupported model type: " + classificationModel.getClass().getSimpleName());
			}
		} finally {
			writer.close();
		}
	}

	public int getFeatureCount() {
		return featureCount;
	}

	public void setFeatureCount(int featureCount) {
		this.featureCount = featureCount;
	}

	
}
