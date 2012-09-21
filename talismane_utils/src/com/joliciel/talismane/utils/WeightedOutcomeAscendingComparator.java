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
package com.joliciel.talismane.utils;

import java.util.Comparator;

/**
 * Orders weighted outcomes from lowest to highest value,
 * if you happen, for some bizarre reason to be interested in the lower-weight values.
 * @author Assaf Urieli
 *
 */
public class WeightedOutcomeAscendingComparator<T> implements Comparator<WeightedOutcome<T>> {

	@Override
	public int compare(WeightedOutcome<T> o1, WeightedOutcome<T> o2) {
		if (o1.getWeight()<o2.getWeight()) {
			return -1;
		} else if (o1.getWeight()>o2.getWeight()) {
			return 1;
		} else {
			int nameCompare = o1.getOutcome().toString().compareTo(o2.getOutcome().toString());
			if (nameCompare!=0) return nameCompare;
			return o1.hashCode()-o2.hashCode();
		}
	}

}
