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
package com.joliciel.talismane.machineLearning;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.utils.LogUtils;

/**
 * An abstract superclass for machine-learning models.
 * @author Assaf
 *
 */
public abstract class AbstractMachineLearningModel<T extends Outcome> implements MachineLearningModel<T> {
	private static final Log LOG = LogFactory.getLog(AbstractMachineLearningModel.class);
	private Map<String,List<String>> descriptors = new HashMap<String, List<String>>();
	private Map<String, Object> modelAttributes = new TreeMap<String, Object>();
	private DecisionFactory<T> decisionFactory;
	
	@Override
	@SuppressWarnings("rawtypes")
	public final void persist(File modelFile) {
		try {
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(modelFile,false));
			Writer writer = new BufferedWriter(new OutputStreamWriter(zos, "UTF-8"));

			zos.putNextEntry(new ZipEntry("algorithm.txt"));
			writer.write(this.getAlgorithm().name());
			writer.flush();
			zos.flush();
			
			for (String descriptorKey : descriptors.keySet()) {
				zos.putNextEntry(new ZipEntry(descriptorKey + "_descriptors.txt"));
				List<String> descriptorList = descriptors.get(descriptorKey);
				for (String descriptor : descriptorList) {
					writer.write(descriptor + "\n");
					writer.flush();
				}
				zos.flush();
				
			}
			
			zos.putNextEntry(new ZipEntry("attributes.txt"));
			for (String name : this.modelAttributes.keySet()) {
				Object value = this.modelAttributes.get(name);
				if (value==null) {
					writer.write(name + "\tnull\n");				
				} else if (value instanceof Collection) {
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
			
			zos.putNextEntry(new ZipEntry("decisionFactory.obj"));
			ObjectOutputStream out = new ObjectOutputStream(zos);

			try {
				out.writeObject(decisionFactory);
			} finally {
				out.flush();
			}
			
			zos.flush();
			
			this.writeDataToStream(zos);
			
			zos.putNextEntry(new ZipEntry("model.bin"));
			this.writeModelToStream(zos);
			zos.flush();

			zos.close();
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	/**
	 * Write data to the stream that's specific to this model.
	 * @param zos
	 */
	public abstract void writeDataToStream(ZipOutputStream zos);
	
	public Map<String, List<String>> getDescriptors() {
		return descriptors;
	}
	public void setDescriptors(Map<String, List<String>> descriptors) {
		this.descriptors = descriptors;
	}
	public Map<String, Object> getModelAttributes() {
		return modelAttributes;
	}
	public void setModelAttributes(Map<String, Object> modelAttributes) {
		this.modelAttributes = modelAttributes;
	}
	public DecisionFactory<T> getDecisionFactory() {
		return decisionFactory;
	}
	public void setDecisionFactory(DecisionFactory<T> decisionFactory) {
		this.decisionFactory = decisionFactory;
	}
	
	@Override
	public void addModelAttribute(String name, Object value) {
		this.modelAttributes.put(name, value);
	}
	

	@Override
	public List<String> getFeatureDescriptors() {
		return this.descriptors.get(MachineLearningModel.FEATURE_DESCRIPTOR_KEY);
	}
}
