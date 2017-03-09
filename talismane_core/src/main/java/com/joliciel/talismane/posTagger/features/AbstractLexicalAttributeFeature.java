package com.joliciel.talismane.posTagger.features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringCollectionFeature;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * For a given set of lexical attributes, returns one String per combination
 * found in a lexical entry associated with this pos-tagged token.
 * 
 * @author Assaf Urieli
 *
 * @param <T>
 */
public abstract class AbstractLexicalAttributeFeature<T> extends AbstractPosTaggedTokenFeature<T, List<WeightedOutcome<String>>>
		implements StringCollectionFeature<T> {

	public AbstractLexicalAttributeFeature(PosTaggedTokenAddressFunction<T> addressFunction) {
		super(addressFunction);
	}

	@Override
	public FeatureResult<List<WeightedOutcome<String>>> checkInternal(T context, RuntimeEnvironment env) {
		PosTaggedTokenWrapper innerWrapper = this.getToken(context, env);
		if (innerWrapper == null)
			return null;
		PosTaggedToken posTaggedToken = innerWrapper.getPosTaggedToken();
		if (posTaggedToken == null)
			return null;
		FeatureResult<List<WeightedOutcome<String>>> featureResult = null;

		List<String> attributes = this.getAttributes(innerWrapper, env);

		Set<String> results = new HashSet<>();
		for (LexicalEntry lexicalEntry : posTaggedToken.getLexicalEntries()) {
			boolean haveAtLeastOne = false;

			Set<String> previousAttributeStrings = new HashSet<>();
			previousAttributeStrings.add("");
			for (String attribute : attributes) {
				List<String> values = lexicalEntry.getAttributeAsList(attribute);
				if (values.size() > 0) {
					Set<String> currentAttributeStrings = new HashSet<>();

					haveAtLeastOne = true;
					for (String value : values) {
						for (String prevString : previousAttributeStrings) {
							if (prevString.length() > 0)
								currentAttributeStrings.add(prevString + "|" + value);
							else
								currentAttributeStrings.add(value);
						}
					}
					previousAttributeStrings = currentAttributeStrings;
				}
			}
			if (haveAtLeastOne) {
				results.addAll(previousAttributeStrings);
			}
		}

		if (results.size() > 0) {
			List<WeightedOutcome<String>> outcomes = new ArrayList<>(results.size());
			for (String result : results) {
				outcomes.add(new WeightedOutcome<String>(result, 1.0));
			}
			featureResult = this.generateResult(outcomes);
		}

		return featureResult;
	}

	protected abstract List<String> getAttributes(PosTaggedTokenWrapper innerWrapper, RuntimeEnvironment env);

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return StringCollectionFeature.class;
	}
}
