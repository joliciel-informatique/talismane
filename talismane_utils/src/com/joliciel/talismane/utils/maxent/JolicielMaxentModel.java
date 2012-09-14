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
package com.joliciel.talismane.utils.maxent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.utils.DecisionMaker;
import com.joliciel.talismane.utils.util.LogUtils;

import opennlp.model.MaxentModel;

/**
 * A wrapper for a maxent model and the features used to train it -
 * useful since the same features need to be used when evaluating on the basis on this model.
 * Also contains the attributes describing how the model was trained, for reference purposes.
 * @author Assaf Urieli
 *
 */
public class JolicielMaxentModel {
	private static final Log LOG = LogFactory.getLog(JolicielMaxentModel.class);
	private MaxentModel model;
	private List<String> featureDescriptors = new ArrayList<String>();
	private List<String> patternDescriptors = new ArrayList<String>();
	private Map<String, Object> modelAttributes = new TreeMap<String, Object>();
	
	/**
	 * Trains the model.
	 * @param modelTrainer
	 * @param featureDescriptors
	 */
	public JolicielMaxentModel(MaxentModelTrainer modelTrainer,
			List<String> featureDescriptors) {
		super();
		this.model = modelTrainer.trainModel();
		this.featureDescriptors = featureDescriptors;
		this.modelAttributes.put("cutoff", modelTrainer.getCutoff());
		this.modelAttributes.put("iterations", modelTrainer.getIterations());
		this.modelAttributes.put("sigma", modelTrainer.getSigma());
		this.modelAttributes.put("smoothing", modelTrainer.getSmoothing());
		this.modelAttributes.putAll(modelTrainer.getCorpusEventStream().getAttributes());
	}
	
	/**
	 * Construct from a newly trained model including the feature descriptors.
	 * @param model
	 * @param featureDescriptors
	 */
	JolicielMaxentModel(MaxentModel model,
			List<String> featureDescriptors) {
		super();
		this.model = model;
		this.featureDescriptors = featureDescriptors;
	}

	/**
	 * Construct from a formerly persisted model file.
	 * @param modelFile
	 * @throws IOException
	 */
	public JolicielMaxentModel(File modelFile) throws IOException {
		this(new ZipInputStream(new FileInputStream(modelFile)));
	}
	
	/**
	 * Construct from a formerly persisted zip input stream.
	 * @throws IOException
	 */
	public JolicielMaxentModel(ZipInputStream zis) {
		try {
			ZipEntry ze;
		    while ((ze = zis.getNextEntry()) != null) {
		    	if (ze.getName().endsWith(".bin")) {
		    	    this.model = new MaxentModelReader(zis).getModel();	    		
		    	} else if (ze.getName().endsWith("_features.txt")) {
		    		Scanner scanner = new Scanner(zis);
		    		this.featureDescriptors = new ArrayList<String>();
		    		while (scanner.hasNextLine()) {
		    			String featureDescriptor = scanner.nextLine();
		    			this.featureDescriptors.add(featureDescriptor);
		    		}
	
		    	} else if (ze.getName().endsWith("_patterns.txt")) {
		    		Scanner scanner = new Scanner(zis);
		    		this.patternDescriptors = new ArrayList<String>();
		    		while (scanner.hasNextLine()) {
		    			String patternDescriptor = scanner.nextLine();
		    			this.patternDescriptors.add(patternDescriptor);
		    		}
	
		    	} else if (ze.getName().endsWith("_attributes.txt")) {
		    		Scanner scanner = new Scanner(zis);
		    		while (scanner.hasNextLine()) {
		    			String line = scanner.nextLine();
		    			if (line.length()>0) {	
			    			String[] parts = line.split("\t");
			    			String name = parts[0];
			    			if (parts.length>2) {
			    				List<String> collection = new ArrayList<String>();
			    				for (int i = 1; i<parts.length; i++) {
			    					collection.add(parts[i]);
			    				}
			    				this.modelAttributes.put(name, collection);
			    			} else {
			    				String value = "";
			    				if (parts.length>1)
			    					value = parts[1];
			    				this.modelAttributes.put(name, value);
			    			}
		    			}
		    		}
		    	}
		    }
			zis.close();
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public void persist(File modelFile) {
		try {
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(modelFile,false));
	
			String entryNameBase = modelFile.getName().substring(0, modelFile.getName().lastIndexOf('.'));
			zos.putNextEntry(new ZipEntry(entryNameBase + ".bin"));
			new MaxentModelWriter(model, zos).persist();
			zos.flush();
			
			Writer writer = new BufferedWriter(new OutputStreamWriter(zos));
			if (featureDescriptors!=null && featureDescriptors.size()>0) {
				zos.putNextEntry(new ZipEntry(entryNameBase + "_features.txt"));
				for (String featureDescriptor : featureDescriptors) {
					writer.write(featureDescriptor + "\n");
					writer.flush();
				}
				zos.flush();
			}
			
			if (patternDescriptors!=null && patternDescriptors.size()>0) {
				zos.putNextEntry(new ZipEntry(entryNameBase + "_patterns.txt"));
				for (String patternDescriptor : patternDescriptors) {
					writer.write(patternDescriptor + "\n");
					writer.flush();
				}
				
				zos.flush();
			}
			
			zos.putNextEntry(new ZipEntry(entryNameBase + "_attributes.txt"));
			for (String name : this.modelAttributes.keySet()) {
				Object value = this.modelAttributes.get(name);
				if (value instanceof Collection) {
					writer.write(name);
					for (Object o : (Collection) value) {
						writer.write("\t" + o.toString());
					}
					writer.write("\n");
				} else {
					writer.write(name + "\t" + value.toString() + "\n");				
				}
				writer.flush();
			}
			
			zos.flush();
			zos.close();
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

	public DecisionMaker getDecisionMaker() {
		MaxentDecisionMaker decisionMaker = new MaxentDecisionMaker(this.getModel());
		return decisionMaker;
	}
	
	public Map<String, Object> getModelAttributes() {
		return modelAttributes;
	}
	
	public void addModelAttribute(String name, Object value) {
		this.modelAttributes.put(name, value);
	}
	
	public MaxentModel getModel() {
		return model;
	}
	public void setModel(MaxentModel model) {
		this.model = model;
	}
	
	public List<String> getFeatureDescriptors() {
		return featureDescriptors;
	}
	public void setFeatureDescriptors(List<String> featureDescriptors) {
		this.featureDescriptors = featureDescriptors;
	}

	public List<String> getPatternDescriptors() {
		return patternDescriptors;
	}

	public void setPatternDescriptors(List<String> patternDescriptors) {
		this.patternDescriptors = patternDescriptors;
	}
	
	
}
