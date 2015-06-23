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
package com.joliciel.talismane.extensions.standoff;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.StringUtils;

/**
 * Class for splitting a CoNNL file into lots of smaller files.
 * @author Assaf Urieli
 *
 */
public class ConllFileSplitter {
	private static final Log LOG = LogFactory.getLog(ConllFileSplitter.class);
	
	private static DecimalFormat df = new DecimalFormat("000");
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws UnsupportedEncodingException 
	 */
	public void split(String filePath, int startIndex, int sentencesPerFile, String encoding) {
		try {
			String fileBase = filePath;
			if (filePath.indexOf('.')>0)
				fileBase = filePath.substring(0,filePath.lastIndexOf('.'));
			File file = new File(filePath);
			Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding)));
	
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
					if (writer==null || sentenceCount % sentencesPerFile==0) {
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
			scanner.close();
			if (writer!=null) {
				writer.flush();
				writer.close();
			}
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		Map<String,String> argMap = StringUtils.convertArgs(args);
		ConllFileSplitter splitter = new ConllFileSplitter();
		splitter.process(argMap);
	}
	
	public void process(Map<String,String> args) {
		String filePath = null;
		if (args.containsKey("inFile"))
			filePath = args.get("inFile");
		else
			throw new TalismaneException("Missing option: inFile");
		
		int startIndex = 1;
		if (args.containsKey("startIndex"))  {
			startIndex = Integer.parseInt(args.get("startIndex"));
		}
		
		int sentencesPerFile = 20;
		if (args.containsKey("sentencesPerFile"))  {
			sentencesPerFile = Integer.parseInt(args.get("sentencesPerFile"));
		}
		
		String encoding = "UTF-8";
		if (args.containsKey("encoding"))
			encoding = args.get("encoding");
		
		this.split(filePath, startIndex, sentencesPerFile, encoding);
	}
}
