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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.utils.LogUtils;

/**
 * An abstract superclass for machine-learning models.
 * @author Assaf
 *
 */
public abstract class AbstractMachineLearningModel implements MachineLearningModel {
	private static final Log LOG = LogFactory.getLog(AbstractMachineLearningModel.class);
	private Map<String,List<String>> descriptors = new HashMap<String, List<String>>();
	private Map<String, String> modelAttributes = new TreeMap<String, String>();
	private Map<String, Object> dependencies = new HashMap<String, Object>();
	private Collection<ExternalResource<?>> externalResources;
	private ExternalResourceFinder externalResourceFinder;
	
	@Override
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
				String value = this.modelAttributes.get(name);
				writer.write(name + "\t" + value + "\n");				
				writer.flush();
			}
			
			for (String name : this.dependencies.keySet()) {
				Object dependency = this.dependencies.get(name);
				zos.putNextEntry(new ZipEntry(name + "_dependency.obj"));
				ObjectOutputStream oos = new ObjectOutputStream(zos);
				try {
					oos.writeObject(dependency);
				} finally {
					oos.flush();
				}
				zos.flush();
			}
			
			this.persistOtherEntries(zos);

			if (this.externalResources!=null) {
				zos.putNextEntry(new ZipEntry("externalResources.obj"));
				ObjectOutputStream oos = new ObjectOutputStream(zos);
				try {
					oos.writeObject(externalResources);
				} finally {
					oos.flush();
				}
				zos.flush();
			}
			
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
	
	public boolean loadZipEntry(ZipInputStream zis, ZipEntry ze) throws IOException {
    	boolean loaded = true;
    	if (ze.getName().equals("model.bin")) {
    	    this.loadModelFromStream(zis);
    	} else if (ze.getName().equals("externalResources.obj")) {
    		ObjectInputStream in = new ObjectInputStream(zis);
			try {
				@SuppressWarnings("unchecked")
				List<ExternalResource<?>> externalResources = (List<ExternalResource<?>>) in.readObject();
				this.setExternalResources(externalResources);
			} catch (ClassNotFoundException e) {
				LogUtils.logError(LOG, e);
				throw new RuntimeException(e);
			}
    	} else if (ze.getName().endsWith("_descriptors.txt")) {
    		String key = ze.getName().substring(0, ze.getName().length() - "_descriptors.txt".length());
    		Scanner scanner = new Scanner(zis, "UTF-8");
    		List<String> descriptorList = new ArrayList<String>();
    		while (scanner.hasNextLine()) {
    			String descriptor = scanner.nextLine();
    			descriptorList.add(descriptor);
    		}
    		this.getDescriptors().put(key, descriptorList);
    	} else if (ze.getName().endsWith("_dependency.obj")) {
    		String key = ze.getName().substring(0, ze.getName().length() - "_dependency.obj".length());
       		ObjectInputStream in = new ObjectInputStream(zis);
			try {
				Object dependency = in.readObject();
				this.dependencies.put(key, dependency);
			} catch (ClassNotFoundException e) {
				LogUtils.logError(LOG, e);
				throw new RuntimeException(e);
			}
		} else if (ze.getName().equals("attributes.txt")) {
    		Scanner scanner = new Scanner(zis, "UTF-8");
    		while (scanner.hasNextLine()) {
    			String line = scanner.nextLine();
    			if (line.length()>0) {	
	    			String[] parts = line.split("\t");
	    			String name = parts[0];
    				String value = "";
    				if (parts.length>1)
    					value = parts[1];
    				this.addModelAttribute(name, value);
    			}
    		}
    	} else {
    		loaded = this.loadDataFromStream(zis, ze);
    	}
    	return loaded;
	}
	
	protected abstract void persistOtherEntries(ZipOutputStream zos) throws IOException;

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
	public Map<String, String> getModelAttributes() {
		return modelAttributes;
	}
	public void setModelAttributes(Map<String, String> modelAttributes) {
		this.modelAttributes = modelAttributes;
	}


	@Override
	public Map<String, Object> getDependencies() {
		return dependencies;
	}

	@Override
	public void addDependency(String name, Serializable dependency) {
		this.dependencies.put(name, dependency);
	}

	@Override
	public void addModelAttribute(String name, String value) {
		this.modelAttributes.put(name, value);
	}

	@Override
	public List<String> getFeatureDescriptors() {
		return this.descriptors.get(MachineLearningModel.FEATURE_DESCRIPTOR_KEY);
	}

	public Collection<ExternalResource<?>> getExternalResources() {
		return externalResources;
	}

	public void setExternalResources(Collection<ExternalResource<?>> externalResources) {
		this.externalResources = new ArrayList<ExternalResource<?>>(externalResources);
	}

	public ExternalResourceFinder getExternalResourceFinder() {
		if (externalResourceFinder==null) {
			externalResourceFinder = new ExternalResourceFinderImpl();
			for (ExternalResource<?> resource : this.externalResources) {
				externalResourceFinder.addExternalResource(resource);
			}
		}
		return externalResourceFinder;
	}
	
	/**
	 * Load this model's internal binary representation from an input stream.
	 * @param inputStream
	 */
	protected abstract void loadModelFromStream(InputStream inputStream);

	/**
	 * Write this model's internal binary representation to an output stream.
	 * @param outputStream
	 */
	protected abstract void writeModelToStream(OutputStream outputStream);
	
	/**
	 * Loads data from the input stream that is specific to this model type.
	 */
	protected abstract boolean loadDataFromStream(InputStream inputStream, ZipEntry zipEntry);

	@Override
	public void onLoadComplete() { }
	
}
