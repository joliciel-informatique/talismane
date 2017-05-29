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
package com.joliciel.talismane.extensions.standoff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseConfigurationOutput;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.utils.LogUtils;

import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

public class StandoffWriter implements ParseConfigurationProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(StandoffWriter.class);
  private final Template template;
  private final Writer writer;
  private int sentenceCount = 0;
  private int tokenCount = 0;
  private int relationCount = 0;
  private int characterCount = 0;

  private final String punctuationDepLabel;

  public StandoffWriter(Writer writer, TalismaneSession session) throws IOException {
    punctuationDepLabel = session.getTransitionSystem().getDependencyLabelSet().getPunctuationLabel();
    this.writer = writer;
    Configuration cfg = new Configuration(new Version(2, 3, 23));
    cfg.setCacheStorage(new NullCacheStorage());
    cfg.setObjectWrapper(new DefaultObjectWrapper(new Version(2, 3, 23)));
    InputStream inputStream = StandoffWriter.class.getResourceAsStream("standoff.ftl");
    Reader templateReader = new BufferedReader(new InputStreamReader(inputStream));

    this.template = new Template("freemarkerTemplate", templateReader, cfg);
  }

  @Override
  public void onNextParseConfiguration(ParseConfiguration parseConfiguration) throws IOException {
    Map<String, Object> model = new HashMap<String, Object>();
    ParseConfigurationOutput output = new ParseConfigurationOutput(parseConfiguration);
    model.put("sentence", output);
    model.put("configuration", parseConfiguration);
    model.put("tokenCount", tokenCount);
    model.put("relationCount", relationCount);
    model.put("sentenceCount", sentenceCount);
    model.put("characterCount", characterCount);
    model.put("LOG", LOG);
    List<DependencyArc> dependencies = new ArrayList<DependencyArc>();
    for (DependencyArc dependencyArc : parseConfiguration.getRealDependencies()) {
      if (!dependencyArc.getLabel().equals(punctuationDepLabel)) {
        dependencies.add(dependencyArc);
      }
    }
    model.put("dependencies", dependencies);
    this.process(model, writer);
    tokenCount += parseConfiguration.getPosTagSequence().size();

    relationCount += dependencies.size();
    characterCount += parseConfiguration.getSentence().getText().length();
    sentenceCount += 1;
  }

  void process(Map<String, Object> model, Writer writer) throws IOException {
    try {
      template.process(model, writer);
      writer.flush();
    } catch (TemplateException te) {
      LogUtils.logError(LOG, te);
      throw new RuntimeException(te);
    }
  }

  @Override
  public void onCompleteParse() {
  }

  @Override
  public void close() throws IOException {
    this.writer.close();
  }

}
