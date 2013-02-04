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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

class DecisionImpl<T extends Outcome> implements Decision<T> {
	private static final DecimalFormat df = new DecimalFormat("0.0000");
	private T outcome;
	private String name;
	private double probability;
	private double probabilityLog;
	private boolean probabilityLogCalculated = false;
	private List<String> authorities = new ArrayList<String>();
	private boolean statistical = true;
	
	public DecisionImpl(String name, double probability) {
		this.name = name;
		this.probability = probability;
		this.statistical = true;
	}
	
	public DecisionImpl(T outcome) {
		this.outcome = outcome;
		this.name = outcome.getCode();
		this.probability = 1.0;
		this.statistical = false;
	}
	
	public String getCode() {
		return name;
	}

	public void setProbability(double value) {
		this.probability = value;
		this.probabilityLogCalculated = false;
	}

	/**
	 * Convert the name to an outcome of type T.
	 * @return
	 */
	public T getOutcome() {
		return outcome;
	}

	void setOutcome(T outcome) {
		this.outcome = outcome;
	}

	public double getProbability() {
		return probability;
	}
	
	public double getProbabilityLog() {
		if (!probabilityLogCalculated) {
			probabilityLog = Math.log(probability);
			probabilityLogCalculated = true;
		}
		return probabilityLog;
	}


	@Override
	public int compareTo(Decision<T> o) {
		if (this.getProbability()<o.getProbability()) {
			return 1;
		} else if (this.getProbability()>o.getProbability()) {
			return -1;
		} else {
			int nameCompare = this.getCode().compareTo(o.getCode().toString());
			if (nameCompare!=0) return nameCompare;
			return this.hashCode()-o.hashCode();
		}
	}

	@Override
	public List<String> getAuthorities() {
		return this.authorities;
	}

	@Override
	public void addAuthority(String authority) {
		this.authorities.add(authority);
	}

	public boolean isStatistical() {
		return statistical;
	}

	void setStatistical(boolean statistical) {
		this.statistical = statistical;
	}

	@Override
	public String toString() {
		return "Decision [" + name
				+ "," + df.format(probability) + "]";
	}
	
	
}
