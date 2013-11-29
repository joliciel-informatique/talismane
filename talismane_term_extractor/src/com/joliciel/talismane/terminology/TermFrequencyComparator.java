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
package com.joliciel.talismane.terminology;

import java.util.Comparator;

public class TermFrequencyComparator implements Comparator<Term> {

	@Override
	public int compare(Term o1, Term o2) {
		if (o1==o2)
			return 0;
		if (o1.equals(o2))
			return 0;
		if (o1.getFrequency()!=o2.getFrequency()) {
			return o2.getFrequency() - o1.getFrequency();
		}
		if (!o1.getText().equals(o2.getText())) {
			return o1.getText().compareTo(o2.getText());
		}
		return 1;
	}

}
