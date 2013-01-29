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
package com.joliciel.talismane.lexicon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Scanner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.utils.LogUtils;

/**
 * Used to serialize a set of lexicon files found in a given directory.
 * @author Assaf
 *
 */
public abstract class LexiconSerializer {
	private static final Log LOG = LogFactory.getLog(LexiconSerializer.class);

	public void serializeLexicons(String[] args) {
		try {
			String lexiconDirPath = "";
			String outDirPath = "";
			String posTagSetPath = "";
			String lexiconPatternPath = "";

			for (String arg : args) {
				int equalsPos = arg.indexOf('=');
				String argName = arg.substring(0, equalsPos);
				String argValue = arg.substring(equalsPos+1);
				if (argName.equals("lexiconDir"))
					lexiconDirPath = argValue;
				else if (argName.equals("outDir"))
					outDirPath = argValue;
				else if (argName.equals("posTagSet"))
					posTagSetPath = argValue;
				else if (argName.equals("lexiconPattern"))
					lexiconPatternPath = argValue;
				else
					throw new RuntimeException("Unknown argument: " + argName);
			}

			if (lexiconDirPath.length()==0)
				throw new RuntimeException("Missing argument: lexiconDir");
			if (outDirPath.length()==0)
				throw new RuntimeException("Missing argument: outDir");
			if (posTagSetPath.length()==0)
				throw new RuntimeException("Missing argument: posTagSet");
			if (lexiconPatternPath.length()==0)
				throw new RuntimeException("Missing argument: lexiconPattern");

			File outDir = new File(outDirPath);
			outDir.mkdirs();

			TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance();

			PosTaggerService posTaggerService = talismaneServiceLocator.getPosTaggerServiceLocator().getPosTaggerService();
			File posTagSetFile = new File(posTagSetPath);
			PosTagSet posTagSet = posTaggerService.getPosTagSet(posTagSetFile);

			TalismaneSession.setPosTagSet(posTagSet);

			File lexiconDir = new File(lexiconDirPath);
			File[] lexiconFiles = lexiconDir.listFiles();

			File lexiconPatternFile = new File(lexiconPatternPath);
			Scanner lexiconPatternScanner = new Scanner(lexiconPatternFile);
			String regex = null;
			if (lexiconPatternScanner.hasNextLine()) {
				regex = lexiconPatternScanner.nextLine();
			}
			RegexLexicalEntryReader lexicalEntryReader = new RegexLexicalEntryReader(this.getMorphologyReader());
			lexicalEntryReader.setRegex(regex);
			for (File inFile : lexiconFiles) {
				LOG.debug("Serializing: " + inFile.getName());
				LexiconFile lexiconFile = new LexiconFile(lexicalEntryReader, inFile);
				lexiconFile.setPosTagSet(posTagSet);

				FileOutputStream fos = null;
				ObjectOutputStream out = null;
				String fileNameBase = inFile.getName();
				if (fileNameBase.indexOf('.')>=0) {
					fileNameBase = fileNameBase.substring(0, fileNameBase.lastIndexOf('.'));

					File outFile = new File(outDir, fileNameBase + ".obj");
					try
					{
						fos = new FileOutputStream(outFile);
						out = new ObjectOutputStream(fos);

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
			}
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	protected abstract LexicalEntryMorphologyReader getMorphologyReader();
}
