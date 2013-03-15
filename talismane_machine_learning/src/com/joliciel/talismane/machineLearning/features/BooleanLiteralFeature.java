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
package com.joliciel.talismane.machineLearning.features;

/**
 * Wrapper for a boolean literal.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class BooleanLiteralFeature<T> extends AbstractFeature<T, Boolean> implements
		BooleanFeature<T> {
	private boolean literal;
	
	public BooleanLiteralFeature(boolean literal) {
		super();
		this.literal = literal;
		this.setName("" + literal);
	}

	@Override
	public FeatureResult<Boolean> check(T context, RuntimeEnvironment env) {
		return this.generateResult(literal);
	}

	public boolean isLiteral() {
		return literal;
	}
}
