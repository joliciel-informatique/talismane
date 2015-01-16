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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.lexicon.EmptyLexicon;
import com.joliciel.talismane.lexicon.LexiconDeserializer;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorOutcome;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.filters.LowercaseKnownFirstWordFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.tokeniser.filters.UppercaseSeriesFilter;
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
	
	private static ClassificationModel<SentenceDetectorOutcome> sentenceModel;
	private static ClassificationModel<TokeniserOutcome> tokeniserModel;
	private static ClassificationModel<PosTag> posTaggerModel;
	private static MachineLearningModel parserModel;
	private static List<PosTaggerLexicon> lexicons;
	private static PosTagSet posTagSet;
	private static TransitionSystem transitionSystem;
	private static boolean replaceTextFilters = false;
	private static boolean replaceTokenFilters = false;
	private static boolean replaceTokenSequenceFilters = false;
	private static String textFiltersStr;
	private static String tokenFiltersStr;
	private static String tokenSequenceFiltersStr;
	private static String posTaggerRulesStr;
	private static String parserRulesStr;
	
	private static Locale locale;
	
	public GenericLanguageImplementation(String sessionId) {
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
		String path = "/com/joliciel/talismane/resources/" + resource;
		LOG.debug("Getting " + path);
		InputStream inputStream = GenericLanguageImplementation.class.getResourceAsStream(path); 
		
		return inputStream;
	}

	@Override
	public Scanner getDefaultPosTaggerRulesScanner() {
		Scanner scanner = null;
		if (posTaggerRulesStr!=null)
			scanner = new Scanner(posTaggerRulesStr);
		return scanner;
	}
	

	@Override
	public Scanner getDefaultParserRulesScanner() {
		Scanner scanner = null;
		if (parserRulesStr!=null)
			scanner = new Scanner(parserRulesStr);
		return scanner;
	}


	@Override
	public List<PosTaggerLexicon> getDefaultLexicons() {
		if (lexicons==null) {
			lexicons = new ArrayList<PosTaggerLexicon>();
			lexicons.add(new EmptyLexicon());
		}
		return lexicons;
	}

	@Override
	public ClassificationModel<SentenceDetectorOutcome> getDefaultSentenceModel() {
		return sentenceModel;
	}

	@Override
	public ClassificationModel<TokeniserOutcome> getDefaultTokeniserModel() {
		return tokeniserModel;
	}

	@Override
	public ClassificationModel<PosTag> getDefaultPosTaggerModel() {
		return posTaggerModel;
	}

	@Override
	public MachineLearningModel getDefaultParserModel() {
		return parserModel;
	}

	@Override
	public Scanner getDefaultTokenSequenceFiltersScanner() {
		String tokenSequenceFilterString = "";
		if (tokenSequenceFiltersStr!=null) {
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
			if (textFiltersStr!=null) {
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
			if (tokenFiltersStr!=null) {
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
		List<PosTagSequenceFilter> filters = new ArrayList<PosTagSequenceFilter>();
		return filters;
	}


	@Override
	public PosTagSet getDefaultPosTagSet() {
		return posTagSet;
	}
	

	public PosTaggerService getPosTaggerService() {
		if (posTaggerService==null) {
			posTaggerService = talismaneServiceLocator.getPosTaggerServiceLocator().getPosTaggerService();
		}
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public ParserService getParserService() {
		if (parserService==null) {
			parserService = talismaneServiceLocator.getParserServiceLocator().getParserService();
		}
		return parserService;
	}

	public void setParserService(ParserService parserService) {
		this.parserService = parserService;
	}

	public TokenFilterService getTokenFilterService() {
		if (tokenFilterService==null)
			tokenFilterService = talismaneServiceLocator.getTokenFilterServiceLocator().getTokenFilterService();
		return tokenFilterService;
	}

	public void setTokenFilterService(TokenFilterService tokenFilterService) {
		this.tokenFilterService = tokenFilterService;
	}

	@Override
	public List<Class<? extends TokenSequenceFilter>> getAvailableTokenSequenceFilters() {
		if (availableTokenSequenceFilters==null) {
			availableTokenSequenceFilters = new ArrayList<Class<? extends TokenSequenceFilter>>();
		}
		return availableTokenSequenceFilters;
	}

	@Override
	public LinguisticRules getDefaultLinguisticRules() {
		return new GenericRules(this.getTalismaneSession());
	}

	public TalismaneSession getTalismaneSession() {
		if (talismaneSession==null) {
			talismaneSession = talismaneServiceLocator.getTalismaneService().getTalismaneSession();
		}
		return talismaneSession;
	}
	

	public MachineLearningService getMachineLearningService() {
		if (machineLearningService==null) {
			machineLearningService = talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService();
		}
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}
	
	public void setLanguagePack(File languagePackFile) {
		ZipInputStream zis = null;
		try {
			zis = new ZipInputStream(new FileInputStream(languagePackFile));
			ZipEntry ze = null;
			
			Map<String,String> argMap = new HashMap<String, String>();
		    while ((ze = zis.getNextEntry()) != null) {
		    	String name = ze.getName();
		    	if (name.indexOf('/')>=0) name = name.substring(name.lastIndexOf('/')+1);
		    	if (name.equals("languagePack.properties")) {
					Properties props = new Properties();
					props.load(zis);
					argMap = StringUtils.getArgMap(props);
					break;
		    	}
		    } // next zip entry
		    zis.close();
		    
		    Map<String,String> reverseMap = new HashMap<String, String>();
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
		    		reverseMap.put(value,key);
		    	}
		    }
		    
		    zis = new ZipInputStream(new FileInputStream(languagePackFile));
			ze = null;
			
		    while ((ze = zis.getNextEntry()) != null) {
		    	String name = ze.getName();
		    	if (name.indexOf('/')>=0) name = name.substring(name.lastIndexOf('/')+1);
		    	String key = reverseMap.get(name);
		    	if (key!=null) {
			    	if (key.equals("transitionSystem")) { 
						transitionSystem = this.getParserService().getArcEagerTransitionSystem();
						Scanner scanner = new Scanner(zis, "UTF-8");
						List<String> dependencyLabels = new ArrayList<String>();
						while (scanner.hasNextLine()) {
							String dependencyLabel = scanner.nextLine();
							if (!dependencyLabel.startsWith("#")) {
								if (dependencyLabel.indexOf('\t')>0)
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
			    		ZipInputStream innerZis = new ZipInputStream(new UnclosableInputStream(zis));
						sentenceModel = this.getMachineLearningService().getClassificationModel(innerZis);
			    	} else if (key.equals("tokeniserModel")) {
			    		ZipInputStream innerZis = new ZipInputStream(new UnclosableInputStream(zis));
			    		tokeniserModel = this.getMachineLearningService().getClassificationModel(innerZis);
			    	} else if (key.equals("posTaggerModel")) {
			    		ZipInputStream innerZis = new ZipInputStream(new UnclosableInputStream(zis));
			    		posTaggerModel = this.getMachineLearningService().getClassificationModel(innerZis);
			    	} else if (key.equals("parserModel")) {
			    		ZipInputStream innerZis = new ZipInputStream(new UnclosableInputStream(zis));
			    		parserModel = this.getMachineLearningService().getClassificationModel(innerZis);
			    	} else if (key.equals("lexicon")) {
			    		ZipInputStream innerZis = new ZipInputStream(new UnclosableInputStream(zis));
			    		LexiconDeserializer deserializer = new LexiconDeserializer(this.getTalismaneSession());
						lexicons = deserializer.deserializeLexicons(innerZis);
			    	}
		    	}
		    }
		} catch (FileNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} finally {
			if (zis!=null) {
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

}
