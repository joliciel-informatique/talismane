///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
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

class DecisionImpl implements Decision {
	private static final DecimalFormat df = new DecimalFormat("0.0000");
	private String outcome;
	private double score;
	private double probability;
	private double probabilityLog;
	private boolean probabilityLogCalculated = false;
	private List<String> authorities = new ArrayList<String>();
	private boolean statistical = true;
	
	public DecisionImpl(String outcome, double probability) {
		this.outcome = outcome;
		this.probability = probability;
		this.statistical = true;
	}
	
	public DecisionImpl(String outcome) {
		this.outcome = outcome;
		this.probability = 1.0;
		this.statistical = false;
	}
	
	public void setProbability(double value) {
		this.probability = value;
		this.probabilityLogCalculated = false;
	}

	public String getOutcome() {
		return outcome;
	}

	void setOutcome(String outcome) {
		this.outcome = outcome;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
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
	public int compareTo(Decision o) {
		if (this.getProbability()<o.getProbability()) {
			return 1;
		} else if (this.getProbability()>o.getProbability()) {
			return -1;
		} else {
			int nameCompare = this.getOutcome().compareTo(o.getOutcome());
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
		return "Decision [" + outcome
				+ "," + df.format(probability) + "]";
	}
	
	
}
