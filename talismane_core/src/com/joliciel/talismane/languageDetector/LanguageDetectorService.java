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
package com.joliciel.talismane.languageDetector;

import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.DecisionMaker;

public interface LanguageDetectorService {
	public LanguageDetector getLanguageDetector(DecisionMaker decisionMaker,
			Set<LanguageDetectorFeature<?>> features);
	public LanguageDetector getLanguageDetector(ClassificationModel model);

	
	public ClassificationEventStream getLanguageDetectorEventStream(LanguageDetectorAnnotatedCorpusReader corpusReader,
			Set<LanguageDetectorFeature<?>> features);
	
	/**
	 * A default reader which assumes one sentence per line.
	 * @param reader
	 * @return
	 */
	public LanguageDetectorAnnotatedCorpusReader getDefaultReader(Map<Locale,Reader> readerMap);

	public Set<LanguageDetectorFeature<?>> getFeatureSet(
			List<String> featureDescriptors);
	
	public LanguageDetectorProcessor getDefaultLanguageDetectorProcessor(Writer out);
}
