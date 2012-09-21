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
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.FeatureWrapper;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.tokeniser.features.TokenFeatureParser;

class PosTagFeatureParser extends AbstractFeatureParser<PosTaggerContext> {
	TokenFeatureParser tokenFeatureParser;

	public PosTagFeatureParser(FeatureService featureService) {
		super(featureService);
	}	

	@Override
	public void addFeatureClasses(FeatureClassContainer container) {
		container.addFeatureClass("Ngram", NgramFeature.class);
		container.addFeatureClass("PrevPosTag", PrevPosTagFeature.class);
		container.addFeatureClass("PrevPosTagIs", PrevPosTagIsFeature.class);
		this.tokenFeatureParser.addFeatureClasses(container);
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
	protected Object parseArgument(FunctionDescriptor argumentDescriptor) {
		return null;
	}

	@Override
	public List<FunctionDescriptor> getModifiedDescriptors(
			FunctionDescriptor functionDescriptor) {
		return tokenFeatureParser.getModifiedDescriptors(functionDescriptor);
	}

	public TokenFeatureParser getTokenFeatureParser() {
		return tokenFeatureParser;
	}

	public void setTokenFeatureParser(
			TokenFeatureParser tokenFeatureParser) {
		this.tokenFeatureParser = tokenFeatureParser;
	}

	private static class PosTaggerFeatureWrapper<T> extends AbstractFeature<PosTaggerContext,T> implements
		PosTaggerFeature<T>, FeatureWrapper<PosTaggerContext, T> {
		private Feature<PosTaggerContext,T> wrappedFeature = null;
		
		public PosTaggerFeatureWrapper(
				Feature<PosTaggerContext, T> wrappedFeature) {
			super();
			this.wrappedFeature = wrappedFeature;
			this.setName(wrappedFeature.getName());
			this.setGroupName(wrappedFeature.getGroupName());
		}
		
		@Override
		public FeatureResult<T> check(PosTaggerContext context) {
			return wrappedFeature.check(context);
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
	}
	
	private class PosTaggerBooleanFeatureWrapper extends PosTaggerFeatureWrapper<Boolean> implements BooleanFeature<PosTaggerContext> {
		public PosTaggerBooleanFeatureWrapper(
				Feature<PosTaggerContext, Boolean> wrappedFeature) {
			super(wrappedFeature);
		}
	}
	
	private class PosTaggerStringFeatureWrapper extends PosTaggerFeatureWrapper<String> implements StringFeature<PosTaggerContext> {
		public PosTaggerStringFeatureWrapper(
				Feature<PosTaggerContext, String> wrappedFeature) {
			super(wrappedFeature);
		}
	}
	
	private class PosTaggerDoubleFeatureWrapper extends PosTaggerFeatureWrapper<Double> implements DoubleFeature<PosTaggerContext> {
		public PosTaggerDoubleFeatureWrapper(
				Feature<PosTaggerContext, Double> wrappedFeature) {
			super(wrappedFeature);
		}
	}
	
	private class PosTaggerIntegerFeatureWrapper extends PosTaggerFeatureWrapper<Integer> implements IntegerFeature<PosTaggerContext> {
		public PosTaggerIntegerFeatureWrapper(
				Feature<PosTaggerContext, Integer> wrappedFeature) {
			super(wrappedFeature);
		}
	}

}
