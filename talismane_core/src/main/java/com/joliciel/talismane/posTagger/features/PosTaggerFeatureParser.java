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
package com.joliciel.talismane.posTagger.features;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.machineLearning.features.AbstractFeature;
import com.joliciel.talismane.machineLearning.features.AbstractFeatureParser;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.DoubleFeature;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureClassContainer;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureWrapper;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.tokeniser.features.TokenAddressFunction;
import com.joliciel.talismane.tokeniser.features.TokenFeatureParser;
import com.joliciel.talismane.tokeniser.features.TokenWrapper;

/**
 * A feature parser for PosTaggerContext features.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTaggerFeatureParser extends AbstractFeatureParser<PosTaggerContext> {
	private final TokenFeatureParser tokenFeatureParser;

	public PosTaggerFeatureParser(TokenFeatureParser tokenFeatureParser) {
		this.tokenFeatureParser = tokenFeatureParser;
		this.setExternalResourceFinder(tokenFeatureParser.getTalismaneSession().getExternalResourceFinder());
	}

	@Override
	public void addFeatureClasses(FeatureClassContainer container) {
		container.addFeatureClass("Ngram", NgramFeature.class);
		container.addFeatureClass("History", PosTaggerHistoryAddressFunction.class);
		container.addFeatureClass("HistoryAbs", HistoryAbsoluteAddressFunction.class);
		container.addFeatureClass("HistoryCountIf", HistoryCountIfFeature.class);
		container.addFeatureClass("HistoryHas", HistoryHasFeature.class);
		container.addFeatureClass("HistorySearch", HistorySearchFeature.class);
		PosTaggerFeatureParser.addPosTaggedTokenFeatureClasses(container);
		this.tokenFeatureParser.addFeatureClasses(container);
	}

	/**
	 * Add pos-tagged token feature classes to the container provided.
	 */
	public static void addPosTaggedTokenFeatureClasses(FeatureClassContainer container) {
		container.addFeatureClass("PosTag", AssignedPosTagFeature.class);
		container.addFeatureClass("PosTagIn", AssignedPosTagInFeature.class);
		container.addFeatureClass("LexicalForm", WordFormFeature.class);
		container.addFeatureClass("Lemma", LemmaFeature.class);
		container.addFeatureClass("Morphology", MorphologyFeature.class);
		container.addFeatureClass("Gender", GrammaticalGenderFeature.class);
		container.addFeatureClass("Number", GrammaticalNumberFeature.class);
		container.addFeatureClass("Person", GrammaticalPersonFeature.class);
		container.addFeatureClass("Tense", VerbTenseFeature.class);
		container.addFeatureClass("Category", LexicalCategoryFeature.class);
		container.addFeatureClass("SubCategory", LexicalSubCategoryFeature.class);
		container.addFeatureClass("PossessorNumber", PossessorNumberFeature.class);
		container.addFeatureClass("Index", TokenIndexFeature.class);
		container.addFeatureClass("WordForm", WordFormFeature.class);
		container.addFeatureClass("ClosedClass", ClosedClassFeature.class);
		container.addFeatureClass("TokenHas", HistoryHasFeature.class);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<PosTaggerFeature<?>> parseDescriptor(FunctionDescriptor functionDescriptor) {
		List<Feature<PosTaggerContext, ?>> posTaggerFeatures = this.parse(functionDescriptor);
		List<PosTaggerFeature<?>> wrappedFeatures = new ArrayList<PosTaggerFeature<?>>();
		for (Feature<PosTaggerContext, ?> posTaggerFeature : posTaggerFeatures) {
			PosTaggerFeature<?> wrappedFeature = null;
			if (posTaggerFeature instanceof PosTaggerFeature) {
				wrappedFeature = (PosTaggerFeature<?>) posTaggerFeature;
			} else if (posTaggerFeature instanceof BooleanFeature) {
				wrappedFeature = new PosTaggerBooleanFeatureWrapper((Feature<PosTaggerContext, Boolean>) posTaggerFeature);
			} else if (posTaggerFeature instanceof StringFeature) {
				wrappedFeature = new PosTaggerStringFeatureWrapper((Feature<PosTaggerContext, String>) posTaggerFeature);
			} else if (posTaggerFeature instanceof IntegerFeature) {
				wrappedFeature = new PosTaggerIntegerFeatureWrapper((Feature<PosTaggerContext, Integer>) posTaggerFeature);
			} else if (posTaggerFeature instanceof DoubleFeature) {
				wrappedFeature = new PosTaggerDoubleFeatureWrapper((Feature<PosTaggerContext, Double>) posTaggerFeature);
			} else {
				wrappedFeature = new PosTaggerFeatureWrapper(posTaggerFeature);
			}
			wrappedFeatures.add(wrappedFeature);
		}
		return wrappedFeatures;
	}

	@Override
	public List<FunctionDescriptor> getModifiedDescriptors(FunctionDescriptor functionDescriptor) {
		return tokenFeatureParser.getModifiedDescriptors(functionDescriptor);
	}

	private static class PosTaggerFeatureWrapper<T> extends AbstractFeature<PosTaggerContext, T>
			implements PosTaggerFeature<T>, FeatureWrapper<PosTaggerContext, T> {
		private Feature<PosTaggerContext, T> wrappedFeature = null;

		public PosTaggerFeatureWrapper(Feature<PosTaggerContext, T> wrappedFeature) {
			super();
			this.wrappedFeature = wrappedFeature;
			this.setName(wrappedFeature.getName());
			this.setCollectionName(wrappedFeature.getCollectionName());
		}

		@Override
		public FeatureResult<T> check(PosTaggerContext context, RuntimeEnvironment env) {
			return wrappedFeature.check(context, env);
		}

		@Override
		public Feature<PosTaggerContext, T> getWrappedFeature() {
			return this.wrappedFeature;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Class<? extends Feature> getFeatureType() {
			return wrappedFeature.getFeatureType();
		}

		@Override
		public String toString() {
			return wrappedFeature.toString();
		}

	}

	private class PosTaggerBooleanFeatureWrapper extends PosTaggerFeatureWrapper<Boolean> implements BooleanFeature<PosTaggerContext> {
		public PosTaggerBooleanFeatureWrapper(Feature<PosTaggerContext, Boolean> wrappedFeature) {
			super(wrappedFeature);
		}
	}

	private class PosTaggerStringFeatureWrapper extends PosTaggerFeatureWrapper<String> implements StringFeature<PosTaggerContext> {
		public PosTaggerStringFeatureWrapper(Feature<PosTaggerContext, String> wrappedFeature) {
			super(wrappedFeature);
		}
	}

	private class PosTaggerDoubleFeatureWrapper extends PosTaggerFeatureWrapper<Double> implements DoubleFeature<PosTaggerContext> {
		public PosTaggerDoubleFeatureWrapper(Feature<PosTaggerContext, Double> wrappedFeature) {
			super(wrappedFeature);
		}
	}

	private class PosTaggerIntegerFeatureWrapper extends PosTaggerFeatureWrapper<Integer> implements IntegerFeature<PosTaggerContext> {
		public PosTaggerIntegerFeatureWrapper(Feature<PosTaggerContext, Integer> wrappedFeature) {
			super(wrappedFeature);
		}
	}

	@Override
	public void injectDependencies(@SuppressWarnings("rawtypes") Feature feature) {
		this.tokenFeatureParser.injectDependencies(feature);
	}

	@Override
	protected boolean canConvert(Class<?> parameterType, Class<?> originalArgumentType) {
		if (TokenAddressFunction.class.isAssignableFrom(parameterType) && PosTaggedTokenAddressFunction.class.isAssignableFrom(originalArgumentType))
			return true;
		return false;
	}

	@Override
	protected Feature<PosTaggerContext, ?> convertArgument(Class<?> parameterType, Feature<PosTaggerContext, ?> originalArgument) {
		if (TokenAddressFunction.class.isAssignableFrom(parameterType) && (originalArgument instanceof PosTaggedTokenAddressFunction)) {
			@SuppressWarnings("unchecked")
			PosTaggedTokenAddressFunction<PosTaggerContext> originalAddressFunction = (PosTaggedTokenAddressFunction<PosTaggerContext>) originalArgument;
			Feature<PosTaggerContext, ?> convertedFunction = new TokenAddressFunctionWrapper(originalAddressFunction);
			return convertedFunction;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Feature<PosTaggerContext, ?> convertFeatureCustomType(Feature<PosTaggerContext, ?> feature) {
		Feature<PosTaggerContext, ?> convertedFeature = feature;
		if (feature.getFeatureType().equals(PosTaggedTokenAddressFunction.class) && !(feature instanceof PosTaggedTokenAddressFunction)) {
			convertedFeature = new PosTaggedTokenAddressFunctionWrapper<PosTaggerContext>((Feature<PosTaggerContext, PosTaggedTokenWrapper>) feature);
		}
		return convertedFeature;
	}

	private static final class TokenAddressFunctionWrapper extends AbstractFeature<PosTaggerContext, TokenWrapper>
			implements TokenAddressFunction<PosTaggerContext> {
		PosTaggedTokenAddressFunction<PosTaggerContext> posTaggedTokenAddressFunction = null;

		public TokenAddressFunctionWrapper(PosTaggedTokenAddressFunction<PosTaggerContext> posTaggedTokenAddressFunction) {
			this.posTaggedTokenAddressFunction = posTaggedTokenAddressFunction;
			this.setName(this.posTaggedTokenAddressFunction.getName());
		}

		@Override
		public FeatureResult<TokenWrapper> check(PosTaggerContext context, RuntimeEnvironment env) {
			FeatureResult<PosTaggedTokenWrapper> posTaggedTokenResult = posTaggedTokenAddressFunction.check(context, env);
			FeatureResult<TokenWrapper> result = null;
			if (posTaggedTokenResult != null) {
				result = this.generateResult(posTaggedTokenResult.getOutcome().getPosTaggedToken());
			}
			return result;
		}

	}
}
