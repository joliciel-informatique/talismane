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
package com.joliciel.talismane.machineLearning;

/**
 * An abstract decision factory, for creating decisions of a particular outcome type.
 * @author Assaf Urieli
 *
 * @param <T> the outcome type
 */
public abstract class AbstractDecisionFactory<T extends Outcome> implements DecisionFactory<T> {
	private static final long serialVersionUID = 19238305543958986L;

	@Override
	public Decision<T> createDecision(String name, double score,
			double probability) {
		DecisionImpl<T> decision = new DecisionImpl<T>(name, probability);
		decision.setOutcome(this.createOutcome(name));
		decision.setScore(score);
		return decision;
	}

	@Override
	public Decision<T> createDecision(String name, double probability) {
		DecisionImpl<T> decision = new DecisionImpl<T>(name, probability);
		decision.setOutcome(this.createOutcome(name));
		return decision;
	}

	@Override
	public Decision<T> createDefaultDecision(T outcome) {
		DecisionImpl<T> decision = new DecisionImpl<T>(outcome);
		return decision;
	}

	/**
	 * Create an outcome of type T for a given code.
	 */
	public abstract T createOutcome(String code);
}
