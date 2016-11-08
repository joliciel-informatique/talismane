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
package com.joliciel.talismane;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.apache.commons.vfs2.FileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.lexicon.Diacriticizer;
import com.joliciel.talismane.lexicon.EmptyLexicon;
import com.joliciel.talismane.lexicon.LexiconChain;
import com.joliciel.talismane.lexicon.LexiconDeserializer;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.parser.ArcEagerTransitionSystem;
import com.joliciel.talismane.parser.ShiftReduceTransitionSystem;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.resources.WordListFinder;
import com.joliciel.talismane.utils.ConfigUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * A class storing session-wide reference data and objects.
 * 
 * @author Assaf Urieli
 *
 */
public class TalismaneSession {
	private static final Logger LOG = LoggerFactory.getLogger(TalismaneSession.class);

	// various static maps for ensuring we don't load the same large resource
	// multiple times if multiple talismane configurations share the same
	// resource
	private static final Map<String, Diacriticizer> diacriticizerMap = new HashMap<>();
	private static final Map<String, List<PosTaggerLexicon>> lexiconMap = new HashMap<>();

	private final String sessionId;
	private final Locale locale;
	private final PosTagSet posTagSet;
	private final List<PosTaggerLexicon> lexicons = new ArrayList<>();
	private final PosTaggerLexicon mergedLexicon;
	private final TransitionSystem transitionSystem;
	private LinguisticRules linguisticRules;
	private Diacriticizer diacriticizer;
	private String outputDivider = "";
	private final WordListFinder wordListFinder = new WordListFinder();
	private final ExternalResourceFinder externalResourceFinder = new ExternalResourceFinder();
	private final Charset inputCharset;
	private final Charset outputCharset;
	private final String baseName;
	private final String suffix;
	private final String fileName;
	private final File outDir;
	private final Map<String, String> lowercasePreferences = new HashMap<>();

