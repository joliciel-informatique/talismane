package com.joliciel.talismane.parser;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class ParseConfigurationProcessorChain implements
		ParseConfigurationProcessor {
	List<ParseConfigurationProcessor> processors = new ArrayList<ParseConfigurationProcessor>();
	
	@Override
	public void onNextParseConfiguration(ParseConfiguration parseConfiguration,
			Writer writer) {
		for (ParseConfigurationProcessor processor : processors) {
			processor.onNextParseConfiguration(parseConfiguration, writer);
		}
	}

	@Override
	public void onCompleteParse() {
		for (ParseConfigurationProcessor processor : processors) {
			processor.onCompleteParse();
		}
	}

	public void addProcessor(ParseConfigurationProcessor processor) {
		this.processors.add(processor);
	}
}
