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
package com.joliciel.talismane.tokeniser.features;

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureClassContainer;
import com.joliciel.talismane.posTagger.features.TokenIndexFeature;
import com.joliciel.talismane.posTagger.features.WordFormFeature;

/**
 * A helper class for adding generic token features to any parser requiring
 * them.
 * 
 * @author Assaf Urieli
 *
 */
public class TokenFeatureParser {

	/**
	 * Add token feature classes to the container provided, including:
	 * <ul>
	 * <li>AndRange: {@link AndRangeFeature}</li>
	 * <li>BackwardSearch: {@link BackwardSearchFeature}</li>
	 * <li>CountIf: {@link CountIfFeature}</li>
	 * <li>FirstWordInCompound: {@link FirstWordInCompoundFeature}</li>
	 * <li>FirstWordInSentence: {@link FirstWordInSentenceFeature}</li>
	 * <li>ForwardSearch: {@link ForwardSearchFeature}</li>
	 * <li>Has: {@link HasFeature}</li>
	 * <li>HasClosedClassesOnly: {@link HasClosedClassesOnlyFeature}</li>
	 * <li>IfThenElse: {@link IfThenElseTokenAddressFeature}</li>
	 * <li>LastWordInCompound: {@link LastWordInCompoundFeature}</li>
	 * <li>LastWordInSentence: {@link LastWordInSentenceFeature}</li>
	 * <li>LemmaForPosTag: {@link LemmaForPosTagFeature}</li>
	 * <li>LexiconAllPosTags: {@link LexiconAllPosTagsFeature}</li>
	 * <li>LexiconPosTag: {@link LexiconPosTagFeature}</li>
	 * <li>LexiconPosTagForString: {@link LexiconPosTagForStringFeature}</li>
	 * <li>LexiconPosTags: {@link LexiconPosTagsFeature}</li>
	 * <li>LexiconPosTagsForString: {@link LexiconPosTagsForStringFeature}</li>
	 * <li>NLetterPrefix: {@link NLetterPrefixFeature}</li>
	 * <li>NLetterSuffix: {@link NLetterSuffixFeature}</li>
	 * <li>Offset: {@link TokenOffsetAddressFunction}</li>
	 * <li>OrRange: {@link OrRangeFeature}</li>
	 * <li>PosTagSet: {@link PosTagSetFeature}</li>
	 * <li>Regex: {@link RegexFeature}</li>
	 * <li>TokenAt: {@link TokenAtAddressFunction}</li>
	 * <li>TokenIndex: {@link TokenIndexFeature}</li>
	 * <li>UnknownWord: {@link UnknownWordFeature}</li>
	 * <li>Word: {@link WordFeature}</li>
	 * <li>WordForm: {@link WordFormFeature}</li>
	 * </ul>
	 */
	public static void addFeatureClasses(FeatureClassContainer container) {
		container.addFeatureClass("AndRange", AndRangeFeature.class);
		container.addFeatureClass("BackwardSearch", BackwardSearchFeature.class);
		container.addFeatureClass("CountIf", CountIfFeature.class);
		container.addFeatureClass("FirstWordInCompound", FirstWordInCompoundFeature.class);
		container.addFeatureClass("FirstWordInSentence", FirstWordInSentenceFeature.class);
		container.addFeatureClass("ForwardSearch", ForwardSearchFeature.class);
		container.addFeatureClass("Has", HasFeature.class);
		container.addFeatureClass("HasClosedClassesOnly", HasClosedClassesOnlyFeature.class);
		container.addFeatureClass("IfThenElse", IfThenElseTokenAddressFeature.class);
		container.addFeatureClass("LastWordInCompound", LastWordInCompoundFeature.class);
		container.addFeatureClass("LastWordInSentence", LastWordInSentenceFeature.class);
		container.addFeatureClass("LemmaForPosTag", LemmaForPosTagFeature.class);
		container.addFeatureClass("LexiconAllPosTags", LexiconAllPosTagsFeature.class);
		container.addFeatureClass("LexiconPosTag", LexiconPosTagFeature.class);
		container.addFeatureClass("LexiconPosTagForString", LexiconPosTagForStringFeature.class);
		container.addFeatureClass("LexiconPosTags", LexiconPosTagsFeature.class);
		container.addFeatureClass("LexiconPosTagsForString", LexiconPosTagsForStringFeature.class);
		container.addFeatureClass("NLetterPrefix", NLetterPrefixFeature.class);
		container.addFeatureClass("NLetterSuffix", NLetterSuffixFeature.class);
		container.addFeatureClass("Offset", TokenOffsetAddressFunction.class);
		container.addFeatureClass("OrRange", OrRangeFeature.class);
		container.addFeatureClass("PosTagSet", PosTagSetFeature.class);
		container.addFeatureClass("Regex", RegexFeature.class);
		container.addFeatureClass("TokenAt", TokenAtAddressFunction.class);
		container.addFeatureClass("TokenIndex", TokenIndexFeature.class);
		container.addFeatureClass("UnknownWord", UnknownWordFeature.class);
		container.addFeatureClass("Word", WordFeature.class);
		container.addFeatureClass("WordForm", WordFormFeature.class);
	}

	public static void injectDependencies(@SuppressWarnings("rawtypes") Feature feature, TalismaneSession talismaneSession) {
		if (feature instanceof NeedsTalismaneSession) {
			((NeedsTalismaneSession) feature).setTalismaneSession(talismaneSession);
		}
	}

}
