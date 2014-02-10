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
package com.joliciel.talismane.posTagger;

import java.util.List;
import java.util.Set;

import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerRule;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;

/**
 * The PosTagger's task is to apply PosTags to Words within a sentence.
 * @author Assaf Urieli
 *
 */
public interface PosTagger {
	/**
	 * Apply PosTags to the tokens in a given sentence.
	 * @param tokens the List of tokens comprising the sentence.
	 * @return a List of TaggedToken reflecting the PosTags applied to the tokens.
	 */
	public abstract PosTagSequence tagSentence(TokenSequence tokenSequence);

	/**
	 * Add an analysis observer to this pos tagger.
	 * @param observer
	 */
	public abstract void addObserver(ClassificationObserver<PosTag> observer);

	public abstract Set<PosTaggerFeature<?>> getPosTaggerFeatures();
	public void setPosTaggerFeatures(Set<PosTaggerFeature<?>> posTaggerFeatures);

	public abstract void setPosTaggerRules(List<PosTaggerRule> posTaggerRules);

	public abstract List<PosTaggerRule> getPosTaggerRules();
	
	/**
	 * Filters to be applied to the token sequences prior to pos-tagging.
	 * @return
	 */
	public List<TokenSequenceFilter> getPreProcessingFilters();

	public void setPreProcessingFilters(List<TokenSequenceFilter> tokenFilters);
	
	public void addPreProcessingFilter(TokenSequenceFilter tokenFilter);

	/**
	 * Filters to be applied to the final pos-tag sequences after pos-tagging.
	 * @return
	 */
	public List<PosTagSequenceFilter> getPostProcessingFilters();

	public void setPostProcessingFilters(List<PosTagSequenceFilter> posTagFilters);
	
	public void addPostProcessingFilter(PosTagSequenceFilter posTagFilter);
}