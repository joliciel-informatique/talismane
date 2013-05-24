///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.utils.JolicielException;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * An external resource read from a text file.<br/>
 * The default name will be the filename.<br/>
 * If a line starts with the string "Name: ", the default name will be replaced by this name.<br/>
 * If a line starts with the string "Multivalued: true", the resource will be considered multivalued with weights.<br/>
 * All lines starting with # are skipped.<br/>
 * Any other line will be broken up by tabs:<br/>
 * For multi-valued resources, the second-to-last tab is the class, the last tab is the weight.<br/>
 * For normal resources, the last tab is the class.<br/>
 * All previous tabs are considered to be key components.<br/>
 * The same set of key components can have multiple classes with different weights.<br/>
 * @author Assaf Urieli
 *
 */
public class TextFileResource implements ExternalResource {
	private static final long serialVersionUID = -6588854532682308236L;
	private static final Log LOG = LogFactory.getLog(TextFileResource.class);
	Map<String, List<WeightedOutcome<String>>> resultsMap = new HashMap<String, List<WeightedOutcome<String>>>();
	Map<String, String> resultMap = new HashMap<String, String>();
	
	private String name;
	private boolean multivalued = false;
	
	public TextFileResource(File file) {
		try {
			this.name = file.getName();
			this.multivalued = false;
			Scanner scanner = new Scanner(file);
			int numParts = -1;
			int i=1;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.length()>0 && !line.startsWith("#")) {
					StringBuilder sb = new StringBuilder();
					String[] parts = line.split("\t");
					if (parts.length==1 && line.startsWith("Name: ")) {
						this.name = line.substring("Name: ".length());
						i++;
						continue;
					}
					if (parts.length==1 && line.startsWith("Multivalued: ")) {
						this.multivalued = line.substring("Multivalued: ".length()).equalsIgnoreCase("true");
						i++;
						continue;
					}
					if (numParts<0) numParts = parts.length;
					if (parts.length!=numParts)
						throw new JolicielException("Wrong number of elements on line " + i + " in file: " + file.getName() );

					if (multivalued) {
						for (int j=0; j<numParts-2; j++) {
							sb.append(parts[j]);
							sb.append("|");
						}
						String key = sb.toString();
						List<WeightedOutcome<String>> resultList = resultsMap.get(key);
						if (resultList==null) {
							resultList = new ArrayList<WeightedOutcome<String>>(1);
							resultsMap.put(key, resultList);
						}
						String outcome = parts[numParts-2];
						double weight = Double.parseDouble(parts[numParts-1]);
						resultList.add(new WeightedOutcome<String>(outcome, weight));
					} else {
						for (int j=0; j<numParts-1; j++) {
							sb.append(parts[j]);
							sb.append("|");
						}
						String key = sb.toString();
						String value = parts[numParts-1];
						resultMap.put(key, value);
					}
				}
				i++;
			}
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	@Override
	public List<WeightedOutcome<String>> getResults(List<String> keyElements) {
		StringBuilder sb = new StringBuilder();
		for (String keyElement : keyElements) {
			sb.append(keyElement);
			sb.append("|");
		}
		String key = sb.toString();
		List<WeightedOutcome<String>> resultList = null;
		if (multivalued) {
			resultList = resultsMap.get(key);
		} else {
			String result = resultMap.get(key);
			if (result!=null) {
				resultList = new ArrayList<WeightedOutcome<String>>();
				resultList.add(new WeightedOutcome<String>(result, 1.0));
			}
		}
		return resultList;
	}
	
	@Override
	public String getResult(List<String> keyElements) {
		StringBuilder sb = new StringBuilder();
		for (String keyElement : keyElements) {
			sb.append(keyElement);
			sb.append("|");
		}
		String key = sb.toString();
		String result = null;
		if (multivalued) {
			List<WeightedOutcome<String>> resultList = resultsMap.get(key);
			if (resultList!=null && resultList.size()>0) {
				result = resultList.get(0).getOutcome();
			}
		} else {
			result = resultMap.get(key);
		}
		return result;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean isMultivalued() {
		return this.multivalued;
	}

}
