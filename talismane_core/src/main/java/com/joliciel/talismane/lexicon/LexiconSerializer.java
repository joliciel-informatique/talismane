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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.StringUtils;

/**
 * <p>
 * Given a set of lexicons described in a properties file, serializes them into
 * a zip file, using regex descriptors to read lexical attributes from the
 * files.
 * </p>
 * <p>
 * The structure of the properties file is as follows:
 * </p>
 * <ul>
 * <li><b>lexicons</b>: comma-delimited list of lexicon names, indicating how
 * each lexicon will be referred to within Talismane</li>
 * <li><b><i>lexiconName</i>.file</b>: the file containing the actual lexicon,
 * assumed to be contained in the same directory as the properties file. The
 * lexicon file structure is described in {@link LexiconFile}.</li>
 * <li><b><i>lexiconName</i>.regex</b>: the file containing the various regex
 * patterns used to extract lexical attributes from the lexicon, as described in
 * {@link RegexLexicalEntryReader}.</li>
 * <li><b><i>lexiconName</i>.posTagMap</b>: optional - the file containing the
 * PosTagMap for the lexicon, as described in {@link DefaultPosTagMapper}.</li>
 * <li><b><i>lexiconName</i>.categories</b>: optional - if included, limits the
 * categories that will be loaded to this list.</li>
 * <li><b><i>lexiconName</i>.exclusions</b>: optional - if included, reads a set
 * of exclusions from an exclusion file, where the first row is the list of
 * tab-delimited attribute names to be examined, and the remaining rows are
 * attribute values. If for a given entry all values match, the entry is
 * excluded.</li>
 * <li><b><i>lexiconName</i>.encoding</b>: optional - if included, the lexicon
 * file is read using the provided encoding. If not, it is read in UTF-8. All
 * other files are assumed to be in UTF-8.</li>
 * <li><b><i>lexiconName</i>.uniqueKey</b>: optional - a comma-delimited list of
 * {@link LexicalAttribute}. If included, defines lexical entry uniqueness: only
 * one entry with a given combination of these attributes will be added, and
 * others will be skipped</li>
 * </ul>
 * <p>
 * The order of lexicons is important, as entries will be searched for in the
 * order provided.
 * </p>
 * 
 * @see LexiconFile
 * @see RegexLexicalEntryReader
 * @see DefaultPosTagMapper
 * @author Assaf Urieli
 *
 */
public class LexiconSerializer {
	private static final Logger LOG = LoggerFactory.getLogger(LexiconSerializer.class);

	/**
	 * For arguments, see {@link #serializeLexicons(Map)}.
	 */
	public void serializeLexicons(String[] args) {
		Map<String, String> argMap = StringUtils.convertArgs(args);
		this.serializeLexicons(argMap);
	}

