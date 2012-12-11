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
//////////////////////////////////////////////////////////////////////////////package com.joliciel.talismane.parser;
package com.joliciel.talismane.parser;

import java.util.List;

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
	FScoreCalculator<String> fscoreCalculator = new FScoreCalculator<String>();
	private boolean labeledEvaluation = true;
	private boolean hasTokeniser = false;
	private boolean hasPosTagger = false;
	
	@Override
	public void onNextParseConfiguration(ParseConfiguration realConfiguration,
			List<ParseConfiguration> guessedConfigurations) {
		PosTagSequence posTagSequence = realConfiguration.getPosTagSequence();		
		ParseConfiguration bestGuess = guessedConfigurations.get(0);
		for (PosTaggedToken posTaggedToken : posTagSequence) {
			if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
				DependencyArc realArc = realConfiguration.getGoverningDependency(posTaggedToken);
				
				DependencyArc guessedArc = null;
				if (!hasTokeniser && !hasPosTagger) {
					guessedArc = bestGuess.getGoverningDependency(posTaggedToken);
				} else {
					for (PosTaggedToken guessedToken : bestGuess.getPosTagSequence()) {
						if (guessedToken.getToken().getStartIndex()==posTaggedToken.getToken().getStartIndex()) {
							guessedArc = bestGuess.getGoverningDependency(guessedToken);
							break;
						}
					}
				}
				
				String realLabel = realArc==null ? "noHead" : labeledEvaluation ? realArc.getLabel() : "head";
				String guessedLabel = guessedArc==null ? "noHead" : labeledEvaluation ? guessedArc.getLabel() : "head";
				
				if (realLabel==null||realLabel.length()==0) realLabel = "noLabel";
				if (guessedLabel==null||guessedLabel.length()==0) guessedLabel = "noLabel";
				
				if (realArc==null || guessedArc==null) {
					fscoreCalculator.increment(realLabel, guessedLabel);
				} else {
					if (hasTokeniser || hasPosTagger) {
						if (realArc.getHead().getToken().getStartIndex()==guessedArc.getHead().getToken().getStartIndex()) {
							fscoreCalculator.increment(realLabel, guessedLabel);
						} else if (realArc.getLabel().equals(guessedArc.getLabel())) {
							fscoreCalculator.increment(realLabel, "wrongHead");
						} else {
							fscoreCalculator.increment(realLabel, "wrongHeadWrongLabel");
						}
					} else {
						if (realArc.getHead().equals(guessedArc.getHead())) {
							fscoreCalculator.increment(realLabel, guessedLabel);
						} else if (realArc.getLabel().equals(guessedArc.getLabel())) {
							fscoreCalculator.increment(realLabel, "wrongHead");
						} else {
							fscoreCalculator.increment(realLabel, "wrongHeadWrongLabel");
						}
					}
				}
			}
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
		// nothing to do here
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
	
	
}
