///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2017 Joliciel Informatique
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
package com.joliciel.talismane.posTagger.features;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * For a given set of lexical attributes, combines all values found in the
 * lexicon into a single String.
 * 
 * @author Assaf Urieli
 *
 * @param <T>
 */
public abstract class AbstractLexicalAttributesFeature<T> extends AbstractPosTaggedTokenFeature<T, String>implements StringFeature<T> {

	public AbstractLexicalAttributesFeature(PosTaggedTokenAddressFunction<T> addressFunction) {
		super(addressFunction);
	}

	@Override
	public FeatureResult<String> checkInternal(T context, RuntimeEnvironment env) throws TalismaneException {
		PosTaggedTokenWrapper innerWrapper = this.getToken(context, env);
		if (innerWrapper == null)
			return null;
		PosTaggedToken posTaggedToken = innerWrapper.getPosTaggedToken();
		if (posTaggedToken == null)
			return null;
		FeatureResult<String> featureResult = null;

		List<String> attributes = this.getAttributes(innerWrapper, env);

		Map<String, Set<String>> results = new HashMap<>();
		for (String attribute : attributes) {
			Set<String> values = new TreeSet<>();
			results.put(attribute, values);
			for (LexicalEntry lexicalEntry : posTaggedToken.getLexicalEntries()) {
				values.addAll(lexicalEntry.getAttributeAsList(attribute));
			}
		}

		boolean firstAttribute = true;
		boolean haveAtLeastOne = false;
		StringBuilder sb = new StringBuilder();
		for (String attribute : attributes) {
			if (!firstAttribute)
				sb.append("|");
			Set<String> values = results.get(attribute);
			if (values.size() > 0) {
				haveAtLeastOne = true;
				sb.append(values.stream().collect(Collectors.joining(";")));
			}
			firstAttribute = false;
		}

		if (haveAtLeastOne) {
			String result = sb.toString();
			featureResult = this.generateResult(result);
		}

		return featureResult;
	}

	protected abstract List<String> getAttributes(PosTaggedTokenWrapper innerWrapper, RuntimeEnvironment env) throws TalismaneException;

}
