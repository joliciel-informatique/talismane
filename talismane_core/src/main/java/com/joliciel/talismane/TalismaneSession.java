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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipInputStream;

import org.apache.commons.vfs2.FileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Talismane.Command;
import com.joliciel.talismane.Talismane.Module;
import com.joliciel.talismane.lexicon.Diacriticizer;
import com.joliciel.talismane.lexicon.EmptyLexicon;
import com.joliciel.talismane.lexicon.LexiconChain;
import com.joliciel.talismane.lexicon.LexiconDeserializer;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.output.CoNLLFormatter;
import com.joliciel.talismane.parser.ArcEagerTransitionSystem;
import com.joliciel.talismane.parser.DependencyLabelSet;
import com.joliciel.talismane.parser.ShiftReduceTransitionSystem;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.rawText.DuplicateWhiteSpaceFilter;
import com.joliciel.talismane.rawText.NewlineEndOfSentenceMarker;
import com.joliciel.talismane.rawText.NewlineSpaceMarker;
import com.joliciel.talismane.rawText.OtherWhiteSpaceFilter;
import com.joliciel.talismane.rawText.RawTextAnnotator;
import com.joliciel.talismane.rawText.RawTextFilterFactory;
import com.joliciel.talismane.rawText.RawTextMarkType;
import com.joliciel.talismane.rawText.RawTextRegexAnnotator;
import com.joliciel.talismane.resources.WordListFinder;
import com.joliciel.talismane.sentenceAnnotators.SentenceAnnotator;
import com.joliciel.talismane.sentenceAnnotators.SentenceAnnotatorLoader;
import com.joliciel.talismane.utils.CSVFormatter;
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

	private final Config config;
	private final String sessionId;
	private final Locale locale;
	private final Command command;
	private final Module module;
	private final int port;
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
	private String baseName = null;
	private final String suffix;
	private final Map<String, String> lowercasePreferences = new HashMap<>();
	private final char endBlockCharCode;
	private final CoNLLFormatter coNLLFormatter;
	private final Charset csvCharset;
	private final int blockSize;
	private final RawTextMarkType newlineMarker;
	private final List<RawTextAnnotator> textAnnotators;
	private final List<SentenceAnnotator> sentenceAnnotators;
	private final List<List<String>> sentenceAnnotatorDescriptors;

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
		this.config = config;

		config.checkValid(ConfigFactory.defaultReference(), "talismane.core");

		Config talismaneConfig = config.getConfig("talismane.core");
		this.command = Command.valueOf(talismaneConfig.getString("command"));

		if (talismaneConfig.hasPath("module")) {
			this.module = Module.valueOf(talismaneConfig.getString("module"));
		} else {
			this.module = null;
		}

		this.port = talismaneConfig.getInt("port");

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

		this.baseName = talismaneConfig.getString("base-name") + this.suffix;

		PosTagSet posTagSet = null;
		String configPath = "talismane.core.pos-tagger.pos-tag-set";
		if (config.hasPath(configPath)) {
			InputStream posTagSetFile = ConfigUtils.getFileFromConfig(config, configPath);
			try (Scanner posTagSetScanner = new Scanner(new BufferedReader(new InputStreamReader(posTagSetFile, "UTF-8")))) {

				posTagSet = new PosTagSet(posTagSetScanner);
			}
		}
		this.posTagSet = posTagSet;

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
				DependencyLabelSet dependencyLabelSet = new DependencyLabelSet(depLabelScanner);
				transitionSystem.setDependencyLabelSet(dependencyLabelSet);
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

		String endBlockCharacter = talismaneConfig.getString("end-block-char-code");
		if (endBlockCharacter.length() > 1) {
			throw new IllegalArgumentException("end block character must be a single character");
		}
		this.endBlockCharCode = endBlockCharacter.charAt(0);

		boolean spacesToUnderscores = talismaneConfig.getBoolean("conll.spaces-to-underscores");
		this.coNLLFormatter = new CoNLLFormatter(spacesToUnderscores);

		String csvSeparator = talismaneConfig.getString("csv.separator");
		if (talismaneConfig.hasPath("csv.encoding"))
			csvCharset = Charset.forName(talismaneConfig.getString("csv.encoding"));
		else
			csvCharset = Charset.defaultCharset();

		Locale outputLocale = null;
		if (talismaneConfig.hasPath("csv.locale")) {
			String csvLocaleString = talismaneConfig.getString("csv.locale");
			outputLocale = Locale.forLanguageTag(csvLocaleString);
		}

		if (csvSeparator.length() > 0)
			CSVFormatter.setGlobalCsvSeparator(csvSeparator);

		if (outputLocale != null)
			CSVFormatter.setGlobalLocale(outputLocale);

		// ##################################################################
		// text annotators
		LOG.debug("text-annotators");
		this.blockSize = talismaneConfig.getInt("block-size");
		this.textAnnotators = new ArrayList<>();
		// insert sentence breaks at end of block
		this.textAnnotators.add(new RawTextRegexAnnotator(Arrays.asList(new RawTextMarkType[] { RawTextMarkType.SKIP, RawTextMarkType.SENTENCE_BREAK }),
				"" + this.endBlockCharCode, 0, blockSize));

		// handle newline as requested
		newlineMarker = RawTextMarkType.valueOf(talismaneConfig.getString("newline"));
		if (newlineMarker.equals(RawTextMarkType.SENTENCE_BREAK))
			this.textAnnotators.add(new NewlineEndOfSentenceMarker(blockSize));
		else if (newlineMarker.equals(RawTextMarkType.SPACE))
			this.textAnnotators.add(new NewlineSpaceMarker(blockSize));

		// get rid of duplicate white-space always
		this.textAnnotators.add(new DuplicateWhiteSpaceFilter(blockSize));

		// replace tabs with white space
		this.textAnnotators.add(new OtherWhiteSpaceFilter(blockSize));

		RawTextFilterFactory factory = new RawTextFilterFactory();

		configPath = "talismane.core.annotators.text-annotators";
		List<String> textAnnotatorPaths = config.getStringList(configPath);
		for (String path : textAnnotatorPaths) {
			LOG.debug("From: " + path);
			InputStream inputStream = ConfigUtils.getFile(config, configPath, path);
			try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					LOG.debug(descriptor);
					if (descriptor.length() > 0 && !descriptor.startsWith("#")) {
						RawTextAnnotator textMarkerFilter = factory.getTextMarkerFilter(descriptor, blockSize);
						this.textAnnotators.add(textMarkerFilter);
					}
				}
			}
		}

		// ##################################################################
		// sentence annotators
		LOG.debug("sentence-annotators");
		SentenceAnnotatorLoader tokenFilterFactory = SentenceAnnotatorLoader.getInstance(this);
		this.sentenceAnnotators = new ArrayList<>();
		this.sentenceAnnotatorDescriptors = new ArrayList<>();
		configPath = "talismane.core.annotators.sentence-annotators";
		List<String> sentenceAnnotatorPaths = config.getStringList(configPath);
		for (String path : sentenceAnnotatorPaths) {
			LOG.debug("From: " + path);
			InputStream inputStream = ConfigUtils.getFile(config, configPath, path);
			try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
				List<SentenceAnnotator> myAnnotators = tokenFilterFactory.loadSentenceAnnotators(scanner, path);
				for (SentenceAnnotator annotator : myAnnotators) {
					this.sentenceAnnotators.add(annotator);
				}
			}
			inputStream = ConfigUtils.getFile(config, configPath, path);
			List<String> descriptors = new ArrayList<>();
			sentenceAnnotatorDescriptors.add(descriptors);
			try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					descriptors.add(line);
				}
			}
		}
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
	 * A string inserted between any two segments of raw text that have been
	 * marked for output in the Talismane analysis. This could, for example, be
	 * a newline.
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
	 * A formatter for CoNLL output and input.
	 */
	public CoNLLFormatter getCoNLLFormatter() {
		return coNLLFormatter;
	}

	/**
	 * The charset for CSV file output.
	 */
	public Charset getCsvCharset() {
		return csvCharset;
	}

	/**
	 * Which command to run.
	 */
	public Command getCommand() {
		return command;
	}

	/**
	 * Which port Talismane should listen on in server mode.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * The configuration used to construct this session.
	 */
	public Config getConfig() {
		return config;
	}

	/**
	 * The module for which to apply the command (for single-module commands,
	 * such as train).
	 */
	public Module getModule() {
		return module;
	}

	/**
	 * A character (typically non-printing) which will mark a stop in the input
	 * stream and set-off analysis, such as the form-feed character.
	 */
	public char getEndBlockCharacter() {
		return endBlockCharCode;
	}

	/**
	 * The minimum block size, in characters, to process by the sentence
	 * detector. Filters are applied to a concatenation of the previous block,
	 * the current block, and the next block prior to sentence detection, in
	 * order to ensure that a filter which crosses block boundaries is correctly
	 * applied. It is not legal to have a filter which matches text greater than
	 * a block size, since this could result in a filter which stops analysis
	 * but doesn't start it again correctly, or vice versa. Block size can be
	 * increased if really big filters are really required. Default is 1000.
	 */
	public int getBlockSize() {
		return blockSize;
	}

	public List<RawTextAnnotator> getTextAnnotators() {
		return textAnnotators;
	}

	public List<SentenceAnnotator> getSentenceAnnotators() {
		return sentenceAnnotators;
	}

	public List<List<String>> getSentenceAnnotatorDescriptors() {
		return sentenceAnnotatorDescriptors;
	}

	public void setFileForBasename(File file) {
		if (file != null) {
			baseName = file.getName();
			if (baseName.indexOf('.') > 0)
				baseName = baseName.substring(0, baseName.lastIndexOf('.'));
			baseName += suffix;
		}
	}
}