	/**
	 * <p>
	 * The following arguments are read from the map:
	 * </p>
	 * <ul>
	 * <li><b>lexiconProps</b>: the lexicon properties file</li>
	 * <li><b>outFile</b>: the path to the zip file to be created</li>
	 * <li><b>posTagSet</b>: the path to the PosTagSet descriptor file, as
	 * described in {@link PosTagSet}</li>
	 * <li><b>encoding</b>: the encoding used to read configuration files. If
	 * not supplied, the default encoding will be used. Note that the encoding
	 * for the actual lexicon files can be overridden in the lexicon properties
	 * file.</li>
	 * </ul>
	 * <p>
	 * All arguments are mandatory.
	 * </p>
	 */
	public void serializeLexicons(Map<String, String> argMap) {
		try {
			String lexiconPropsPath = null;
			String outFilePath = null;
			String posTagSetPath = null;
			String defaultEncoding = null;

			for (Entry<String, String> entry : argMap.entrySet()) {
				String argName = entry.getKey();
				String argValue = entry.getValue();
				if (argName.equals("lexiconProps"))
					lexiconPropsPath = argValue;
				else if (argName.equals("outFile"))
					outFilePath = argValue;
				else if (argName.equals("posTagSet"))
					posTagSetPath = argValue;
				else if (argName.equals("encoding"))
					defaultEncoding = argValue;
				else
					throw new RuntimeException("Unknown argument: " + argName);
			}

			if (lexiconPropsPath == null)
				throw new RuntimeException("Missing argument: lexiconProps");
			if (outFilePath == null)
				throw new RuntimeException("Missing argument: outFile");
			if (posTagSetPath == null)
				throw new RuntimeException("Missing argument: posTagSet");

			File outFile = null;
			File outDir = null;
			if (outFilePath != null) {
				outFile = new File(outFilePath);
				outDir = outFile.getParentFile();
			}
			if (outDir != null)
				outDir.mkdirs();

			try (FileOutputStream fos = new FileOutputStream(outFile); ZipOutputStream zos = new ZipOutputStream(fos);) {

				File lexiconPropertiesFile = new File(lexiconPropsPath);
				File lexiconDir = lexiconPropertiesFile.getParentFile();

				Charset defaultCharset = Charset.defaultCharset();
				if (defaultEncoding != null)
					defaultCharset = Charset.forName(defaultEncoding);

				zos.putNextEntry(new ZipEntry("lexicon.properties"));
				Writer writer = new BufferedWriter(new OutputStreamWriter(zos, "UTF-8"));
				try (Scanner lexiconPropsScanner = new Scanner(
						new BufferedReader(new InputStreamReader(new FileInputStream(lexiconPropertiesFile), "UTF-8")))) {
					while (lexiconPropsScanner.hasNextLine()) {
						String line = lexiconPropsScanner.nextLine();
						writer.write(line + "\n");
					}
				}
				writer.flush();
				zos.flush();

				Map<String, String> properties = StringUtils.getArgMap(lexiconPropertiesFile, defaultCharset);

				String[] lexiconList = properties.get("lexicons").split(",");

				List<String> knownPropertyList = Arrays.asList("file", "regex", "posTagMap", "categories", "exclusions", "encoding", "uniqueKey");
				Set<String> knownProperties = new HashSet<String>(knownPropertyList);
				for (String property : properties.keySet()) {
					if (property.equals("lexicons")) {
						// nothing to do
					} else {
						boolean foundLexicon = false;
						for (String lexiconName : lexiconList) {
							if (property.startsWith(lexiconName + ".")) {
								foundLexicon = true;
								String remainder = property.substring(lexiconName.length() + 1);
								if (!knownProperties.contains(remainder)) {
									throw new TalismaneException("Unknown property: " + property);
								}
							}
							if (foundLexicon)
								break;
						}
						if (!foundLexicon)
							throw new TalismaneException("Unknown lexicon in property: " + property);
					}
				}

				File posTagSetFile = new File(posTagSetPath);
				Scanner posTagSetScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(posTagSetFile), defaultCharset)));

				TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance("");
				PosTaggerService posTaggerService = talismaneServiceLocator.getPosTaggerServiceLocator().getPosTaggerService();
				PosTagSet posTagSet = posTaggerService.getPosTagSet(posTagSetScanner);

