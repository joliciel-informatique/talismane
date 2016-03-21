///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Joliciel Informatique
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.GenericRules;
import com.joliciel.talismane.LanguagePackImplementation;
import com.joliciel.talismane.LinguisticRules;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.Diacriticizer;
import com.joliciel.talismane.lexicon.EmptyLexicon;
import com.joliciel.talismane.lexicon.LexicalEntryReader;
import com.joliciel.talismane.lexicon.LexiconDeserializer;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.lexicon.RegexLexicalEntryReader;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.tokeniser.filters.LowercaseKnownFirstWordFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.tokeniser.filters.UppercaseSeriesFilter;
import com.joliciel.talismane.utils.ArrayListNoNulls;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.StringUtils;
import com.joliciel.talismane.utils.io.UnclosableInputStream;

public class GenericLanguageImplementation implements LanguagePackImplementation {
	private static final Log LOG = LogFactory.getLog(GenericLanguageImplementation.class);
	private TalismaneServiceLocator talismaneServiceLocator = null;
	private PosTaggerService posTaggerService;
	private ParserService parserService;
	private MachineLearningService machineLearningService;
	private TokenFilterService tokenFilterService;

	private List<Class<? extends TokenSequenceFilter>> availableTokenSequenceFilters;
	private TalismaneSession talismaneSession;

	private ClassificationModel sentenceModel;
	private ClassificationModel tokeniserModel;
	private ClassificationModel posTaggerModel;
	private MachineLearningModel parserModel;
	private List<PosTaggerLexicon> lexicons;
	private PosTagSet posTagSet;
	private TransitionSystem transitionSystem;
	private boolean replaceTextFilters = false;
	private boolean replaceTokenFilters = false;
	private boolean replaceTokenSequenceFilters = false;
	private String textFiltersStr;
	private String tokenFiltersStr;
	private String tokenSequenceFiltersStr;
	private String posTaggerRulesStr;
	private String parserRulesStr;
	private LexicalEntryReader corpusLexicalEntryReader = null;
	private Diacriticizer diacriticizer;
	private Map<String, String> lowercasePreferences = new HashMap<String, String>();

	private Locale locale;

	private static Map<String, GenericLanguageImplementation> instances = new HashMap<String, GenericLanguageImplementation>();

	private static Map<String, LanguageResources> resourceCache = new HashMap<String, LanguageResources>();

	private static final class LanguageResources {
		ClassificationModel sentenceModel;
		ClassificationModel tokeniserModel;
		ClassificationModel posTaggerModel;
		MachineLearningModel parserModel;
		List<PosTaggerLexicon> lexicons;
	}

	public static GenericLanguageImplementation getInstance(String sessionId) {
		GenericLanguageImplementation instance = instances.get(sessionId);
		if (instance == null) {
			instance = new GenericLanguageImplementation(sessionId);
			instances.put(sessionId, instance);
		}
		return instance;
	}

	protected GenericLanguageImplementation(String sessionId) {
		talismaneServiceLocator = TalismaneServiceLocator.getInstance(sessionId);
		talismaneSession = talismaneServiceLocator.getTalismaneService().getTalismaneSession();
	}

	@SuppressWarnings("unused")
	private static ZipInputStream getZipInputStreamFromResource(String resource) {
		InputStream inputStream = getInputStreamFromResource(resource);
		ZipInputStream zis = new ZipInputStream(inputStream);

		return zis;
	}

	private static InputStream getInputStreamFromResource(String resource) {
		String path = "resources/" + resource;
		LOG.debug("Getting " + path);
		InputStream inputStream = GenericLanguageImplementation.class.getResourceAsStream(path);

		return inputStream;
	}

	@Override
	public Scanner getDefaultPosTaggerRulesScanner() {
		Scanner scanner = null;
		if (posTaggerRulesStr != null)
			scanner = new Scanner(posTaggerRulesStr);
		return scanner;
	}

	@Override
	public Scanner getDefaultParserRulesScanner() {
		Scanner scanner = null;
		if (parserRulesStr != null)
			scanner = new Scanner(parserRulesStr);
		return scanner;
	}

