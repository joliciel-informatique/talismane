package com.joliciel.talismane.utils.stats;

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
