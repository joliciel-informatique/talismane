package com.joliciel.talismane.parser;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.joliciel.talismane.posTagger.PosTagSequence;

public class ParseTimeByLengthObserver implements ParseEvaluationObserver {
	private static final Log LOG = LogFactory.getLog(ParseTimeByLengthObserver.class);
	private Map<Integer, DescriptiveStatistics> timeStatsPerLength = new TreeMap<Integer, DescriptiveStatistics>();

	private long startTime;
	@Override
	public void onParseStart(ParseConfiguration realConfiguration,
			List<PosTagSequence> posTagSequences) {
		startTime = System.currentTimeMillis();
	}

	@Override
	public void onParseEnd(ParseConfiguration realConfiguration,
			List<ParseConfiguration> guessedConfigurations) {
		long totalTime = System.currentTimeMillis() - startTime;
		int length = realConfiguration.getPosTagSequence().size();
		DescriptiveStatistics stats = timeStatsPerLength.get(length);
		if (stats==null) {
			stats = new DescriptiveStatistics();
			timeStatsPerLength.put(length, stats);
		}
		stats.addValue(totalTime);
	}

	@Override
	public void onEvaluationComplete() {
		LOG.info("##################");
		LOG.info("timeStatsPerLength");
		LOG.info("\tlength\tcount\tmean\tperToken");
		DecimalFormat df = new DecimalFormat("0.00");
		for (int length : timeStatsPerLength.keySet()) {
			DescriptiveStatistics stats = timeStatsPerLength.get(length);
			long count = stats.getN();
			double mean = stats.getMean();
			double perToken = mean / length;
			LOG.info("\t" + length + "\t" + count + "\t" + df.format(mean) + "\t" + df.format(perToken));
		}
		LOG.info("##################");
	}
	
	/**
	 * Total parsing time statistics for sentences of different lengths.
	 * @return
	 */

	public Map<Integer, DescriptiveStatistics> getTimeStatsPerLength() {
		return timeStatsPerLength;
	}
}
