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

import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.utils.features.BooleanFeature;

class PosTaggerRuleImpl implements PosTaggerRule {
	private BooleanFeature<PosTaggerContext> condition;
	private PosTag tag;
	private boolean negative;
	
	public PosTaggerRuleImpl(BooleanFeature<PosTaggerContext> condition,
			PosTag tag) {
		super();
		this.condition = condition;
		this.tag = tag;
	}
	public BooleanFeature<PosTaggerContext> getCondition() {
		return condition;
	}

	public PosTag getTag() {
		return tag;
	}
	public boolean isNegative() {
		return negative;
	}
	public void setNegative(boolean negative) {
		this.negative = negative;
	}

}