	@Override
	public List<PosTaggerLexicon> getDefaultLexicons() {
		if (lexicons == null) {
			lexicons = new ArrayListNoNulls<PosTaggerLexicon>();
			lexicons.add(new EmptyLexicon());
		}
		return lexicons;
	}

	@Override
	public ClassificationModel getDefaultSentenceModel() {
		return sentenceModel;
	}

	@Override
	public ClassificationModel getDefaultTokeniserModel() {
		return tokeniserModel;
	}

	@Override
	public ClassificationModel getDefaultPosTaggerModel() {
		return posTaggerModel;
	}

	@Override
	public MachineLearningModel getDefaultParserModel() {
		return parserModel;
	}

	@Override
	public Scanner getDefaultTokenSequenceFiltersScanner() {
		String tokenSequenceFilterString = "";
		if (tokenSequenceFiltersStr != null) {
			tokenSequenceFilterString = tokenSequenceFiltersStr;
		}
		if (!replaceTokenSequenceFilters) {
			tokenSequenceFilterString += "\n";
			tokenSequenceFilterString += LowercaseKnownFirstWordFilter.class.getSimpleName() + "\n";
			tokenSequenceFilterString += UppercaseSeriesFilter.class.getSimpleName() + "\n";
		}

		Scanner scanner = new Scanner(tokenSequenceFilterString);
		return scanner;
	}

