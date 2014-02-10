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
//////////////////////////////////////////////////////////////////////////////package com.joliciel.talismane.parser;
package com.joliciel.talismane.parser;

import java.io.File;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.stats.FScoreCalculator;

/**
 * Calculates the f-score during a parse evaluation.
 * @author Assaf Urieli
 *
 */
public class ParseEvaluationFScoreCalculator implements ParseEvaluationObserver {
	private static final Log LOG = LogFactory.getLog(ParseEvaluationFScoreCalculator.class);
	FScoreCalculator<String> fscoreCalculator = new FScoreCalculator<String>();
	private boolean labeledEvaluation = true;
	private boolean hasTokeniser = false;
	private boolean hasPosTagger = false;
	private File fscoreFile;
	private String skipLabel = null;
	
	public ParseEvaluationFScoreCalculator() {}
	public ParseEvaluationFScoreCalculator(File fscoreFile) {
		this.fscoreFile = fscoreFile;
	}
	@Override
	public void onNextParseConfiguration(ParseConfiguration realConfiguration,
			List<ParseConfiguration> guessedConfigurations) {
		PosTagSequence posTagSequence = realConfiguration.getPosTagSequence();		
		ParseConfiguration bestGuess = guessedConfigurations.get(0);
		int mismatchedTokens = 0;
		for (PosTaggedToken posTaggedToken : posTagSequence) {
			if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
				DependencyArc realArc = realConfiguration.getGoverningDependency(posTaggedToken);
				
				DependencyArc guessedArc = null;

				boolean foundToken = false;
				for (PosTaggedToken guessedToken : bestGuess.getPosTagSequence()) {
					if (guessedToken.getToken().getStartIndex()==posTaggedToken.getToken().getStartIndex()) {
						if (guessedToken.getToken().isEmpty()&&!posTaggedToken.getToken().isEmpty())
							continue;
						if (!guessedToken.getToken().isEmpty()&&posTaggedToken.getToken().isEmpty())
							continue;
						foundToken = true;
						guessedArc = bestGuess.getGoverningDependency(guessedToken);
						break;
					}
				}
				
				if (!foundToken) {
					LOG.info("Mismatched token :" + posTaggedToken.getToken().getText() + ", index " + posTaggedToken.getToken().getIndex());
					mismatchedTokens+=1;
				}
				
				String realLabel = realArc==null ? "noHead" : labeledEvaluation ? realArc.getLabel() : "head";
				String guessedLabel = guessedArc==null ? "noHead" : labeledEvaluation ? guessedArc.getLabel() : "head";
				
				if (realLabel==null||realLabel.length()==0) realLabel = "noLabel";
				if (guessedLabel==null||guessedLabel.length()==0) guessedLabel = "noLabel";
				
				// anything attached "by default" to the root, without a label, should be considered a "no head" rather than "no label"
				if (realArc!=null && realArc.getHead().getTag().equals(PosTag.ROOT_POS_TAG) && realLabel.equals("noLabel"))
					realLabel = "noHead";
				if (guessedArc!=null && guessedArc.getHead().getTag().equals(PosTag.ROOT_POS_TAG) && guessedLabel.equals("noLabel"))
					guessedLabel = "noHead";
				
				if (realArc==null || guessedArc==null) {
					fscoreCalculator.increment(realLabel, guessedLabel);
				} else {
					boolean sameHead = realArc.getHead().getToken().getStartIndex()==guessedArc.getHead().getToken().getStartIndex();

					if (sameHead) {
						fscoreCalculator.increment(realLabel, guessedLabel);
					} else if (guessedLabel.equals("noHead")) {
						fscoreCalculator.increment(realLabel, "noHead");
					} else if (realArc.getLabel().equals(guessedArc.getLabel())) {
						fscoreCalculator.increment(realLabel, "wrongHead");
					} else {
						fscoreCalculator.increment(realLabel, "wrongHeadWrongLabel");
					}
					
				} // have one of the arcs
			} // is root tag?
		} // next pos-tagged token
		
		if ((double) mismatchedTokens / (double) posTagSequence.size() > 0.5) {
			// more than half of the tokens mismatched?
			throw new TalismaneException("Too many mismatched tokens in sentence: " + posTagSequence.getTokenSequence().getSentence().getText());
		}
	}

	public FScoreCalculator<String> getFscoreCalculator() {
		return fscoreCalculator;
	}

	public boolean isLabeledEvaluation() {
		return labeledEvaluation;
	}

	public void setLabeledEvaluation(boolean labeledEvaluation) {
		this.labeledEvaluation = labeledEvaluation;
	}

	@Override
	public void onEvaluationComplete() {
		if (fscoreFile!=null) {
			FScoreCalculator<String> fScoreCalculator = this.getFscoreCalculator();
			
			double fscore = fScoreCalculator.getTotalFScore();
			LOG.debug("F-score: " + fscore);
			fScoreCalculator.writeScoresToCSVFile(fscoreFile);
		}
	}

	public boolean isHasTokeniser() {
		return hasTokeniser;
	}

	public void setHasTokeniser(boolean hasTokeniser) {
		this.hasTokeniser = hasTokeniser;
	}

	public boolean isHasPosTagger() {
		return hasPosTagger;
	}

	public void setHasPosTagger(boolean hasPosTagger) {
		this.hasPosTagger = hasPosTagger;
	}
	
	public String getSkipLabel() {
		return skipLabel;
	}
	public void setSkipLabel(String skipLabel) {
		this.skipLabel = skipLabel;
	}
}
