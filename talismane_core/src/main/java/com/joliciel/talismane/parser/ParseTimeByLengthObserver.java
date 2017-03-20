package com.joliciel.talismane.parser;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.utils.CSVFormatter;

public class ParseTimeByLengthObserver implements ParseEvaluationObserver {
	private static final Logger LOG = LoggerFactory.getLogger(ParseTimeByLengthObserver.class);
	private static final CSVFormatter CSV = new CSVFormatter();
	private Map<Integer, DescriptiveStatistics> timeStatsPerLength = new TreeMap<Integer, DescriptiveStatistics>();

	private Writer writer = null;

	private long startTime;

	@Override
	public void onParseStart(ParseConfiguration realConfiguration, List<PosTagSequence> posTagSequences) {
		startTime = System.currentTimeMillis();
	}

	@Override
	public void onParseEnd(ParseConfiguration realConfiguration, List<ParseConfiguration> guessedConfigurations) {
		long totalTime = System.currentTimeMillis() - startTime;
		int length = realConfiguration.getPosTagSequence().size();
		DescriptiveStatistics stats = timeStatsPerLength.get(length);
		if (stats == null) {
			stats = new DescriptiveStatistics();
			timeStatsPerLength.put(length, stats);
		}
		stats.addValue(totalTime);
	}

	@Override
	public void onEvaluationComplete() throws IOException {
		LOG.info("##################");
		LOG.info("timeStatsPerLength");
		LOG.info("\tlength\tcount\tmean\tperToken");
		if (writer != null)
			writer.write(CSV.format("length") + CSV.format("count") + CSV.format("mean") + CSV.format("perToken") + "\n");
		DecimalFormat df = new DecimalFormat("0.00");
		for (int length : timeStatsPerLength.keySet()) {
			DescriptiveStatistics stats = timeStatsPerLength.get(length);
			long count = stats.getN();
			double mean = stats.getMean();
			double perToken = mean / length;
			LOG.info("\t" + length + "\t" + count + "\t" + df.format(mean) + "\t" + df.format(perToken));
			if (writer != null)
				writer.write(CSV.format(length) + CSV.format(count) + CSV.format(mean) + CSV.format(perToken) + "\n");
		}
		if (writer != null) {
			writer.flush();
			writer.close();
		}
		LOG.info("##################");
	}

	/**
	 * Total parsing time statistics for sentences of different lengths.
	 */
	public Map<Integer, DescriptiveStatistics> getTimeStatsPerLength() {
		return timeStatsPerLength;
	}

	public Writer getWriter() {
		return writer;
	}

	public void setWriter(Writer writer) {
		this.writer = writer;
	}

}