	@Override
	public Scanner getDefaultTextMarkerFiltersScanner() {
		try {
			String textFilterString = "";
			if (textFiltersStr != null) {
				textFilterString += textFiltersStr;
			}
			if (!replaceTextFilters) {
				InputStream inputStream = getInputStreamFromResource("text_marker_filters.txt");
				String defaultTextMarkerFilters = StringUtils.readerToString(new InputStreamReader(inputStream, "UTF-8"));
				textFilterString += defaultTextMarkerFilters;
			}
			return new Scanner(textFilterString);
		} catch (UnsupportedEncodingException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Scanner getDefaultTokenFiltersScanner() {
		try {
			String tokenFilterString = "";
			if (tokenFiltersStr != null) {
				tokenFilterString += tokenFiltersStr;
			}
			if (!replaceTokenFilters) {
				InputStream inputStream = getInputStreamFromResource("token_filters.txt");
				String defaultTextMarkerFilters = StringUtils.readerToString(new InputStreamReader(inputStream, "UTF-8"));
				tokenFilterString += defaultTextMarkerFilters;
			}
			return new Scanner(tokenFilterString);
		} catch (UnsupportedEncodingException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public TransitionSystem getDefaultTransitionSystem() {
		return transitionSystem;
	}

	@Override
	public List<PosTagSequenceFilter> getDefaultPosTagSequenceFilters() {
		List<PosTagSequenceFilter> filters = new ArrayListNoNulls<PosTagSequenceFilter>();
		return filters;
	}

	@Override
	public PosTagSet getDefaultPosTagSet() {
		return posTagSet;
	}

	public PosTaggerService getPosTaggerService() {
		if (posTaggerService == null) {
			posTaggerService = talismaneServiceLocator.getPosTaggerServiceLocator().getPosTaggerService();
		}
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public ParserService getParserService() {
		if (parserService == null) {
			parserService = talismaneServiceLocator.getParserServiceLocator().getParserService();
		}
		return parserService;
	}

	public void setParserService(ParserService parserService) {
		this.parserService = parserService;
	}

	public TokenFilterService getTokenFilterService() {
		if (tokenFilterService == null)
			tokenFilterService = talismaneServiceLocator.getTokenFilterServiceLocator().getTokenFilterService();
		return tokenFilterService;
	}

	public void setTokenFilterService(TokenFilterService tokenFilterService) {
		this.tokenFilterService = tokenFilterService;
	}

	@Override
	public List<Class<? extends TokenSequenceFilter>> getAvailableTokenSequenceFilters() {
		if (availableTokenSequenceFilters == null) {
			availableTokenSequenceFilters = new ArrayListNoNulls<Class<? extends TokenSequenceFilter>>();
		}
		return availableTokenSequenceFilters;
	}

	@Override
	public LinguisticRules getDefaultLinguisticRules() {
		return new GenericRules(this.getTalismaneSession());
	}

	public TalismaneSession getTalismaneSession() {
		if (talismaneSession == null) {
			talismaneSession = talismaneServiceLocator.getTalismaneService().getTalismaneSession();
		}
		return talismaneSession;
	}

	public MachineLearningService getMachineLearningService() {
		if (machineLearningService == null) {
			machineLearningService = talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService();
		}
		return machineLearningService;
	}

	public void setMachineLearningService(MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	@Override
	public void setLanguagePack(File languagePackFile) {
		ZipInputStream zis = null;
		try {
			InputStream languagePackInputStream = new BufferedInputStream(new FileInputStream(languagePackFile));
			InputStream languagePackInputStream2 = new BufferedInputStream(new FileInputStream(languagePackFile));
			zis = new ZipInputStream(languagePackInputStream);
			ZipEntry ze = null;

			Map<String, String> argMap = new HashMap<String, String>();
			while ((ze = zis.getNextEntry()) != null) {
				String name = ze.getName();
				if (name.indexOf('/') >= 0)
					name = name.substring(name.lastIndexOf('/') + 1);
				if (name.equals("languagePack.properties")) {
					Properties props = new Properties();
					props.load(zis);
					argMap = StringUtils.getArgMap(props);
					break;
				}
			} // next zip entry
			zis.close();

			Map<String, String> reverseMap = new HashMap<String, String>();
			for (String key : argMap.keySet()) {
				String value = argMap.get(key);
				if (value.startsWith("replace:")) {
					value = value.substring("replace:".length());
					if (key.equals("textFilters")) {
						replaceTextFilters = true;
					} else if (key.equals("tokenFilters")) {
						replaceTokenFilters = true;
					} else if (key.equals("tokenSequenceFilters")) {
						replaceTokenSequenceFilters = true;
					}
				}
				if (key.equals("locale")) {
					locale = Locale.forLanguageTag(value);
				} else {
					reverseMap.put(value, key);
				}
			}

			zis = new ZipInputStream(languagePackInputStream2);
			ze = null;

			String path = languagePackFile.getCanonicalPath();
			LanguageResources languageResources = resourceCache.get(path);
			if (languageResources == null) {
				LOG.info("Creating new language resource cache for " + path);
				languageResources = new LanguageResources();
				resourceCache.put(path, languageResources);
			} else {
				LOG.info("Using existing language resource cache for " + path);
			}

			while ((ze = zis.getNextEntry()) != null) {
				String name = ze.getName();
				if (name.indexOf('/') >= 0)
					name = name.substring(name.lastIndexOf('/') + 1);
				String key = reverseMap.get(name);
				if (key != null) {
					if (key.equals("transitionSystem")) {
						transitionSystem = this.getParserService().getArcEagerTransitionSystem();
						@SuppressWarnings("resource")
						Scanner scanner = new Scanner(zis, "UTF-8");
						Set<String> dependencyLabels = new HashSet<String>();
						while (scanner.hasNextLine()) {
							String dependencyLabel = scanner.nextLine();
							if (!dependencyLabel.startsWith("#")) {
								if (dependencyLabel.indexOf('\t') > 0)
									dependencyLabel = dependencyLabel.substring(0, dependencyLabel.indexOf('\t'));
								dependencyLabels.add(dependencyLabel);
							}
						}
						transitionSystem.setDependencyLabels(dependencyLabels);
					} else if (key.equals("posTagSet")) {
						Scanner scanner = new Scanner(zis, "UTF-8");
						posTagSet = this.getPosTaggerService().getPosTagSet(scanner);
					} else if (key.equals("textFilters")) {
						Scanner scanner = new Scanner(zis, "UTF-8");
						textFiltersStr = this.getStringFromScanner(scanner);
					} else if (key.equals("tokenFilters")) {
						Scanner scanner = new Scanner(zis, "UTF-8");
						tokenFiltersStr = this.getStringFromScanner(scanner);
					} else if (key.equals("tokenSequenceFilters")) {
						Scanner scanner = new Scanner(zis, "UTF-8");
						tokenSequenceFiltersStr = this.getStringFromScanner(scanner);
					} else if (key.equals("posTaggerRules")) {
						Scanner scanner = new Scanner(zis, "UTF-8");
						posTaggerRulesStr = this.getStringFromScanner(scanner);
					} else if (key.equals("parserRules")) {
						Scanner scanner = new Scanner(zis, "UTF-8");
						parserRulesStr = this.getStringFromScanner(scanner);
					} else if (key.equals("sentenceModel")) {
						if (languageResources.sentenceModel == null) {
							ZipInputStream innerZis = new ZipInputStream(new UnclosableInputStream(zis));
							sentenceModel = this.getMachineLearningService().getClassificationModel(innerZis);
							languageResources.sentenceModel = sentenceModel;
						} else {
							sentenceModel = languageResources.sentenceModel;
						}
					} else if (key.equals("tokeniserModel")) {
						if (languageResources.tokeniserModel == null) {
							ZipInputStream innerZis = new ZipInputStream(new UnclosableInputStream(zis));
							tokeniserModel = this.getMachineLearningService().getClassificationModel(innerZis);
							languageResources.tokeniserModel = tokeniserModel;
						} else {
							tokeniserModel = languageResources.tokeniserModel;
						}
					} else if (key.equals("posTaggerModel")) {
						if (languageResources.posTaggerModel == null) {
							ZipInputStream innerZis = new ZipInputStream(new UnclosableInputStream(zis));
							posTaggerModel = this.getMachineLearningService().getClassificationModel(innerZis);
							languageResources.posTaggerModel = posTaggerModel;
						} else {
							posTaggerModel = languageResources.posTaggerModel;
						}
					} else if (key.equals("parserModel")) {
						if (languageResources.parserModel == null) {
							ZipInputStream innerZis = new ZipInputStream(new UnclosableInputStream(zis));
							parserModel = this.getMachineLearningService().getClassificationModel(innerZis);
							languageResources.parserModel = parserModel;
						} else {
							parserModel = languageResources.parserModel;
						}
					} else if (key.equals("lexicon")) {
						if (languageResources.lexicons == null) {
							ZipInputStream innerZis = new ZipInputStream(new UnclosableInputStream(zis));
							LexiconDeserializer deserializer = new LexiconDeserializer(this.getTalismaneSession());
							lexicons = deserializer.deserializeLexicons(innerZis);
							languageResources.lexicons = lexicons;
						} else {
							lexicons = languageResources.lexicons;
						}
					} else if (key.equals("diacriticizer")) {
						ZipInputStream innerZis = new ZipInputStream(new UnclosableInputStream(zis));
						ObjectInputStream in = new ObjectInputStream(innerZis);
						diacriticizer = (Diacriticizer) in.readObject();
						in.close();
						diacriticizer.setTalismaneSession(talismaneSession);
					} else if (key.equals("lowercasePreferences")) {
						@SuppressWarnings("resource")
						Scanner scanner = new Scanner(zis, "UTF-8");
						while (scanner.hasNextLine()) {
							String line = scanner.nextLine().trim();
							if (line.length() > 0 && !line.startsWith("#")) {
								String[] parts = line.split("\t");
								String uppercase = parts[0];
								String lowercase = parts[1];
								lowercasePreferences.put(uppercase, lowercase);
							}
						}
					} else if (key.equals("corpusLexiconEntryRegex")) {
						Scanner corpusLexicalEntryRegexScanner = new Scanner(zis, "UTF-8");
						corpusLexicalEntryReader = new RegexLexicalEntryReader(corpusLexicalEntryRegexScanner);
					} else {
						throw new TalismaneException("Unknown key in languagePack.properties: " + key);
					}
				}
			}
		} catch (ClassNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} finally {
			if (zis != null) {
				try {
					zis.close();
				} catch (IOException e) {
					LogUtils.logError(LOG, e);
					throw new RuntimeException(e);
				}
			}
		}
	}

	private String getStringFromScanner(Scanner scanner) {
		StringBuilder sb = new StringBuilder();
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			sb.append(line + "\n");
		}
		return sb.toString();
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public LexicalEntryReader getDefaultCorpusLexicalEntryReader() {
		return corpusLexicalEntryReader;
	}

	@Override
	public Diacriticizer getDiacriticizer() {
		return diacriticizer;
	}

	@Override
	public Map<String, String> getLowercasePreferences() {
		return lowercasePreferences;
	}

}
