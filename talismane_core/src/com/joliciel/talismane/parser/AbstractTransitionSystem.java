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
package com.joliciel.talismane.parser;

import com.joliciel.talismane.machineLearning.AbstractDecisionFactory;
import com.joliciel.talismane.machineLearning.Decision;

abstract class AbstractTransitionSystem extends AbstractDecisionFactory<Transition> implements TransitionSystem {
	private static final long serialVersionUID = -8654873067306650539L;

	@Override
	public Decision<Transition> createDecision(String name, double probability) {
		Decision<Transition> decision = super.createDecision(name, probability);
		decision.getOutcome().setDecision(decision);
		return decision;
	}

	@Override
	public Decision<Transition> createDefaultDecision(Transition outcome) {
		Decision<Transition> decision = super.createDefaultDecision(outcome);
		outcome.setDecision(decision);
		return decision;
	}

	@Override
	public Transition createOutcome(String code) {
		return this.getTransitionForCode(code);
	}
}
