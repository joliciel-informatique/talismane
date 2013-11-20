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
package com.joliciel.talismane.parser.features;

import java.util.Set;

import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.parser.Transition;

final class ParserRuleImpl implements ParserRule {
	private BooleanFeature<ParseConfigurationWrapper> condition;
	private Transition transition;
	private Set<Transition> transitions;
	private boolean negative;
	
	public ParserRuleImpl(BooleanFeature<ParseConfigurationWrapper> condition,
			Transition transition) {
		super();
		this.condition = condition;
		this.transition = transition;
	}
	
	
	public ParserRuleImpl(BooleanFeature<ParseConfigurationWrapper> condition,
			Set<Transition> transitions) {
		super();
		this.condition = condition;
		this.transitions = transitions;
		this.transition = transitions.iterator().next();
	}
	
	public BooleanFeature<ParseConfigurationWrapper> getCondition() {
		return condition;
	}

	public Transition getTransition() {
		return transition;
	}
	public boolean isNegative() {
		return negative;
	}
	public void setNegative(boolean negative) {
		this.negative = negative;
	}
	@Override
	public String toString() {
		return "ParserRule: " + (negative ? "!" : "") + this.transition.getCode() + ": " + this.condition.getName();
	}
	
	public Set<Transition> getTransitions() {
		return transitions;
	}
	public void setTransitions(Set<Transition> transitions) {
		this.transitions = transitions;
	}
	

}
