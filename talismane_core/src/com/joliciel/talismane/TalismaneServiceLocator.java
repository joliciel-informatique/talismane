///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.features.FeatureServiceLocator;
import com.joliciel.talismane.parser.ParserServiceLocator;
import com.joliciel.talismane.parser.features.ParserFeatureServiceLocator;
import com.joliciel.talismane.posTagger.PosTaggerServiceLocator;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureServiceLocator;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorServiceLocator;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureServiceLocator;
import com.joliciel.talismane.tokeniser.TokeniserServiceLocator;
import com.joliciel.talismane.tokeniser.features.TokeniserFeatureServiceLocator;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternServiceLocator;

/**
 * Top-level locator for implementations of Talismane interfaces.
 * @author Assaf Urieli
 *
 */
public class TalismaneServiceLocator {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(TalismaneServiceLocator.class);
	private PosTaggerFeatureServiceLocator posTaggerFeatureServiceLocator;
	private PosTaggerServiceLocator posTaggerServiceLocator;
	private TokeniserServiceLocator tokeniserServiceLocator;
	private TokeniserFeatureServiceLocator tokeniserFeatureServiceLocator;
	private TokeniserPatternServiceLocator tokeniserPatternServiceLocator;
	private SentenceDetectorServiceLocator sentenceDetectorServiceLocator;
	private SentenceDetectorFeatureServiceLocator sentenceDetectorFeatureServiceLocator;
	private ParserServiceLocator parserServiceLocator;
	private ParserFeatureServiceLocator parserFeatureServiceLocator;
	private FeatureServiceLocator featureServiceLocator;
	
    private static TalismaneServiceLocator instance = null;

    private TalismaneServiceLocator() {
    	
    }
    
    public static TalismaneServiceLocator getInstance() {
    	if (instance==null) {
    		instance = new TalismaneServiceLocator();
    	}
    	return instance;
    }
	

	public PosTaggerFeatureServiceLocator getPosTaggerFeatureServiceLocator() {
		if (this.posTaggerFeatureServiceLocator==null) {
			this.posTaggerFeatureServiceLocator = new PosTaggerFeatureServiceLocator(this);
		}
		return posTaggerFeatureServiceLocator;
	}
	

	public PosTaggerServiceLocator getPosTaggerServiceLocator() {
		if (this.posTaggerServiceLocator==null) {
			this.posTaggerServiceLocator = new PosTaggerServiceLocator(this);
		}
		return posTaggerServiceLocator;
	}
	

	public TokeniserServiceLocator getTokeniserServiceLocator() {
		if (this.tokeniserServiceLocator==null) {
			this.tokeniserServiceLocator = new TokeniserServiceLocator(this);
		}
		return tokeniserServiceLocator;
	}
	

	public SentenceDetectorServiceLocator getSentenceDetectorServiceLocator() {
		if (this.sentenceDetectorServiceLocator==null) {
			this.sentenceDetectorServiceLocator = new SentenceDetectorServiceLocator(this);
		}
		return sentenceDetectorServiceLocator;
	}

	public SentenceDetectorFeatureServiceLocator getSentenceDetectorFeatureServiceLocator() {
		if (this.sentenceDetectorFeatureServiceLocator==null) {
			this.sentenceDetectorFeatureServiceLocator = new SentenceDetectorFeatureServiceLocator(this);
		}
		return sentenceDetectorFeatureServiceLocator;
	}

	public ParserServiceLocator getParserServiceLocator() {
		if (this.parserServiceLocator==null) {
			this.parserServiceLocator = new ParserServiceLocator(this);
		}
		return parserServiceLocator;
	}


	public ParserFeatureServiceLocator getParserFeatureServiceLocator() {
		if (this.parserFeatureServiceLocator==null) {
			this.parserFeatureServiceLocator = new ParserFeatureServiceLocator(this);
		}
		return parserFeatureServiceLocator;
	}

	public FeatureServiceLocator getFeatureServiceLocator() {
		if (this.featureServiceLocator==null) {
			this.featureServiceLocator = FeatureServiceLocator.getInstance();
		}
		return featureServiceLocator;
	}

	public TokeniserFeatureServiceLocator getTokeniserFeatureServiceLocator() {
		if (this.tokeniserFeatureServiceLocator==null) {
			this.tokeniserFeatureServiceLocator = new TokeniserFeatureServiceLocator(this);
		}
		return tokeniserFeatureServiceLocator;

	}
	

	public TokeniserPatternServiceLocator getTokenPatternServiceLocator() {
		if (this.tokeniserPatternServiceLocator==null) {
			this.tokeniserPatternServiceLocator = new TokeniserPatternServiceLocator(this);
		}
		return tokeniserPatternServiceLocator;

	}
}
