package com.joliciel.talismane.terminology;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.utils.LogUtils;

public class TermAnalysisWriter implements TermObserver {
	private static final Log LOG = LogFactory.getLog(TermAnalysisWriter.class);
	private Writer writer;
	
	
	public TermAnalysisWriter(Writer writer) {
		super();
		this.writer = writer;
	}

	@Override
	public void onNewContext(String context) {
		try {
			writer.write("\n#### Sentence: " + context + "\n");
			writer.flush();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onNewTerm(Term term) {
		try {
			writer.write(term.getText() + "\n");
			writer.flush();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

}
