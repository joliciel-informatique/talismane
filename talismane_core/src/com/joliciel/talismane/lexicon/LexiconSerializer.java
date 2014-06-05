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
package com.joliciel.talismane.lexicon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.Scanner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.StringUtils;
import com.joliciel.talismane.utils.io.DirectoryReader;

/**
 * Used to serialize a set of lexicon files found in a given directory.
 * @author Assaf Urieli
 *
 */
public abstract class LexiconSerializer {
	private static final Log LOG = LogFactory.getLog(LexiconSerializer.class);

	public void serializeLexicons(String[] args) {
		Map<String,String> argMap = StringUtils.convertArgs(args);
		this.serializeLexicons(argMap);
	}
	
	public void serializeLexicons(Map<String,String> argMap) {
		try {
			String lexiconDirPath = null;
			String outDirPath = null;
			String outFilePath = null;
			String lexiconPatternPath = null;
			String regex = null;

			for (Entry<String, String> entry : argMap.entrySet()) {
				String argName = entry.getKey();
				String argValue = entry.getValue();
				if (argName.equals("lexiconDir"))
					lexiconDirPath = argValue;
				else if (argName.equals("outFile"))
					outFilePath = argValue;
				else if (argName.equals("outDir"))
					outDirPath = argValue;
				else if (argName.equals("lexiconPattern"))
					lexiconPatternPath = argValue;
				else if (argName.equals("regex"))
					regex = argValue;
				else
					throw new RuntimeException("Unknown argument: " + argName);
			}

			if (lexiconDirPath==null)
				throw new RuntimeException("Missing argument: lexiconDir");
			if (outDirPath==null && outFilePath==null)
				throw new RuntimeException("Missing argument: outDir or outFile");
			if (lexiconPatternPath==null && regex==null)
				throw new RuntimeException("Missing argument: lexiconPattern or regex");

			File outFile = null;
			File outDir = null;
			if (outFilePath!=null) {
				outFile = new File(outFilePath);
				outDir = outFile.getParentFile();
			} else {
				outDir = new File(outDirPath);
			}
			if (outDir!=null)
				outDir.mkdirs();

			File lexiconDir = new File(lexiconDirPath);
			File[] lexiconFiles = lexiconDir.listFiles();

			if (regex==null) {
				File lexiconPatternFile = new File(lexiconPatternPath);
				Scanner lexiconPatternScanner = new Scanner(lexiconPatternFile);
				if (lexiconPatternScanner.hasNextLine()) {
					regex = lexiconPatternScanner.nextLine();
				}
			}
			
			RegexLexicalEntryReader lexicalEntryReader = new RegexLexicalEntryReader(this.getMorphologyReader());
			lexicalEntryReader.setRegex(regex);
			
			if (outFile!=null) {
				DirectoryReader directoryReader = new DirectoryReader(lexiconDir, StandardCharsets.UTF_8);
				directoryReader.setEndOfFileString("\n");
				LOG.debug("Serializing: " + lexiconDir.getName());
				LexiconFile lexiconFile = new LexiconFile(lexicalEntryReader, directoryReader);

				String fileNameBase = outFile.getName();
				if (fileNameBase.indexOf('.')>=0)
					fileNameBase = fileNameBase.substring(0, fileNameBase.lastIndexOf('.'));
				this.serializeLexicon(outFile, fileNameBase, lexiconFile);				
			} else {
				for (File inFile : lexiconFiles) {
					LOG.debug("Serializing: " + inFile.getName());
					LexiconFile lexiconFile = new LexiconFile(lexicalEntryReader, inFile);
	
					String fileNameBase = inFile.getName();
					if (fileNameBase.indexOf('.')>=0)
						fileNameBase = fileNameBase.substring(0, fileNameBase.lastIndexOf('.'));
	
					outFile = new File(outDir, fileNameBase + ".zip");
					this.serializeLexicon(outFile, fileNameBase, lexiconFile);
				}
			}
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	private void serializeLexicon(File outFile, String fileNameBase, LexiconFile lexiconFile) {
		FileOutputStream fos = null;
		ZipOutputStream zos = null;
		ObjectOutputStream out = null;
		try
		{
			fos = new FileOutputStream(outFile);
			zos = new ZipOutputStream(fos);
			zos.putNextEntry(new ZipEntry(fileNameBase + ".obj"));
			out = new ObjectOutputStream(zos);

			try {
				out.writeObject(lexiconFile);
			} finally {
				out.flush();
				out.close();
			}
		} catch(IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	protected abstract LexicalEntryMorphologyReader getMorphologyReader();
}
