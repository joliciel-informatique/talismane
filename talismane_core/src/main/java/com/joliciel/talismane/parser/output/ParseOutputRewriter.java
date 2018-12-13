package com.joliciel.talismane.parser.output;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.Talismane.BuiltInTemplate;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.corpus.CorpusLine;
import com.joliciel.talismane.corpus.CorpusLine.CorpusElement;
import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.utils.ConfigUtils;
import com.joliciel.talismane.utils.LogUtils;
import com.typesafe.config.Config;

import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

public class ParseOutputRewriter implements ParseConfigurationProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(ParseOutputRewriter.class);
  private final Template template;
  private final Writer writer;

  private static final Pattern linePattern = Pattern.compile("\\$\\{line(\\d+)\\}");
  private final List<RewriteRule> rewriteRules;

  private static final class RewriteRule {
    private final Map<CorpusElement, Pattern> conditions;
    private final Action action;

    public RewriteRule(Map<CorpusElement, Pattern> conditions, Action action) {
      super();
      this.conditions = conditions;
      this.action = action;
    }

  }

  public ParseOutputRewriter(File outDir, TalismaneSession session) throws IOException, TalismaneException {
    this(new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(new File(outDir, session.getBaseName() + "_dep.txt"), false), session.getOutputCharset())), session);
  }

  public ParseOutputRewriter(Writer writer, TalismaneSession session) throws IOException, TalismaneException {
    Config config = session.getConfig();
    Config parserConfig = config.getConfig("talismane.core." + session.getId() + ".parser");
    List<? extends Config> ruleConfigs = parserConfig.getConfigList("output.rewrite-rules");
    rewriteRules = new ArrayList<>();
    for (Config rewriteConfig : ruleConfigs) {
      Map<CorpusElement, Pattern> conditions = new HashMap<>();
      for (String corpusElementName : rewriteConfig.getObject("conditions").keySet()) {
        CorpusElement corpusElement = CorpusElement.valueOf(corpusElementName);
        Pattern pattern = Pattern.compile(rewriteConfig.getString("conditions." + corpusElementName));
        conditions.put(corpusElement, pattern);
      }
      Config actionConfig = rewriteConfig.getConfig("action");
      String actionType = actionConfig.getString("type");
      Action action = null;
      if (actionType.equals("split")) {
        action = new SplitAction(actionConfig);
      } else {
        throw new TalismaneException("Unknown action type: " + actionType);
      }
      RewriteRule rewriteRule = new RewriteRule(conditions, action);
      rewriteRules.add(rewriteRule);
    }

    this.writer = writer;

    Reader templateReader = null;
    String configPath = "talismane.core." + session.getId() + ".parser.output.template";
    if (config.hasPath(configPath)) {
      templateReader = new BufferedReader(new InputStreamReader(ConfigUtils.getFileFromConfig(config, configPath)));
    } else {
      String templateName = null;
      BuiltInTemplate builtInTemplate = BuiltInTemplate.valueOf(parserConfig.getString("output.built-in-template"));
      switch (builtInTemplate) {
      case standard:
        templateName = "parser_conll_corpus_line_template.ftl";
        break;
      default:
        throw new RuntimeException("Unknown builtInTemplate for parser: " + builtInTemplate.name());
      }

      String path = "output/" + templateName;
      InputStream inputStream = Talismane.class.getResourceAsStream(path);
      if (inputStream == null)
        throw new IOException("Resource not found in classpath: " + path);
      templateReader = new BufferedReader(new InputStreamReader(inputStream));
    }

    Configuration cfg = new Configuration(new Version(2, 3, 23));
    cfg.setCacheStorage(new NullCacheStorage());
    cfg.setObjectWrapper(new DefaultObjectWrapper(new Version(2, 3, 23)));
    this.template = new Template("freemarkerTemplate", templateReader, cfg);
  }

  private interface Action {
  }

  private static final class ConditionalAction {
    private final Map<CorpusElement, Pattern> conditions = new HashMap<>();
    private final int relativeIndex;
    private final Map<CorpusElement, String> elementValues = new HashMap<>();
    private final boolean isDefault;

    public ConditionalAction(Config config) {
      if (config.hasPath("relative-index"))
        relativeIndex = config.getInt("relative-index");
      else
        relativeIndex = 0;

      if (config.hasPath("default"))
        isDefault = config.getBoolean("default");
      else
        isDefault = false;

      for (String innerElementName : config.getObject("results").keySet()) {
        CorpusElement innerElement = CorpusElement.valueOf(innerElementName);
        elementValues.put(innerElement, config.getString("results." + innerElementName));
      }

      for (String key : config.root().keySet()) {
        try {
          CorpusElement corpusElement = CorpusElement.valueOf(key);
          Pattern pattern = Pattern.compile(config.getString(key));
          conditions.put(corpusElement, pattern);
        } catch (IllegalArgumentException iae) {
          // do nothing
        }
      }
    }
  }

  private static final class SplitAction implements Action {
    private final List<Map<CorpusElement, String>> elementValues = new ArrayList<>();
    private final List<List<ConditionalAction>> conditionalValues = new ArrayList<>();

    public SplitAction(Config config) {
      int i = 1;
      while (config.hasPath("line" + i)) {
        Map<CorpusElement, String> myElementValues = new HashMap<>();
        List<ConditionalAction> myConditionalActions = new ArrayList<>();
        elementValues.add(myElementValues);
        conditionalValues.add(myConditionalActions);
        for (String corpusElementName : config.getObject("line" + i).keySet()) {
          if (corpusElementName.equals("conditional")) {
            for (Config conditionalConfig : config.getConfigList("line" + i + ".conditional")) {
              ConditionalAction conditionalAction = new ConditionalAction(conditionalConfig);
              myConditionalActions.add(conditionalAction);
            }
          } else {
            CorpusElement corpusElement = CorpusElement.valueOf(corpusElementName);
            myElementValues.put(corpusElement, config.getString("line" + i + "." + corpusElementName));
          }
        }
        i += 1;
      }
    }

  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public void onNextParseConfiguration(ParseConfiguration parseConfiguration) throws TalismaneException, IOException {
    List<CorpusLine> corpusLines = this.getCorpusLines(parseConfiguration);
    Map<String, Object> model = new HashMap<>();
    model.put("sentence", corpusLines);
    model.put("LOG", LOG);

    try {
      template.process(model, writer);
      writer.flush();
    } catch (TemplateException te) {
      LogUtils.logError(LOG, te);
      throw new RuntimeException(te);
    }
  }

  List<CorpusLine> getCorpusLines(ParseConfiguration parseConfiguration) throws TalismaneException {
    // first convert the parse configuration to a list of corpus lines
    List<CorpusLine> corpusLines = new ArrayList<>();
    for (PosTaggedToken posTaggedToken : parseConfiguration.getPosTagSequence()) {
      if (!posTaggedToken.isRoot()) {
        DependencyArc arc = parseConfiguration.getGoverningDependency(posTaggedToken);
        DependencyArc nonProjArc = parseConfiguration.getGoverningDependency(posTaggedToken, false);
        String line = posTaggedToken.getIndex() + "\t" + posTaggedToken.getToken().getOriginalText() + "\t" + posTaggedToken.getLemmaForCoNLL() + "\t"
            + posTaggedToken.getTag().getCode() + "\t" + posTaggedToken.getTag().getCode() + "\t" + posTaggedToken.getMorphologyForCoNLL() + "\t"
            + (arc != null ? arc.getHead().getIndex() : 0) + "\t" + (arc != null ? arc.getLabel() : "_");

        CorpusLine corpusLine = new CorpusLine(line, posTaggedToken.getToken().getLineNumber());
        corpusLine.setIndex(posTaggedToken.getIndex());
        corpusLine.setToken(posTaggedToken.getToken().getOriginalText());
        corpusLine.setLemma(posTaggedToken.getLemmaForCoNLL());
        corpusLine.setPosTag(posTaggedToken.getTag().getCode());
        String morphology = posTaggedToken.getMorphologyForCoNLL();
        corpusLine.setMorphology(morphology.length() == 0 ? "_" : morphology);
        corpusLine.setGovernorIndex(arc != null ? arc.getHead().getIndex() : 0);
        corpusLine.setLabel(arc != null ? arc.getLabel() : "_");
        corpusLine.setNonProjGovernorIndex(nonProjArc != null ? nonProjArc.getHead().getIndex() : 0);
        corpusLine.setNonProjLabel(nonProjArc != null ? nonProjArc.getLabel() : "_");
        if (posTaggedToken.getToken().getPrecedingRawOutput() != null)
          corpusLine.setElement(CorpusElement.PRECEDING_RAW_OUTPUT, posTaggedToken.getToken().getPrecedingRawOutput());
        if (posTaggedToken.getToken().getTrailingRawOutput() != null)
          corpusLine.setElement(CorpusElement.TRAILING_RAW_OUTPUT, posTaggedToken.getToken().getTrailingRawOutput());
        corpusLine.setTokenProbability(posTaggedToken.getToken().getProbability());
        corpusLine.setPosTagProbability(posTaggedToken.getProbability());
        if (arc != null)
          corpusLine.setParseProbability(arc.getProbability());
        corpusLines.add(corpusLine);
      }
    }

    Map<CorpusLine, SplitAction> splitActions = new HashMap<>();
    for (CorpusLine corpusLine : corpusLines) {
      if (LOG.isDebugEnabled())
        LOG.debug(corpusLine.toString());
      for (RewriteRule rewriteRule : rewriteRules) {
        boolean matches = true;
        conditionLoop: for (CorpusElement corpusElement : rewriteRule.conditions.keySet()) {
          Pattern pattern = rewriteRule.conditions.get(corpusElement);
          if (LOG.isTraceEnabled())
            LOG.trace("For " + corpusElement.name() + ", matching " + pattern.pattern());
          switch (corpusElement) {
          case POSTAG:
            if (!pattern.matcher(corpusLine.getPosTag()).matches()) {
              if (LOG.isTraceEnabled())
                LOG.trace("Match failed for " + corpusLine.getPosTag());
              matches = false;
              break conditionLoop;
            }
            break;
          case TOKEN:
            if (!pattern.matcher(corpusLine.getToken()).matches()) {
              matches = false;
              break conditionLoop;
            }
            break;
          case LEMMA:
            if (!pattern.matcher(corpusLine.getLemma()).matches()) {
              matches = false;
              break conditionLoop;
            }
            break;
          case LABEL:
            if (!pattern.matcher(corpusLine.getLabel()).matches()) {
              matches = false;
              break conditionLoop;
            }
            break;
          default:
            throw new TalismaneException(ParseOutputRewriter.class.getSimpleName() + " cannot match on " + corpusElement.name());
          }
        }
        if (matches) {
          if (rewriteRule.action instanceof SplitAction) {
            SplitAction splitAction = (SplitAction) rewriteRule.action;
            splitActions.put(corpusLine, splitAction);
          }
        }
      }
    }

    if (splitActions.size() > 0) {
      List<CorpusLine> newCorpusLines = new ArrayList<>();
      Map<Integer, Integer> oldToNewIndexMap = new HashMap<>();
      oldToNewIndexMap.put(0, 0);
      int currentIndex = 1;
      for (int i = 0; i < corpusLines.size(); i++) {
        CorpusLine corpusLine = corpusLines.get(i);
        oldToNewIndexMap.put(i + 1, currentIndex);
        if (splitActions.containsKey(corpusLine)) {
          SplitAction splitAction = splitActions.get(corpusLine);
          currentIndex += splitAction.elementValues.size();
        } else {
          currentIndex++;
        }
      }

      for (int i = 0; i < corpusLines.size(); i++) {
        CorpusLine corpusLine = corpusLines.get(i);
        CorpusLine newCorpusLine = corpusLine.cloneCorpusLine();
        newCorpusLine.setIndex(oldToNewIndexMap.get(corpusLine.getIndex()));
        newCorpusLine.setGovernorIndex(oldToNewIndexMap.get(corpusLine.getGovernorIndex()));
        newCorpusLine.setNonProjGovernorIndex(oldToNewIndexMap.get(corpusLine.getNonProjGovernorIndex()));
        if (splitActions.containsKey(corpusLine)) {
          SplitAction splitAction = splitActions.get(corpusLine);
          for (int j = 0; j < splitAction.elementValues.size(); j++) {
            CorpusLine splitCorpusLine = new CorpusLine(corpusLine.getLine(), corpusLine.getLineNumber());
            splitCorpusLine.setIndex(oldToNewIndexMap.get(corpusLine.getIndex()) + j);
            Map<CorpusElement, String> elementValues = splitAction.elementValues.get(j);
            this.setElementValues(elementValues, oldToNewIndexMap, newCorpusLine, splitCorpusLine);

            // The first matching element in each group will be applied
            // The default element marks the end of each group, and will be
            // applied if no other match has applied.
            List<ConditionalAction> conditionalActions = splitAction.conditionalValues.get(j);
            boolean groupHasMatch = false;
            for (ConditionalAction conditionalAction : conditionalActions) {
              CorpusLine baseLine = corpusLines.get(i + conditionalAction.relativeIndex);

              if (conditionalAction.isDefault) {
                if (!groupHasMatch) {
                  Map<CorpusElement, String> conditionalElementValues = conditionalAction.elementValues;
                  this.setElementValues(conditionalElementValues, oldToNewIndexMap, newCorpusLine, splitCorpusLine);
                }
                // The default action marks the end of each matching group.
                groupHasMatch = false;
              } else {
                boolean match = true;
                for (CorpusElement corpusElement : conditionalAction.conditions.keySet()) {
                  String origValue = baseLine.getElement(corpusElement);
                  Pattern pattern = conditionalAction.conditions.get(corpusElement);
                  if (!pattern.matcher(origValue).matches()) {
                    match = false;
                    break;
                  }
                }
                if (match) {
                  Map<CorpusElement, String> conditionalElementValues = conditionalAction.elementValues;
                  this.setElementValues(conditionalElementValues, oldToNewIndexMap, newCorpusLine, splitCorpusLine);
                  groupHasMatch = true;
                } // did this action match?
              } // default action?
            } // next conditional action

            newCorpusLines.add(splitCorpusLine);
          } // next split
        } else {
          newCorpusLines.add(newCorpusLine);
        } // should line be split?
      } // next corpus line
      corpusLines = newCorpusLines;
    } // have split actions?

    return corpusLines;
  }

  private void setElementValues(Map<CorpusElement, String> elementValues, Map<Integer, Integer> oldToNewIndexMap, CorpusLine origLine,
      CorpusLine splitCorpusLine) throws TalismaneException {
    for (CorpusElement key : elementValues.keySet()) {
      String elementValue = elementValues.get(key);
      if (elementValue.equals("${orig}")) {
        splitCorpusLine.setElement(key, origLine.getElement(key));
      } else {
        Matcher matcher = linePattern.matcher(elementValue);
        if (matcher.matches()) {
          int lineNumber = Integer.parseInt(matcher.group(1));
          int equivalentIndex = (origLine.getIndex() + lineNumber) - 1;
          switch (key) {
          case GOVERNOR:
            splitCorpusLine.setGovernorIndex(equivalentIndex);
            break;
          case NON_PROJ_GOVERNOR:
            splitCorpusLine.setNonProjGovernorIndex(equivalentIndex);
            break;
          default:
            throw new TalismaneException("element value '" + elementValue + "' not supported for corpus element " + key.name());
          }
        } else {
          splitCorpusLine.setElement(key, elementValue);
        }
      }
    }
  }

  @Override
  public void onCompleteParse() throws IOException {
  }

}
