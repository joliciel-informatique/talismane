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
package com.joliciel.talismane.machineLearning.linearsvm;

import gnu.trove.map.TObjectIntMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.AbstractMachineLearningModel;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.MachineLearningAlgorithm;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.utils.JolicielException;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.io.UnclosableWriter;

import de.bwaldvogel.liblinear.Model;

class LinearSVMOneVsRestModel extends AbstractMachineLearningModel implements ClassificationModel {
	private static final Log LOG = LogFactory.getLog(LinearSVMOneVsRestModel.class);

	private MachineLearningService machineLearningService;
	
	private List<Model> models = new ArrayList<Model>();
	private TObjectIntMap<String> featureIndexMap = null;
	private List<String> outcomes = null;
	private transient Set<String> outcomeNames = null;
	
	/**
	 * Default constructor for factory.
	 */
	LinearSVMOneVsRestModel() {}
	
	/**
	 * Construct from a newly trained model including the feature descriptors.
	 */
	LinearSVMOneVsRestModel(
			Map<String,List<String>> descriptors,
			Map<String,Object> trainingParameters) {
		super();
		this.setDescriptors(descriptors);
		this.setTrainingParameters(trainingParameters);
	}
	
	public void addModel(Model model) {
		this.models.add(model);
	}
	
	@Override
	public DecisionMaker getDecisionMaker() {
		LinearSVMOneVsRestDecisionMaker decisionMaker = new LinearSVMOneVsRestDecisionMaker(models, this.featureIndexMap, this.outcomes);
		decisionMaker.setMachineLearningService(this.getMachineLearningService());
		return decisionMaker;
	}

	@Override
	public ClassificationObserver getDetailedAnalysisObserver(File file) {
		throw new JolicielException("No detailed analysis observer currently available for linear SVM.");
	}

	@Override
	public void writeModelToStream(OutputStream outputStream) {
		try {
			ZipOutputStream zos = new ZipOutputStream(outputStream);
			zos.setLevel(ZipOutputStream.STORED);
			int i = 0;
			for (Model model : models) {
				LOG.debug("Writing model " + i + " for outcome " + outcomes.get(i));
				ZipEntry zipEntry = new ZipEntry("model" + i);
				i++;
				zos.putNextEntry(zipEntry);
				Writer writer = new OutputStreamWriter(zos, "UTF-8");
				Writer unclosableWriter = new UnclosableWriter(writer);
				model.save(unclosableWriter);
				zos.closeEntry();
				zos.flush();
			}
		} catch (UnsupportedEncodingException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void loadModelFromStream(InputStream inputStream) {
		// load model or use it directly
		try {
			models = new ArrayList<Model>();
			ZipInputStream zis = new ZipInputStream(inputStream);
			ZipEntry zipEntry = null;
			while ((zipEntry = zis.getNextEntry())!=null) {
				LOG.debug("Reading " + zipEntry.getName());
				Reader reader = new InputStreamReader(zis, "UTF-8");
				Model model = Model.load(reader);
				models.add(model);
			}
		} catch (UnsupportedEncodingException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public MachineLearningAlgorithm getAlgorithm() {
		return MachineLearningAlgorithm.LinearSVMOneVsRest;
	}

	/**
	 * A map of feature names to unique indexes.
	 */
	public TObjectIntMap<String> getFeatureIndexMap() {
		return featureIndexMap;
	}

	public void setFeatureIndexMap(TObjectIntMap<String> featureIndexMap) {
		this.featureIndexMap = featureIndexMap;
	}

	/**
	 * A list of outcomes, where the indexes are the ones used by the binary model.
	 */	
	public List<String> getOutcomes() {
		return outcomes;
	}

	public void setOutcomes(List<String> outcomes) {
		this.outcomes = outcomes;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected boolean loadDataFromStream(InputStream inputStream, ZipEntry zipEntry) {
		try {
			boolean loaded = true;
			if (zipEntry.getName().equals("featureIndexMap.obj")) {
	    		ObjectInputStream in = new ObjectInputStream(inputStream);
				featureIndexMap = (TObjectIntMap<String>) in.readObject();
	    	} else if (zipEntry.getName().equals("outcomes.obj")) {
	    		ObjectInputStream in = new ObjectInputStream(inputStream);
				outcomes = (List<String>) in.readObject();
	    	} else {
	    		loaded = false;
	    	}
			return loaded;
		} catch (ClassNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void writeDataToStream(ZipOutputStream zos) {
		try {
			zos.putNextEntry(new ZipEntry("featureIndexMap.obj"));
			ObjectOutputStream out = new ObjectOutputStream(zos);
	
			try {
				out.writeObject(featureIndexMap);
			} finally {
				out.flush();
			}
			
			zos.flush();
			
			zos.putNextEntry(new ZipEntry("outcomes.obj"));
			out = new ObjectOutputStream(zos);
			try {
				out.writeObject(outcomes);
			} finally {
				out.flush();
			}
			
			zos.flush();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public Set<String> getOutcomeNames() {
		if (this.outcomeNames==null) {
			this.outcomeNames = new TreeSet<String>(this.outcomes);
		}
		return this.outcomeNames;
	}

	public List<Model> getModels() {
		return models;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	@Override
	protected void persistOtherEntries(ZipOutputStream zos) throws IOException {
	}
	
	
}
