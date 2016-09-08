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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.utils.JolicielException;

/**
 * Finds the external resource corresponding to a given name.
 * 
 * @author Assaf Urieli
 *
 */
public class ExternalResourceFinder {
	private static final Logger LOG = LoggerFactory.getLogger(ExternalResourceFinder.class);
	private Map<String, ExternalResource<?>> resourceMap = new HashMap<String, ExternalResource<?>>();

	public ExternalResource<?> getExternalResource(String name) {
		return this.resourceMap.get(name);
	}

	public void addExternalResource(ExternalResource<?> externalResource) {
		LOG.debug("Adding resource with name: " + externalResource.getName());
		this.resourceMap.put(externalResource.getName(), externalResource);
	}

	public Collection<ExternalResource<?>> getExternalResources() {
		return resourceMap.values();
	}

	/**
	 * Add external resources located in a scanner from a particular filename.
	 */
	public void addExternalResource(String fileName, Scanner scanner) {
		LOG.debug("Reading " + fileName);
		String typeLine = scanner.nextLine();

		if (!typeLine.startsWith("Type: "))
			throw new JolicielException("In file " + fileName + ", expected line starting with \"Type: \"");

		String type = typeLine.substring("Type: ".length());

		if ("KeyValue".equals(type)) {
			TextFileResource textFileResource = new TextFileResource(fileName, scanner);
			this.addExternalResource(textFileResource);
		} else if ("KeyMultiValue".equals(type)) {
			TextFileMultivaluedResource resource = new TextFileMultivaluedResource(fileName, scanner);
			this.addExternalResource(resource);
		} else {
			throw new JolicielException("Unexpected type in file: " + fileName + ": " + type);
		}
	}
}
