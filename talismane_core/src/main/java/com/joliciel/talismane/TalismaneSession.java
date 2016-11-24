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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.vfs2.FileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Talismane.Command;
import com.joliciel.talismane.Talismane.Module;
import com.joliciel.talismane.filters.DuplicateWhiteSpaceFilter;
import com.joliciel.talismane.filters.MarkerFilterType;
import com.joliciel.talismane.filters.NewlineEndOfSentenceMarker;
import com.joliciel.talismane.filters.NewlineSpaceMarker;
import com.joliciel.talismane.filters.OtherWhiteSpaceFilter;
import com.joliciel.talismane.filters.RegexMarkerFilter;
import com.joliciel.talismane.filters.TextMarkerFilter;
import com.joliciel.talismane.filters.TextMarkerFilterFactory;
import com.joliciel.talismane.lexicon.Diacriticizer;
import com.joliciel.talismane.lexicon.EmptyLexicon;
import com.joliciel.talismane.lexicon.LexiconChain;
import com.joliciel.talismane.lexicon.LexiconDeserializer;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.output.CoNLLFormatter;
import com.joliciel.talismane.parser.ArcEagerTransitionSystem;
import com.joliciel.talismane.parser.ShiftReduceTransitionSystem;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilterFactory;
import com.joliciel.talismane.resources.WordListFinder;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterFactory;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilterFactory;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.ConfigUtils;
import com.joliciel.talismane.utils.io.CurrentFileProvider;
import com.joliciel.talismane.utils.io.DirectoryReader;
import com.joliciel.talismane.utils.io.DirectoryWriter;
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
	private final String baseName;
	private final String suffix;
	private final String fileName;
	private final File outDir;
	private final Map<String, String> lowercasePreferences = new HashMap<>();
	private final char endBlockCharCode;
	private final CoNLLFormatter coNLLFormatter;
	private final Charset csvCharset;
	private final int blockSize;
	private final MarkerFilterType newlineMarker;
	private final List<TextMarkerFilter> textFilters;
	private final List<Annotator> textAnnotators;
	private final List<Pair<String, Annotator>> textAnnotatorsWithDescriptors;
	private final List<TokenSequenceFilter> tokenSequenceFilters;
	private final List<Pair<String, TokenSequenceFilter>> tokenSequenceFiltersWithDescriptors;
	private final List<PosTagSequenceFilter> posTagSequenceFilters;
	private final List<Pair<String, PosTagSequenceFilter>> posTagSequenceFiltersWithDescriptors;

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

		if (talismaneConfig.hasPath("in-file")) {
			fileName = talismaneConfig.getString("in-file");
		} else {
			fileName = "";
		}

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

		String configPath = "talismane.core.out-dir";
		if (config.hasPath(configPath)) {
			String outDirPath = config.getString(configPath);
			outDir = new File(outDirPath);
			outDir.mkdirs();
		} else {
			configPath = "talismane.core.out-file";
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
		// text filters
		LOG.debug("text-filters");
		this.blockSize = talismaneConfig.getInt("block-size");
		this.textFilters = new ArrayList<>();
		// insert sentence breaks at end of block
		this.textFilters.add(new RegexMarkerFilter(Arrays.asList(new MarkerFilterType[] { MarkerFilterType.SKIP, MarkerFilterType.SENTENCE_BREAK }),
				"" + this.endBlockCharCode, 0, blockSize));

		// handle newline as requested
		newlineMarker = MarkerFilterType.valueOf(talismaneConfig.getString("newline"));
		if (newlineMarker.equals(MarkerFilterType.SENTENCE_BREAK))
			this.textFilters.add(new NewlineEndOfSentenceMarker(blockSize));
		else if (newlineMarker.equals(MarkerFilterType.SPACE))
			this.textFilters.add(new NewlineSpaceMarker(blockSize));

		// get rid of duplicate white-space always
		this.textFilters.add(new DuplicateWhiteSpaceFilter(blockSize));

		// replace tabs with white space
		this.textFilters.add(new OtherWhiteSpaceFilter(blockSize));

		TextMarkerFilterFactory factory = new TextMarkerFilterFactory();

		configPath = "talismane.core.annotators.text-filters";
		List<String> textFilterPaths = config.getStringList(configPath);
		for (String path : textFilterPaths) {
			LOG.debug("From: " + path);
			InputStream textFilterFile = ConfigUtils.getFile(config, configPath, path);
			try (Scanner scanner = new Scanner(textFilterFile, "UTF-8")) {
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					LOG.debug(descriptor);
					if (descriptor.length() > 0 && !descriptor.startsWith("#")) {
						TextMarkerFilter textMarkerFilter = factory.getTextMarkerFilter(descriptor, blockSize);
						this.textFilters.add(textMarkerFilter);
					}
				}
			}
		}

		// ##################################################################
		// text annotators
		LOG.debug("text-annotators");
		TokenFilterFactory tokenFilterFactory = TokenFilterFactory.getInstance(this);
		this.textAnnotators = new ArrayList<>();
		this.textAnnotatorsWithDescriptors = new ArrayList<>();
		configPath = "talismane.core.annotators.text-annotators";
		List<String> tokenFilterPaths = config.getStringList(configPath);
		for (String path : tokenFilterPaths) {
			LOG.debug("From: " + path);
			InputStream inputStream = ConfigUtils.getFile(config, configPath, path);
			try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
				List<Pair<TokenFilter, String>> myFilters = tokenFilterFactory.readTokenFilters(scanner, path);
				for (Pair<TokenFilter, String> tokenFilterPair : myFilters) {
					this.textAnnotators.add(tokenFilterPair.getLeft());
					this.textAnnotatorsWithDescriptors.add(new ImmutablePair<>(tokenFilterPair.getRight(), tokenFilterPair.getLeft()));
				}
			}
		}

		// ##################################################################
		// token sequence filters
		TokenSequenceFilterFactory tokenSequenceFilterFactory = TokenSequenceFilterFactory.getInstance(this);
		this.tokenSequenceFilters = new ArrayList<>();
		this.tokenSequenceFiltersWithDescriptors = new ArrayList<>();

		LOG.debug("token-sequence-filters");
		configPath = "talismane.core.annotators.token-sequence-filters";
		List<String> tokenSequenceFilterPaths = config.getStringList(configPath);
		for (String path : tokenSequenceFilterPaths) {
			LOG.debug("From: " + path);
			InputStream inputStream = ConfigUtils.getFile(config, configPath, path);
			try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					LOG.debug(descriptor);
					if (descriptor.length() > 0 && !descriptor.startsWith("#")) {
						TokenSequenceFilter tokenSequenceFilter = tokenSequenceFilterFactory.getTokenSequenceFilter(descriptor);
						if (tokenSequenceFilter instanceof NeedsTalismaneSession)
							((NeedsTalismaneSession) tokenSequenceFilter).setTalismaneSession(this);
						this.tokenSequenceFilters.add(tokenSequenceFilter);
						this.tokenSequenceFiltersWithDescriptors.add(new ImmutablePair<>(descriptor, tokenSequenceFilter));
					}
				}
			}
		}

		// ##################################################################
		// pos-tag sequence filters
		LOG.debug("postag-sequence-filters");
		configPath = "talismane.core.annotators.postag-sequence-filters";
		PosTagSequenceFilterFactory posTagSequenceFilterFactory = new PosTagSequenceFilterFactory();
		this.posTagSequenceFilters = new ArrayList<>();
		this.posTagSequenceFiltersWithDescriptors = new ArrayList<>();

		List<String> posTagSequenceFilterPaths = config.getStringList(configPath);
		for (String path : posTagSequenceFilterPaths) {
			LOG.debug("From: " + path);
			InputStream inputStream = ConfigUtils.getFile(config, configPath, path);
			try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					LOG.debug(descriptor);
					if (descriptor.length() > 0 && !descriptor.startsWith("#")) {
						PosTagSequenceFilter filter = posTagSequenceFilterFactory.getPosTagSequenceFilter(descriptor);
						this.posTagSequenceFilters.add(filter);
						this.posTagSequenceFiltersWithDescriptors.add(new ImmutablePair<>(descriptor, filter));
					}
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

	/**
	 * The reader to be used to read the data for analysis.
	 * 
	 * @throws IOException
	 */
	public Reader getReader() throws IOException {
		return this.getReader(true);
	}

	/**
	 * The reader to be used to read the data for training.
	 * 
	 * @throws IOException
	 */
	public Reader getTrainingReader() throws IOException {
		return this.getReader(false);
	}

	private Reader getReader(boolean forAnalysis) throws IOException {
		Reader reader = null;
		String configPath = "talismane.core.in-file";
		if (config.hasPath(configPath)) {
			InputStream inFile = ConfigUtils.getFileFromConfig(config, configPath);
			reader = new BufferedReader(new InputStreamReader(inFile, this.getInputCharset()));
		} else {
			configPath = "talismane.core.in-dir";
			if (config.hasPath(configPath)) {
				String inDirPath = config.getString(configPath);
				File inDir = new File(inDirPath);
				if (!inDir.exists())
					throw new FileNotFoundException("inDir does not exist: " + inDirPath);
				if (!inDir.isDirectory())
					throw new FileNotFoundException("inDir must be a directory, not a file - use inFile instead: " + inDirPath);

				@SuppressWarnings("resource")
				DirectoryReader directoryReader = new DirectoryReader(inDir, this.getInputCharset());
				if (forAnalysis) {

					directoryReader.setEndOfFileString("\n" + this.endBlockCharCode);
				} else {
					directoryReader.setEndOfFileString("\n");
				}
				reader = directoryReader;
			} else {
				reader = new BufferedReader(new InputStreamReader(System.in, this.getInputCharset()));
			}
		}
		return reader;
	}

	public Writer getWriter() throws IOException {
		Writer writer = null;
		String configPath = "talismane.core.out-file";
		if (config.hasPath(configPath)) {
			String outFilePath = config.getString(configPath);
			File outFile = new File(outFilePath);
			File outDir = outFile.getParentFile();
			if (outDir != null)
				outDir.mkdirs();
			outFile.delete();
			outFile.createNewFile();

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), this.getOutputCharset()));
		} else {
			String configPathOutDir = "talismane.core.out-dir";
			String configPathInDir = "talismane.core.in-dir";
			if (config.hasPath(configPathOutDir) && config.hasPath(configPathInDir) && (this.getReader() instanceof CurrentFileProvider)
					&& command != Command.evaluate) {
				String outDirPath = config.getString(configPathOutDir);
				String inDirPath = config.getString(configPathInDir);

				File outDir = new File(outDirPath);
				outDir.mkdirs();
				File inDir = new File(inDirPath);

				@SuppressWarnings("resource")
				DirectoryWriter directoryWriter = new DirectoryWriter(inDir, outDir, this.getSuffix(), this.getOutputCharset());
				writer = directoryWriter;
			} else {
				writer = new BufferedWriter(new OutputStreamWriter(System.out, this.getOutputCharset()));
			}
		}
		return writer;
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

	public List<TextMarkerFilter> getTextFilters() {
		return textFilters;
	}

	public List<Annotator> getTextAnnotators() {
		return textAnnotators;
	}

	public List<Pair<String, Annotator>> getTextAnnotatorsWithDescriptors() {
		return textAnnotatorsWithDescriptors;
	}

	public List<TokenSequenceFilter> getTokenSequenceFilters() {
		return tokenSequenceFilters;
	}

	public List<Pair<String, TokenSequenceFilter>> getTokenSequenceFiltersWithDescriptors() {
		return tokenSequenceFiltersWithDescriptors;
	}

	public List<PosTagSequenceFilter> getPosTagSequenceFilters() {
		return posTagSequenceFilters;
	}

	public List<Pair<String, PosTagSequenceFilter>> getPosTagSequenceFiltersWithDescriptors() {
		return posTagSequenceFiltersWithDescriptors;
	}

}