	/**
	 * 
	 * @param config
	 *            The configuration to use for the current session
	 * @param sessionId
	 *            A unique session id, which should be tied to a unique
	 *            configuration - options include the path to the configuration
	 *            file. This id can be used for caching objects related to this
	 *            configuration.
	 * @throws IOException
	 *             if a problem occurred when reading resources referred to by
	 *             the configuration
	 * @throws ClassNotFoundException
	 *             if a resource contains the wrong serialized class or version
	 */
	public TalismaneSession(Config config, String sessionId) throws IOException, ClassNotFoundException {
		this.sessionId = sessionId;

		config.checkValid(ConfigFactory.defaultReference(), "talismane.core");

		Config talismaneConfig = config.getConfig("talismane.core");

		String encoding = null;
		if (talismaneConfig.hasPath("encoding"))
			encoding = talismaneConfig.getString("encoding");

		String inputEncoding = encoding;
		String outputEncoding = encoding;
		if (talismaneConfig.hasPath("input-encoding"))
			inputEncoding = talismaneConfig.getString("input-encoding");
		if (talismaneConfig.hasPath("output-encoding"))
			outputEncoding = talismaneConfig.getString("output-encoding");

		if (inputEncoding == null)
			inputCharset = Charset.defaultCharset();
		else
			inputCharset = Charset.forName(inputEncoding);
		if (outputEncoding == null)
			outputCharset = Charset.defaultCharset();
		else
			outputCharset = Charset.forName(outputEncoding);

		locale = Locale.forLanguageTag(talismaneConfig.getString("locale"));

		this.suffix = talismaneConfig.getString("suffix");

		if (talismaneConfig.hasPath("in-file"))
			fileName = talismaneConfig.getString("in-file");
		else
			fileName = "";

		String baseName = "Talismane";
		String baseNamePath = null;
		if (talismaneConfig.hasPath("out-file"))
			baseNamePath = talismaneConfig.getString("out-file");
		else if (talismaneConfig.hasPath("in-file"))
			baseNamePath = talismaneConfig.getString("in-file");

		if (baseNamePath != null) {
			baseNamePath = baseNamePath.replace('\\', '/');

			if (baseNamePath.indexOf('.') > 0)
				baseName = baseNamePath.substring(baseNamePath.lastIndexOf('/') + 1, baseNamePath.lastIndexOf('.'));
			else
				baseName = baseNamePath.substring(baseNamePath.lastIndexOf('/') + 1);
		}

		this.baseName = baseName + this.suffix;

		String configPath = "talismane.core.outDir";
		if (config.hasPath(configPath)) {
			String outDirPath = config.getString(configPath);
			outDir = new File(outDirPath);
			outDir.mkdirs();
		} else {
			configPath = "talismane.core.outFile";
			if (config.hasPath(configPath)) {
				String outFilePath = config.getString(configPath);
				File outFile = new File(outFilePath);
				outDir = outFile.getParentFile();
				if (outDir != null) {
					outDir.mkdirs();
				}
			} else {
				outDir = null;
			}
		}

		// TODO: the posTagSet should only get read from config when training
		// when analysing with a model, it should get read from the model!
		PosTagSet posTagSet = null;
		configPath = "talismane.core.pos-tagger.pos-tag-set";
		if (config.hasPath(configPath)) {
			InputStream posTagSetFile = ConfigUtils.getFileFromConfig(config, configPath);
			try (Scanner posTagSetScanner = new Scanner(new BufferedReader(new InputStreamReader(posTagSetFile, "UTF-8")))) {

				posTagSet = new PosTagSet(posTagSetScanner);
			}
		}
		this.posTagSet = posTagSet;

		// TODO: the transition system should only get read from config when
		// training when analysing with a model, it should get read from the
		// model!
		String transitionSystemStr = config.getString("talismane.core.parser.transition-system");
		TransitionSystem transitionSystem = null;
		if (transitionSystemStr.equalsIgnoreCase("ShiftReduce")) {
			transitionSystem = new ShiftReduceTransitionSystem();
		} else if (transitionSystemStr.equalsIgnoreCase("ArcEager")) {
			transitionSystem = new ArcEagerTransitionSystem();
		} else {
			throw new TalismaneException("Unknown transition system: " + transitionSystemStr);
		}
		this.transitionSystem = transitionSystem;

		configPath = "talismane.core.parser.dependency-labels";
		if (config.hasPath(configPath)) {
			InputStream dependencyLabelFile = ConfigUtils.getFileFromConfig(config, configPath);
			try (Scanner depLabelScanner = new Scanner(new BufferedReader(new InputStreamReader(dependencyLabelFile, "UTF-8")))) {
				Set<String> dependencyLabels = new HashSet<String>();
				while (depLabelScanner.hasNextLine()) {
					String dependencyLabel = depLabelScanner.nextLine();
					if (!dependencyLabel.startsWith("#"))
						dependencyLabels.add(dependencyLabel);
				}
				transitionSystem.setDependencyLabels(dependencyLabels);
			}
		}

		configPath = "talismane.core.lexicons";
		List<String> lexiconPaths = config.getStringList(configPath);
		for (String lexiconPath : lexiconPaths) {
			List<PosTaggerLexicon> lexicons = lexiconMap.get(lexiconPath);

			if (lexicons == null) {
				InputStream lexiconFile = ConfigUtils.getFile(config, configPath, lexiconPath);

				LexiconDeserializer lexiconDeserializer = new LexiconDeserializer(this);
				lexicons = lexiconDeserializer.deserializeLexicons(new ZipInputStream(lexiconFile));
				lexiconMap.put(lexiconPath, lexicons);
			}

			for (PosTaggerLexicon oneLexicon : lexicons) {
				this.lexicons.add(oneLexicon);
			}
		}

		if (lexicons.size() == 0)
			mergedLexicon = new EmptyLexicon();
		else if (lexicons.size() == 1)
			mergedLexicon = lexicons.get(0);
		else {
			LexiconChain lexiconChain = new LexiconChain();
			for (PosTaggerLexicon lexicon : lexicons) {
				lexiconChain.addLexicon(lexicon);
			}
			mergedLexicon = lexiconChain;
		}

		configPath = "talismane.core.word-lists";
		List<String> wordListPaths = config.getStringList(configPath);
		if (wordListPaths.size() > 0) {
			for (String path : wordListPaths) {
				LOG.info("Reading word list from " + path);
				List<FileObject> fileObjects = ConfigUtils.getFileObjects(path);
				for (FileObject fileObject : fileObjects) {
					InputStream externalResourceFile = fileObject.getContent().getInputStream();
					try (Scanner scanner = new Scanner(externalResourceFile)) {
						wordListFinder.addWordList(fileObject.getName().getBaseName(), scanner);
					}
				}
			}
		}

		configPath = "talismane.core.external-resources";
		List<String> externalResourcePaths = config.getStringList(configPath);
		if (externalResourcePaths.size() > 0) {
			for (String path : externalResourcePaths) {
				LOG.info("Reading external resources from " + path);
				List<FileObject> fileObjects = ConfigUtils.getFileObjects(path);
				for (FileObject fileObject : fileObjects) {
					InputStream externalResourceFile = fileObject.getContent().getInputStream();
					try (Scanner scanner = new Scanner(externalResourceFile)) {
						externalResourceFinder.addExternalResource(fileObject.getName().getBaseName(), scanner);
					}
				}
			}
		}

		configPath = "talismane.core.lowercase-preferences";

		if (config.hasPath(configPath)) {
			InputStream lowercasePreferencesFile = ConfigUtils.getFileFromConfig(config, configPath);
			try (Scanner scanner = new Scanner(lowercasePreferencesFile, "UTF-8")) {
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine().trim();
					if (line.length() > 0 && !line.startsWith("#")) {
						String[] parts = line.split("\t");
						String uppercase = parts[0];
						String lowercase = parts[1];
						lowercasePreferences.put(uppercase, lowercase);
					}
				}
			}
		}

