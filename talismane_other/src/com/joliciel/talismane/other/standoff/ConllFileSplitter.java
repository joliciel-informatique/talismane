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
package com.joliciel.talismane.other.standoff;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Scanner;

import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.TalismaneException;

/**
 * Class for splitting a CoNNL file into lots of smaller files.
 * @author Assaf Urieli
 *
 */
public class ConllFileSplitter {
	private static DecimalFormat df = new DecimalFormat("000");
	
	/**
	 * @param args
	 */
	public void split(String filePath, int startIndex, String encoding) throws Exception {
		String fileBase = filePath;
		if (filePath.indexOf('.')>0)
			fileBase = filePath.substring(0,filePath.lastIndexOf('.'));
		File file = new File(filePath);
		Scanner scanner = new Scanner(file, encoding);
		boolean hasSentence = false;
		int currentFileIndex = startIndex;
		
		int sentenceCount = 0;
		Writer writer = null;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.length()==0 && !hasSentence) {
				continue;
			} else if (line.length()==0) {
				writer.write("\n");
				writer.flush();
				hasSentence=false;
			} else {
				if (!hasSentence) {
					hasSentence = true;
					sentenceCount++;
				}
				if (writer==null || sentenceCount % 20==0) {
					if (writer!=null) {
						writer.flush();
						writer.close();
					}
					File outFile = new File(fileBase + "_" + df.format(currentFileIndex) + ".tal");
					outFile.delete();
					outFile.createNewFile();
					writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile, false), encoding));
					currentFileIndex++;
					hasSentence=false;
				}

				writer.write(line + "\n");
				writer.flush();
			}
		}
		if (writer!=null) {
			writer.flush();
			writer.close();
		}
	}

	public static void main(String[] args) throws Exception {
    	Map<String,String> innerArgs = TalismaneConfig.convertArgs(args);
		String filePath = null;
		if (innerArgs.containsKey("inFile"))
			filePath = innerArgs.get("inFile");
		else
			throw new TalismaneException("Missing option: inFile");
		
		int startIndex = 1;
		if (innerArgs.containsKey("startIndex"))  {
			startIndex = Integer.parseInt(innerArgs.get("startIndex"));
		}
		
		String encoding = "UTF-8";
		if (innerArgs.containsKey("encoding"))
			encoding = innerArgs.get("encoding");
		ConllFileSplitter splitter = new ConllFileSplitter();
		splitter.split(filePath, startIndex, encoding);
	}
}