				for (String lexiconName : lexiconList) {
					LOG.debug("Lexicon: " + lexiconName);
					String lexiconFilePath = properties.get(lexiconName + ".file");
					String lexiconRegexPath = properties.get(lexiconName + ".regex");
					String lexiconPosTagMapPath = properties.get(lexiconName + ".posTagMap");
					String lexiconExclusionPath = properties.get(lexiconName + ".exclusions");
					String categoryString = properties.get(lexiconName + ".categories");
					String lexiconEncoding = properties.get(lexiconName + ".encoding");
					String lexiconUniqueKey = properties.get(lexiconName + ".uniqueKey");

					File lexiconRegexFile = new File(lexiconDir, lexiconRegexPath);
					Scanner regexScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(lexiconRegexFile), defaultCharset)));

					File lexiconInputFile = new File(lexiconDir, lexiconFilePath);
					InputStream inputStream = null;
					if (lexiconInputFile.getName().endsWith(".zip")) {
						InputStream inputStream2 = new FileInputStream(lexiconInputFile);
						@SuppressWarnings("resource")
						ZipInputStream zis = new ZipInputStream(inputStream2);
						zis.getNextEntry();
						inputStream = zis;
					} else {
						inputStream = new FileInputStream(lexiconInputFile);
					}

					Charset lexiconCharset = defaultCharset;
					if (lexiconEncoding != null)
						lexiconCharset = Charset.forName(lexiconEncoding);

					Reader reader = new BufferedReader(new InputStreamReader(inputStream, lexiconCharset));
					Scanner lexiconScanner = new Scanner(reader);

					RegexLexicalEntryReader lexicalEntryReader = new RegexLexicalEntryReader(regexScanner);

					regexScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(lexiconRegexFile), defaultCharset)));
					zos.putNextEntry(new ZipEntry(lexiconName + "_regex.txt"));
					while (regexScanner.hasNextLine()) {
						String line = regexScanner.nextLine();
						writer.write(line + "\n");
					}
					regexScanner.close();
					writer.flush();
					zos.flush();

					Set<String> categories = null;
					if (categoryString != null) {
						categories = new HashSet<String>();
						String[] cats = categoryString.split(",");
						for (String cat : cats)
							categories.add(cat);
					}

					List<String> exclusionAttributes = null;
					List<List<String>> exclusions = null;
					if (lexiconExclusionPath != null) {
						exclusions = new ArrayList<List<String>>();
						File lexiconExclusionFile = new File(lexiconDir, lexiconExclusionPath);
						Scanner exclusionScanner = new Scanner(
								new BufferedReader(new InputStreamReader(new FileInputStream(lexiconExclusionFile), defaultCharset)));
						while (exclusionScanner.hasNextLine()) {
							String line = exclusionScanner.nextLine();
							if (line.length() == 0 || line.startsWith("#"))
								continue;
							String[] parts = line.split("\t");
							if (exclusionAttributes == null) {
								exclusionAttributes = new ArrayList<String>();
								for (String part : parts) {
									exclusionAttributes.add(part);
								}
							} else {
								List<String> exclusion = new ArrayList<String>();
								for (String part : parts) {
									exclusion.add(part);
								}
								exclusions.add(exclusion);
							}
						}
						exclusionScanner.close();
					}

					List<LexicalAttribute> uniqueAttributes = null;
					if (lexiconUniqueKey != null) {
						uniqueAttributes = new ArrayList<LexicalAttribute>();
						String[] uniqueKeyElements = lexiconUniqueKey.split(",");
						for (String uniqueKeyElement : uniqueKeyElements) {
							try {
								LexicalAttribute attribute = LexicalAttribute.valueOf(uniqueKeyElement);
								uniqueAttributes.add(attribute);
							} catch (IllegalArgumentException e) {
								lexiconScanner.close();
								throw new TalismaneException("Unknown attribute in " + lexiconName + ".uniqueKey: " + uniqueKeyElement);
							}
						}
					}

					LOG.debug("Serializing: " + lexiconFilePath);

					LexiconFile lexiconFile = new LexiconFile(lexiconName, lexiconScanner, lexicalEntryReader);
					if (categories != null)
						lexiconFile.setCategories(categories);
					if (exclusionAttributes != null)
						lexiconFile.setExclusionAttributes(exclusionAttributes);
					if (exclusions != null)
						lexiconFile.setExclusions(exclusions);
					if (uniqueAttributes != null)
						lexiconFile.setUniqueKeyAttributes(uniqueAttributes);

					lexiconFile.load();
					inputStream.close();
					lexiconFile.setPosTagSet(posTagSet);

					if (lexiconPosTagMapPath != null) {
						File lexiconPosTagMapFile = new File(lexiconDir, lexiconPosTagMapPath);
						@SuppressWarnings("resource")
						Scanner posTagMapScanner = new Scanner(
								new BufferedReader(new InputStreamReader(new FileInputStream(lexiconPosTagMapFile), defaultCharset)));
						PosTagMapper posTagMapper = new DefaultPosTagMapper(posTagMapScanner, posTagSet);
						posTagMapScanner.close();
						lexiconFile.setPosTagMapper(posTagMapper);

						posTagMapScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(lexiconPosTagMapFile), defaultCharset)));
						zos.putNextEntry(new ZipEntry(lexiconName + "_posTagMap.txt"));
						while (posTagMapScanner.hasNextLine()) {
							String line = posTagMapScanner.nextLine();
							writer.write(line + "\n");
						}
						writer.flush();
						zos.flush();
					}

					zos.putNextEntry(new ZipEntry(lexiconName + ".obj"));
					ObjectOutputStream out = new ObjectOutputStream(zos);
					try {
						out.writeObject(lexiconFile);
					} finally {
						out.flush();
					}
					zos.flush();
				}
			}
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
}
