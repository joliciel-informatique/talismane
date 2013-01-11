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
package com.joliciel.talismane.output;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;
import com.joliciel.talismane.utils.LogUtils;

import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Processes output by writing via a freemarker template.
 * @author Assaf Urieli
 *
 */
public class FreemarkerTemplateWriter implements ParseConfigurationProcessor, PosTagSequenceProcessor, TokenSequenceProcessor, SentenceProcessor {
	private static final Log LOG = LogFactory.getLog(FreemarkerTemplateWriter.class);
	private Writer writer;
	private Template template;
	
	public FreemarkerTemplateWriter(Writer writer, Reader templateReader) {
		super();
		try {
			this.writer = writer;
			Configuration cfg = new Configuration();
			cfg.setCacheStorage(new NullCacheStorage());
			cfg.setObjectWrapper(new DefaultObjectWrapper());
	
			this.template = new Template("freemarkerTemplate", templateReader, cfg);
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	void process(Map<String,Object> model) {
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
	public void onNextParseConfiguration(ParseConfiguration parseConfiguration) {
		Map<String,Object> model = new HashMap<String, Object>();
		ParseConfigurationOutput output = new ParseConfigurationOutput(parseConfiguration);
		model.put("sentence", output);
		model.put("configuration", parseConfiguration);
		model.put("LOG", LOG);
		this.process(model);
	}
	
	@Override
	public void onNextPosTagSequence(PosTagSequence posTagSequence) {
		Map<String,Object> model = new HashMap<String, Object>();
		model.put("sentence", posTagSequence);
		model.put("LOG", LOG);
		this.process(model);
	}

	@Override
	public void onNextTokenSequence(TokenSequence tokenSequence) {
		Map<String,Object> model = new HashMap<String, Object>();
		model.put("sentence", tokenSequence);
		model.put("LOG", LOG);
		this.process(model);
	}

	@Override
	public void process(String sentence) {
		Map<String,Object> model = new HashMap<String, Object>();
		model.put("sentence", sentence);
		model.put("LOG", LOG);
		this.process(model);
	}

	public static final class ParseConfigurationOutput extends ArrayList<ParseConfigurationTokenOutput> {

		/**
		 * 
		 */
		private static final long serialVersionUID = 8874423911960774024L;

		public ParseConfigurationOutput(ParseConfiguration parseConfiguration) {
			Map<Token, DependencyArc> tokenDependencyMap = new HashMap<Token, DependencyArc>();
			for (DependencyArc dependencyArc : parseConfiguration.getDependencies()) {
				tokenDependencyMap.put(dependencyArc.getDependent().getToken(), dependencyArc);
			}

			Map<Token, ParseConfigurationTokenOutput> tokenOutputMap = new HashMap<Token, ParseConfigurationTokenOutput>();

			for (PosTaggedToken posTaggedToken : parseConfiguration.getPosTagSequence()) {
				ParseConfigurationTokenOutput unit = new ParseConfigurationTokenOutput(posTaggedToken);
				tokenOutputMap.put(posTaggedToken.getToken(), unit);
				this.add(unit);
			}
			
			for (ParseConfigurationTokenOutput unit : this) {
				DependencyArc arc = tokenDependencyMap.get(unit.getToken());
				if (arc!=null) {
					ParseConfigurationTokenOutput governorOutput = tokenOutputMap.get(arc.getHead().getToken());
					unit.setGovernor(governorOutput);
					unit.setLabel(arc.getLabel());
				}
			}
		}
	}
	public static final class ParseConfigurationTokenOutput {
		private PosTaggedToken posTaggedToken;
		private Token token;
		private PosTag tag;
		private LexicalEntry lexicalEntry;
		private ParseConfigurationTokenOutput governor;
		private String label;
		
		public ParseConfigurationTokenOutput(PosTaggedToken posTaggedToken) {
			this.posTaggedToken = posTaggedToken;
			this.token = posTaggedToken.getToken();
			this.tag = posTaggedToken.getTag();
			this.lexicalEntry = posTaggedToken.getLexicalEntry();
		}
		
		public PosTaggedToken getPosTaggedToken() {
			return posTaggedToken;
		}

		public Token getToken() {
			return token;
		}
		
		public PosTag getTag() {
			return tag;
		}
		
		public LexicalEntry getLexicalEntry() {
			return lexicalEntry;
		}

		public ParseConfigurationTokenOutput getGovernor() {
			return governor;
		}
		public void setGovernor(ParseConfigurationTokenOutput governor) {
			this.governor = governor;
		}

		public String getLabel() {
			return label;
		}
		public void setLabel(String label) {
			this.label = label;
		}
	}

	@Override
	public void onCompleteParse() {
		// nothing to do here
	}
}