		Diacriticizer diacriticizer = null;
		configPath = "talismane.core.diacriticizer";
		if (config.hasPath(configPath)) {
			String diacriticizerPath = config.getString(configPath);
			diacriticizer = diacriticizerMap.get(diacriticizerPath);

			if (diacriticizer == null) {
				LOG.info("Loading new diacriticizer from: " + diacriticizerPath);
				InputStream diacriticizerFile = ConfigUtils.getFileFromConfig(config, configPath);
				try (ZipInputStream zis = new ZipInputStream(diacriticizerFile)) {
					zis.getNextEntry();
					ObjectInputStream in = new ObjectInputStream(zis);
					diacriticizer = (Diacriticizer) in.readObject();
					diacriticizerMap.put(diacriticizerPath, diacriticizer);
				}
			} else {
				LOG.info("Fetching diacriticizer from cache for: " + diacriticizerPath);
			}
			diacriticizer.setLowercasePreferences(lowercasePreferences);
		}
		this.diacriticizer = diacriticizer;

	}

	public synchronized PosTagSet getPosTagSet() {
		if (posTagSet == null)
			throw new TalismaneException("PosTagSet missing.");
		return posTagSet;
	}

	public synchronized TransitionSystem getTransitionSystem() {
		if (transitionSystem == null)
			throw new TalismaneException("TransitionSystem missing.");
		return transitionSystem;
	}

	/**
	 * A list of lexicons setup for the current session.
	 */
	public synchronized List<PosTaggerLexicon> getLexicons() {
		return lexicons;
	}

	public Locale getLocale() {
		return locale;
	}

	public synchronized LinguisticRules getLinguisticRules() {
		if (linguisticRules == null) {
			linguisticRules = new GenericRules(this);
		}
		return linguisticRules;
	}

	public void setLinguisticRules(LinguisticRules linguisticRules) {
		this.linguisticRules = linguisticRules;
	}

	/**
	 * Get a lexicon which merges all of the lexicons added, prioritised in the
	 * order in which they were added.
	 */
	public synchronized PosTaggerLexicon getMergedLexicon() {
		return mergedLexicon;
	}

	public Diacriticizer getDiacriticizer() {
		if (diacriticizer == null) {
			diacriticizer = new Diacriticizer(this.getMergedLexicon());
			diacriticizer.setLocale(this.getLocale());
			diacriticizer.setLowercasePreferences(lowercasePreferences);
		}
		return diacriticizer;
	}

	public void setDiacriticizer(Diacriticizer diacriticizer) {
		this.diacriticizer = diacriticizer;
	}

	/**
	 * A string inserted between outputs (such as a newline).
	 */

	public String getOutputDivider() {
		return outputDivider;
	}

	public void setOutputDivider(String outputDivider) {
		this.outputDivider = outputDivider;
	}

	public WordListFinder getWordListFinder() {
		return wordListFinder;
	}

	public ExternalResourceFinder getExternalResourceFinder() {
		return externalResourceFinder;
	}

	public String getSessionId() {
		return sessionId;
	}

	/**
	 * The base name, out of which to construct output file names.
	 */
	public synchronized String getBaseName() {
		return baseName;
	}

	/**
	 * Input charset for all corpus/lexicon input - does not apply to
	 * configuration files, which are assumed to be in UTF-8.
	 * 
	 * @return
	 */
	public Charset getInputCharset() {
		return inputCharset;
	}

	/**
	 * Charset for all Talismane output.
	 * 
	 * @return
	 */
	public Charset getOutputCharset() {
		return outputCharset;
	}

	/**
	 * A suffix to add to output file names, e.g. when comparing results for
	 * different configurations.
	 * 
	 * @return
	 */
	public String getSuffix() {
		return suffix;
	}

	/**
	 * The filename to tag against output tokens - typically the inFile if it
	 * exists, or nothing.
	 * 
	 * @return
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * The directory to which we write any output files.
	 */
	public synchronized File getOutDir() {
		return outDir;
	}
}
