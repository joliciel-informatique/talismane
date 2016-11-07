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
package com.joliciel.talismane.output;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;
import com.joliciel.talismane.utils.LogUtils;

import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

/**
 * Processes output by writing via a freemarker template.
 * 
 * @author Assaf Urieli
 *
 */
public class FreemarkerTemplateWriter implements ParseConfigurationProcessor, PosTagSequenceProcessor, TokenSequenceProcessor, SentenceProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(FreemarkerTemplateWriter.class);
	private Template template;
	private int sentenceCount = 0;
	private int tokenCount = 0;
	private int relationCount = 0;
	private int characterCount = 0;

	public FreemarkerTemplateWriter(Reader templateReader) {
		super();
		try {
			Configuration cfg = new Configuration(new Version(2, 3, 23));
			cfg.setCacheStorage(new NullCacheStorage());
			cfg.setObjectWrapper(new DefaultObjectWrapper(new Version(2, 3, 23)));

			this.template = new Template("freemarkerTemplate", templateReader, cfg);
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

	void process(Map<String, Object> model, Writer writer) {
		try {
			template.process(model, writer);
			writer.flush();
		} catch (TemplateException te) {
			LogUtils.logError(LOG, te);
			throw new RuntimeException(te);
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

	@Override
	public void onNextParseConfiguration(ParseConfiguration parseConfiguration, Writer writer) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("Outputting: " + parseConfiguration.toString());
		}
		Map<String, Object> model = new HashMap<String, Object>();
		ParseConfigurationOutput output = new ParseConfigurationOutput(parseConfiguration);
		model.put("sentence", output);
		model.put("configuration", parseConfiguration);
		model.put("tokenCount", tokenCount);
		model.put("relationCount", relationCount);
		model.put("sentenceCount", sentenceCount);
		model.put("characterCount", characterCount);
		model.put("LOG", LOG);
		this.process(model, writer);
		tokenCount += parseConfiguration.getPosTagSequence().size();
		relationCount += parseConfiguration.getRealDependencies().size();
		characterCount += parseConfiguration.getSentence().getText().length();
		sentenceCount += 1;
	}

	@Override
	public void onNextPosTagSequence(PosTagSequence posTagSequence, Writer writer) {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("sentence", posTagSequence);
		model.put("text", posTagSequence.getTokenSequence().getSentence().getText());
		model.put("LOG", LOG);
		this.process(model, writer);
	}

	@Override
	public void onNextTokenSequence(TokenSequence tokenSequence, Writer writer) {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("sentence", tokenSequence);
		model.put("text", tokenSequence.getSentence().getText());
		model.put("LOG", LOG);
		this.process(model, writer);
	}

	@Override
	public void onNextSentence(Sentence sentence, Writer writer) {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("sentence", sentence);
		model.put("LOG", LOG);
		this.process(model, writer);
	}

	@Override
	public void onCompleteParse() {
		// nothing to do here
	}

	@Override
	public void onCompleteAnalysis() {
		// nothing to do here
	}
}
