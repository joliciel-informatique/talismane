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
package com.joliciel.talismane.machineLearning.maxent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipInputStream;

import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.MachineLearningServiceLocator;

import opennlp.model.Context;
import opennlp.model.IndexHashTable;
import opennlp.model.MaxentModel;

/**
 * A class for writing a MaxEnt model to a CSV file.
 * @author Assaf Urieli
 *
 */
public class MaxentModelCSVExporter {
	public static void main(String[] args) throws Exception {
		String maxentModelFile = null;
		boolean top100 = false;
		String excludeListPath = null;
		
		for (String arg : args) {
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos+1);
			if (argName.equals("model"))
				maxentModelFile = argValue;
			else if (argName.equals("top100"))
				top100 = argValue.equals("true");
			else if (argName.equals("excludeList"))
				excludeListPath = argValue;
			else
				throw new RuntimeException("Unknown argument: " + argName);
		}

		MachineLearningServiceLocator locator = MachineLearningServiceLocator.getInstance();
		MachineLearningService machineLearningService = locator.getMachineLearningService();
		ZipInputStream zis = new ZipInputStream(new FileInputStream(maxentModelFile));
		OpenNLPModel machineLearningModel = (OpenNLPModel) machineLearningService.getClassificationModel(zis);
		
		MaxentModel model = machineLearningModel.getModel();
		Object[] dataStructures = model.getDataStructures();
		Context[] modelParameters = (Context[]) dataStructures[0];
		@SuppressWarnings("unchecked")
		IndexHashTable<String> predicateTable = (IndexHashTable<String>) dataStructures[1];
		String[] outcomeNames = (String[]) dataStructures[2];
		String[] predicates = new String[predicateTable.size()];
		predicateTable.toArray(predicates);
		Writer csvFileWriter = null;
		
		File csvFile = new File(maxentModelFile + ".model.csv");
		csvFile.delete();
		csvFile.createNewFile();
		csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));

		Set<String> excludeList = null;
		if (excludeListPath!=null) {
			excludeList = new HashSet<String>();
			File excludeListFile = new File(excludeListPath);
			Scanner excludeListScanner = new Scanner(excludeListFile);
			while (excludeListScanner.hasNextLine()) {
				String excludeItem = excludeListScanner.nextLine();
				if (!excludeItem.startsWith("#"))
					excludeList.add(excludeItem);
			}
		}
		
		try {
			if (top100) {
				Map<String,Integer> outcomeMap = new TreeMap<String, Integer>();
				for (int i=0;i<outcomeNames.length;i++) {
					String outcomeName = outcomeNames[i];
					outcomeMap.put(outcomeName, i);
				}
				for (String outcome : outcomeMap.keySet()) {
					csvFileWriter.write(outcome + ",,");
				}
				csvFileWriter.write("\n");
				
				Map<String,Set<MaxentParameter>> outcomePredicates = new HashMap<String, Set<MaxentParameter>>();
				for (int i=0;i<modelParameters.length;i++) {
					Context context = modelParameters[i];
					int[] outcomeIndexes = context.getOutcomes();
					double[] parameters = context.getParameters();
					for (int j=0;j<outcomeIndexes.length;j++) {
						int outcomeIndex = outcomeIndexes[j];
						String outcomeName = outcomeNames[outcomeIndex];
						double value = parameters[j];
						Set<MaxentParameter> outcomeParameters = outcomePredicates.get(outcomeName);
						if (outcomeParameters==null) {
							outcomeParameters = new TreeSet<MaxentParameter>();
							outcomePredicates.put(outcomeName, outcomeParameters);
						}
						MaxentParameter param = new MaxentParameter(predicates[i], value);
						outcomeParameters.add(param);
					}
				}
				
				for (int i=0;i<100;i++) {
					for (String outcome : outcomeMap.keySet()) {
						Set<MaxentParameter> outcomeParameters = outcomePredicates.get(outcome);
						if (outcomeParameters==null) {
							csvFileWriter.write(",,");
						} else {
							Iterator<MaxentParameter> iParams = outcomeParameters.iterator();
							MaxentParameter param = null;
							for (int j=0;j<=i;j++) {
								if (iParams.hasNext()) {
									param = iParams.next();
								} else {
									param = null;
									break;
								}
							}
							if (param==null)
								csvFileWriter.write(",,");
							else
								csvFileWriter.write("\"" + param.getPredicate() + "\"," + param.getValue() + ",");
						}
					}
					csvFileWriter.write("\n");
				}
			} else {
				csvFileWriter.write("predicate,");
				for (String outcomeName : outcomeNames) {
					csvFileWriter.write(outcomeName + ",");
				}
				csvFileWriter.write("\n");
				
				int i = 0;
				for (String predicate : predicates) {
					if (excludeList!=null) {
						boolean excludeMe = false;
						for(String excludeItem : excludeList) {
							if (predicate.startsWith(excludeItem)) {
								excludeMe = true;
								break;
							}
						}
						
						if (excludeMe) {
							i++;
							continue;
						}
					}
					
					csvFileWriter.write("\"" + predicate + "\",");
					Context context = modelParameters[i];
					int[] outcomeIndexes = context.getOutcomes();
					double[] parameters = context.getParameters();
					for (int j=0;j<outcomeNames.length;j++) {
						int paramIndex = -1;
						for (int k=0;k<outcomeIndexes.length;k++) {
							if (outcomeIndexes[k]==j) {
								paramIndex = k;
								break;
							}
						}
						double value = 0.0;
						if (paramIndex>=0)
							value = parameters[paramIndex];
						csvFileWriter.write(value + ",");
					}
					csvFileWriter.write("\n");
					i++;
				}
			}
		} finally {
			if (csvFileWriter!=null) {
				csvFileWriter.flush();
				csvFileWriter.close();
			}
		}
	}
	
	private static class MaxentParameter implements Comparable<MaxentParameter> {
		private String predicate;
		private double value;
		public MaxentParameter(String predicate, double value) {
			this.predicate = predicate;
			this.value = value;
		}
		public String getPredicate() {
			return predicate;
		}
		public double getValue() {
			return value;
		}
		@Override
		public int compareTo(MaxentParameter o) {
			if (this.getValue()==o.getValue()) {
				return this.getPredicate().compareTo(o.getPredicate());
			}
			if (o.getValue()>this.getValue())
				return 1;
			return -1;
		}

		
		
	}
}
