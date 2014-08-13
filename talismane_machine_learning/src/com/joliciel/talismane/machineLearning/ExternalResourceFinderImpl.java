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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.utils.LogUtils;

class ExternalResourceFinderImpl implements ExternalResourceFinder {
	private static final Log LOG = LogFactory.getLog(ExternalResourceFinderImpl.class);
	private Map<String,ExternalResource<?>> resourceMap = new HashMap<String, ExternalResource<?>>();
	private Map<String,ExternalWordList> wordListMap = new HashMap<String, ExternalWordList>();
	
	@Override
	public ExternalResource<?> getExternalResource(String name) {
		return this.resourceMap.get(name);
	}
	
	public void addExternalResource(ExternalResource<?> externalResource) {
		LOG.debug("Adding resource with name: " + externalResource.getName());
		this.resourceMap.put(externalResource.getName(), externalResource);
	}

	@Override
	public Collection<ExternalResource<?>> getExternalResources() {
		return resourceMap.values();
	}

	@Override
	public void addExternalResources(File externalResourceFile) {
		try {
			if (externalResourceFile.isDirectory()) {
				File[] files = externalResourceFile.listFiles();
				for (File resourceFile : files) {
					this.addExternalResourceFromFile(resourceFile);
				}
			} else {
				this.addExternalResourceFromFile(externalResourceFile);
			}
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	private void addExternalResourceFromFile(File externalResourceFile) throws IOException, ClassNotFoundException {
		LOG.debug("Reading " + externalResourceFile.getName());
		if (externalResourceFile.getName().endsWith(".zip")) {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(externalResourceFile));
			zis.getNextEntry();
			ObjectInputStream ois = new ObjectInputStream(zis);
			Object resourceObject = ois.readObject();
			if (resourceObject instanceof ExternalResource) {
				ExternalResource<?> externalResource = (ExternalResource<?>) resourceObject;
				this.addExternalResource(externalResource);
			} else if (resourceObject instanceof ExternalWordList) {
				ExternalWordList externalWordList = (ExternalWordList) resourceObject;
				this.addExternalWordList(externalWordList);
			}
		} else {
			boolean wordList = false;
			Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(externalResourceFile), "UTF-8")));
			if (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.startsWith("Type: WordList")) {
					wordList = true;
				}
			}
			if (wordList) {
				TextFileWordList textFileWordList = new TextFileWordList(externalResourceFile);
				this.addExternalWordList(textFileWordList);
			} else {
				TextFileResource textFileResource = new TextFileResource(externalResourceFile);
				this.addExternalResource(textFileResource);
			}
		}
	}

	@Override
	public ExternalWordList getExternalWordList(String name) {
		return this.wordListMap.get(name);
	}

	@Override
	public void addExternalWordList(ExternalWordList externalWordList) {
		LOG.debug("Adding word list with name: " + externalWordList.getName());
		this.wordListMap.put(externalWordList.getName(), externalWordList);
	}

	@Override
	public Collection<ExternalWordList> getExternalWordLists() {
		return wordListMap.values();
	}
}
