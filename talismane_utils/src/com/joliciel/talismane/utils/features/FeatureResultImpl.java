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
package com.joliciel.talismane.utils.features;

final class FeatureResultImpl<T> implements FeatureResult<T> {
	private Feature<?,T> feature;
	private T outcome;

	@SuppressWarnings("unchecked")
	public FeatureResultImpl(Feature<?,T> feature, T outcome) {
		this.feature = feature;
		if (outcome==null)
			throw new RuntimeException("Trying to set null outcome");
		
		if (outcome!=null && outcome instanceof String) {
			String string = (String) outcome;
			string = string.replace("_", "&und;");
			string = string.replace(' ', '_');
			string = string.replace("=", "&eq;");
			string = string.replace("\n", "&nl;");
			this.outcome = (T) string;
		} else {
			this.outcome = outcome;
		}
	}

	/* (non-Javadoc)
	 * @see com.joliciel.talismane.posTagger.features.FeatureResult#getFeature()
	 */
	@Override
	public Feature<?,T> getFeature() {
		return feature;
	}

	/* (non-Javadoc)
	 * @see com.joliciel.talismane.posTagger.features.FeatureResult#getOutcome()
	 */
	@Override
	public T getOutcome() {
		return this.outcome;
	}
	

	@Override
	public String toString() {
		String string = this.getName();
		if (outcome instanceof Double || outcome instanceof Integer)
			string += "=" + outcome.toString();
		return string;
	}

	@Override
	public String getName() {
		String string = feature.getName();
		if (!(outcome instanceof Double || outcome instanceof Integer))
			string += ":" + outcome.toString();
		return string;
	}
}
