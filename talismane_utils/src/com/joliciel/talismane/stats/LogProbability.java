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
package com.joliciel.talismane.stats;

/**
 * Stores a probability and gives access to its log.
 * @author Assaf Urieli
 *
 */
public class LogProbability implements Probability {
	private double value = 0.0;
	private Double log = null;
	
	public LogProbability(double value) {
		this.value = value;
	}
	
	@Override
	public double getLog() {
		if (this.log==null) {
			this.log = Math.log(this.value);
		}
		return this.log.doubleValue();
	}

	@Override
	public double getValue() {
		return this.value;
	}

}
