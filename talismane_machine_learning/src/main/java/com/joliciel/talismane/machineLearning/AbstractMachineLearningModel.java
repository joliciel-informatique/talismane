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
package com.joliciel.talismane.machineLearning;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * An abstract superclass for machine-learning models.
 * 
 * @author Assaf
 *
 */
public abstract class AbstractMachineLearningModel implements MachineLearningModel {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(AbstractMachineLearningModel.class);
	private Map<String, List<String>> descriptors = new HashMap<String, List<String>>();
	private Map<String, Object> modelAttributes = new TreeMap<String, Object>();
	private Map<String, Object> dependencies = new HashMap<String, Object>();
	private Collection<ExternalResource<?>> externalResources;
	private ExternalResourceFinder externalResourceFinder;
	private Config config;

	public AbstractMachineLearningModel() {
		super();
	}

	public AbstractMachineLearningModel(Config config, Map<String, List<String>> descriptors) {
		super();
		this.descriptors = descriptors;
		this.config = config;
	}

	@Override
	public final void persist(File modelFile) throws IOException {
		this.persist(new FileOutputStream(modelFile, false));
	}

	@Override
	public final void persist(OutputStream outputStream) throws IOException {
		ZipOutputStream zos = new ZipOutputStream(outputStream);
		Writer writer = new BufferedWriter(new OutputStreamWriter(zos, "UTF-8"));

		zos.putNextEntry(new ZipEntry("algorithm.txt"));
		writer.write(this.getAlgorithm().name());
		writer.flush();
		zos.flush();

		zos.putNextEntry(new ZipEntry("config.txt"));
		writer.write(this.getConfig().root().render());
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

		zos.putNextEntry(new ZipEntry("attributes.obj"));
		ObjectOutputStream attributesOos = new ObjectOutputStream(zos);
		try {
			attributesOos.writeObject(this.getModelAttributes());
		} finally {
			attributesOos.flush();
		}
		zos.flush();

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

		if (this.externalResources != null) {
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
	}

	@Override
	public boolean loadZipEntry(ZipInputStream zis, ZipEntry ze) throws IOException, ClassNotFoundException {
		boolean loaded = true;
		if (ze.getName().equals("model.bin")) {
			this.loadModelFromStream(zis);
		} else if (ze.getName().equals("externalResources.obj")) {
			ObjectInputStream in = new ObjectInputStream(zis);
			@SuppressWarnings("unchecked")
			List<ExternalResource<?>> externalResources = (List<ExternalResource<?>>) in.readObject();
			this.setExternalResources(externalResources);
		} else if (ze.getName().endsWith("_descriptors.txt")) {
			String key = ze.getName().substring(0, ze.getName().length() - "_descriptors.txt".length());
			@SuppressWarnings("resource")
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

			Object dependency = in.readObject();
			this.dependencies.put(key, dependency);
		} else if (ze.getName().equals("attributes.obj")) {
			ObjectInputStream in = new ObjectInputStream(zis);

			@SuppressWarnings("unchecked")
			Map<String, Object> attributes = (Map<String, Object>) in.readObject();
			this.setModelAttributes(attributes);
		} else if (ze.getName().equals("attributes.txt")) {
			// for backwards compatibility, when attributes where always strings
			@SuppressWarnings("resource")
			Scanner scanner = new Scanner(zis, "UTF-8");
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.length() > 0) {
					String[] parts = line.split("\t");
					String name = parts[0];
					String value = "";
					if (parts.length > 1)
						value = parts[1];
					this.addModelAttribute(name, value);
				}
			}
		} else if (ze.getName().equals("config.txt")) {
			Reader reader = new InputStreamReader(zis, "UTF-8");

			this.config = ConfigFactory.parseReader(reader);
		} else {
			loaded = this.loadDataFromStream(zis, ze);
		}
		return loaded;
	}

	protected abstract void persistOtherEntries(ZipOutputStream zos) throws IOException;

	/**
	 * Write data to the stream that's specific to this model.
	 * 
	 * @throws IOException
	 */
	public abstract void writeDataToStream(ZipOutputStream zos) throws IOException;

	@Override
	public Map<String, List<String>> getDescriptors() {
		return descriptors;
	}

	public void setDescriptors(Map<String, List<String>> descriptors) {
		this.descriptors = descriptors;
	}

	@Override
	public Map<String, Object> getModelAttributes() {
		return modelAttributes;
	}

	public void setModelAttributes(Map<String, Object> modelAttributes) {
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
	public void addModelAttribute(String name, Object value) {
		this.modelAttributes.put(name, value);
	}

	@Override
	public List<String> getFeatureDescriptors() {
		return this.descriptors.get(MachineLearningModel.FEATURE_DESCRIPTOR_KEY);
	}

	@Override
	public Collection<ExternalResource<?>> getExternalResources() {
		return externalResources;
	}

	@Override
	public void setExternalResources(Collection<ExternalResource<?>> externalResources) {
		this.externalResources = new ArrayList<ExternalResource<?>>(externalResources);
	}

	@Override
	public ExternalResourceFinder getExternalResourceFinder() {
		if (externalResourceFinder == null) {
			externalResourceFinder = new ExternalResourceFinder();
			for (ExternalResource<?> resource : this.externalResources) {
				externalResourceFinder.addExternalResource(resource);
			}
		}
		return externalResourceFinder;
	}

	/**
	 * Load this model's internal binary representation from an input stream.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	protected abstract void loadModelFromStream(InputStream inputStream) throws UnsupportedEncodingException, IOException, ClassNotFoundException;

	/**
	 * Write this model's internal binary representation to an output stream.
	 * 
	 * @throws IOException
	 */
	protected abstract void writeModelToStream(OutputStream outputStream) throws IOException;

	/**
	 * Loads data from the input stream that is specific to this model type.
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	protected abstract boolean loadDataFromStream(InputStream inputStream, ZipEntry zipEntry) throws IOException, ClassNotFoundException;

	@Override
	public Config getConfig() {
		return config;
	}
}
