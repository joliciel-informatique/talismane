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
package com.joliciel.talismane.tokeniser.output;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.Talismane.BuiltInTemplate;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.utils.ConfigUtils;
import com.joliciel.talismane.utils.LogUtils;
import com.typesafe.config.Config;

import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

/**
 * Processes tokeniser output by writing via a freemarker template.<br/>
 * Will either use the built-in template specified in
 * talismane.core.[sessionId].tokeniser.output.built-in-template, or any other
 * template specified in talismane.core.[sessionId].tokeniser.output.template.
 * <br/>
 * If no writer is specified, will write to a file with the suffix "_tok.txt".
 * 
 * @author Assaf Urieli
 *
 */
public class FreemarkerTokenWriter implements TokenSequenceProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(FreemarkerTokenWriter.class);
  private final Template template;
  private final Writer writer;
  private int sentenceNumber = 0;

  public FreemarkerTokenWriter(File outDir, String sessionId) throws IOException, TalismaneException {
    this(new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(new File(outDir, 
          TalismaneSession.get(sessionId).getBaseName() + "_tok.txt"), false),
          TalismaneSession.get(sessionId).getOutputCharset())), sessionId);
  }

  public FreemarkerTokenWriter(Writer writer, String sessionId) throws IOException, TalismaneException {
    this.writer = writer;

    Config config = ConfigFactory.load();
    Config tokeniserConfig = config.getConfig("talismane.core." + sessionId + ".tokeniser");

    Reader templateReader = null;
    String configPath = "talismane.core." + sessionId + ".tokeniser.output.template";
    if (config.hasPath(configPath)) {
      templateReader = new BufferedReader(new InputStreamReader(ConfigUtils.getFileFromConfig(config, configPath)));
    } else {
      String tokeniserTemplateName = null;
      BuiltInTemplate builtInTemplate = BuiltInTemplate.valueOf(tokeniserConfig.getString("output.built-in-template"));
      switch (builtInTemplate) {
      case standard:
        tokeniserTemplateName = "tokeniser_template.ftl";
        break;
      case with_location:
        tokeniserTemplateName = "tokeniser_template_with_location.ftl";
        break;
      case with_prob:
        tokeniserTemplateName = "tokeniser_template_with_prob.ftl";
        break;
      default:
        throw new TalismaneException("Unknown builtInTemplate for tokeniser: " + builtInTemplate.name());
      }

      String path = "output/" + tokeniserTemplateName;
      InputStream inputStream = Talismane.class.getResourceAsStream(path);
      if (inputStream == null)
        throw new IOException("Resource not found in classpath: " + path);
      templateReader = new BufferedReader(new InputStreamReader(inputStream));
    }

    this.template = this.getTemplate(templateReader);
  }

  public FreemarkerTokenWriter(Reader templateReader, Writer writer, String sessionId) throws IOException, TalismaneException {
    this.writer = writer;
    this.template = this.getTemplate(templateReader);
  }

  private Template getTemplate(Reader templateReader) throws IOException {
    Configuration cfg = new Configuration(new Version(2, 3, 23));
    cfg.setCacheStorage(new NullCacheStorage());
    cfg.setObjectWrapper(new DefaultObjectWrapper(new Version(2, 3, 23)));
    return new Template("freemarkerTemplate", templateReader, cfg);
  }

  void process(Map<String, Object> model) throws IOException {
    try {
      template.process(model, writer);
      writer.flush();
    } catch (TemplateException te) {
      LogUtils.logError(LOG, te);
      throw new RuntimeException(te);
    }
  }

  @Override
  public void onNextTokenSequence(TokenSequence tokenSequence) throws IOException {
    sentenceNumber++;
    Map<String, Object> model = new HashMap<String, Object>();
    model.put("sentence", tokenSequence);
    model.put("sentenceNumber", sentenceNumber);
    model.put("text", tokenSequence.getSentence().getText());
    model.put("LOG", LOG);
    this.process(model);
  }

  @Override
  public void close() throws IOException {
    this.writer.close();
  }
}
