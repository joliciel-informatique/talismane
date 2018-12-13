package com.joliciel.talismane.corpus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.corpus.CorpusLine.CorpusElement;
import com.typesafe.config.Config;

public abstract class CorpusSentenceRule {
  private static final Logger LOG = LoggerFactory.getLogger(CorpusSentenceRule.class);

  protected final Pattern pattern;

  public CorpusSentenceRule(Config config) {
    this.pattern = Pattern.compile(config.getString("pattern"));
  }

  public Pattern getPattern() {
    return pattern;
  }

  public interface Action {
  }

  /**
   * Return an action to apply for this rule, assuming the rule matched a given
   * line.
   * 
   * @param matchingLine
   *          the line matched
   * @param lineNumber
   *          the line number of the matched line
   * @param matcher
   *          the matcher which matched the line
   * @param corpusLines
   *          the full set of corpus lines to which we will apply the action.
   * @return the action to apply, or null if no action is to be applied.
   */
  public abstract Action apply(String matchingLine, int lineNumber, Matcher matcher, List<CorpusLine> corpusLines);

  public static CorpusSentenceRule getRule(Config config) throws TalismaneException {
    String action = config.getString("action");
    if (action.startsWith("merge")) {
      return new MergeSentenceRule(config);
    }
    throw new TalismaneException("Unknown rule type: " + action);
  }

  public static final class MergeAction implements Action {
    private final CorpusLine mergedLine;
    private final List<CorpusLine> linesToMerge;
    private final int firstIndex;

    public MergeAction(CorpusLine mergedLine, List<CorpusLine> linesToMerge) {
      this.mergedLine = mergedLine;
      this.linesToMerge = linesToMerge;
      this.firstIndex = linesToMerge.get(0).getIndex();
    }

    public CorpusLine getMergedLine() {
      return mergedLine;
    }

    public List<CorpusLine> getLinesToMerge() {
      return linesToMerge;
    }

    public int getFirstIndex() {
      return firstIndex;
    }

    @Override
    public String toString() {
      return "MergeAction [mergedLine=" + mergedLine + ", linesToMerge=" + linesToMerge + ", firstIndex=" + firstIndex + "]";
    }
  }

  private static final class MergeSentenceRule extends CorpusSentenceRule {
    private final String[] linePlaceholders;
    private final List<Map<CorpusElement, Pattern>> conditions;
    private final Map<CorpusElement, String> results;
    private final Pattern placeholderPattern = Pattern.compile("\\$(\\d+)");
    private final Pattern linePattern = Pattern.compile("\\$\\{line(\\d+)\\}");

    public MergeSentenceRule(Config config) {
      super(config);
      String action = config.getString("action");
      linePlaceholders = action.substring("merge ".length()).split(" ");
      Map<Integer, Map<CorpusElement, Pattern>> conditionMap = new TreeMap<>();
      for (String conditionKey : config.getObject("conditions").keySet()) {
        int lineNumber = Integer.parseInt(conditionKey.substring("line".length()));
        Map<CorpusElement, Pattern> lineConditions = new HashMap<>();
        conditionMap.put(lineNumber, lineConditions);
        for (String corpusElementName : config.getObject("conditions." + conditionKey).keySet()) {
          CorpusElement corpusElement = CorpusElement.valueOf(corpusElementName);
          Pattern pattern = Pattern.compile(config.getString("conditions." + conditionKey + "." + corpusElementName));
          lineConditions.put(corpusElement, pattern);
        }
      }

      conditions = new ArrayList<>();
      for (int i = 0; i < linePlaceholders.length; i++) {
        if (conditionMap.containsKey(i + 1)) {
          conditions.add(conditionMap.get(i + 1));
        } else {
          conditions.add(Collections.emptyMap());
        }
      }

      results = new HashMap<>();
      for (String corpusElementName : config.getObject("result").keySet()) {
        CorpusElement corpusElement = CorpusElement.valueOf(corpusElementName);
        String value = config.getString("result." + corpusElementName);
        results.put(corpusElement, value);
      }
    }

    @Override
    public Action apply(String matchingLine, int lineNumber, Matcher matcher, List<CorpusLine> corpusLines) {
      Map<Integer, Integer> lineIndexes = new HashMap<>();

      int i = 1;
      for (String linePlaceholder : linePlaceholders) {
        if (linePlaceholder.startsWith("$")) {
          int match = Integer.parseInt(linePlaceholder.substring(1));
          int lineIndex = Integer.parseInt(matcher.group(match));
          lineIndexes.put(i, lineIndex);
        }
        i++;
      }

      List<CorpusLine> linesToMerge = new ArrayList<>();
      for (CorpusLine corpusLine : corpusLines) {
        for (Entry<Integer, Integer> lineEntry : lineIndexes.entrySet()) {
          if (corpusLine.getIndex() == lineEntry.getValue()) {
            linesToMerge.add(corpusLine);
            if (LOG.isTraceEnabled())
              LOG.trace("Line to merge: " + corpusLine);
            break;
          }
        }
      }

      // test the conditions
      boolean conditionsMet = true;
      conditionLoop: for (int j = 0; j < linesToMerge.size(); j++) {
        Map<CorpusElement, Pattern> conditionMap = conditions.get(j);
        CorpusLine corpusLine = linesToMerge.get(j);
        if (conditionMap.size() == 0)
          continue;
        for (CorpusElement corpusElement : conditionMap.keySet()) {
          Pattern pattern = conditionMap.get(corpusElement);
          if (LOG.isTraceEnabled())
            LOG.trace("Testing line " + (j + 1) + ", corpusElement: " + corpusElement + ", pattern: " + pattern.pattern());
          if (!pattern.matcher(corpusLine.getElement(corpusElement)).matches()) {
            if (LOG.isTraceEnabled())
              LOG.trace("Test failed");
            conditionsMet = false;
            break conditionLoop;
          }
          if (LOG.isTraceEnabled())
            LOG.trace("Test passed");
        }
      }

      if (!conditionsMet)
        return null;

      // build the merged line
      CorpusLine mergedLine = new CorpusLine(matchingLine, lineNumber);
      mergedLine.setElement(CorpusElement.INDEX, linesToMerge.get(1).getElement(CorpusElement.INDEX));
      for (CorpusElement corpusElement : results.keySet()) {
        String corpusElementValue = results.get(corpusElement);
        Matcher placeholderMatcher = placeholderPattern.matcher(corpusElementValue);
        if (placeholderMatcher.matches()) {
          int placeholder = Integer.parseInt(placeholderMatcher.group(1));
          String value = matcher.group(placeholder);
          mergedLine.setElement(corpusElement, value);
        } else {
          Matcher lineMatcher = linePattern.matcher(corpusElementValue);
          if (lineMatcher.matches()) {
            int lineIndex = Integer.parseInt(lineMatcher.group(1));
            CorpusLine corpusLine = linesToMerge.get(lineIndex - 1);
            mergedLine.setElement(corpusElement, corpusLine.getElement(corpusElement));
          } else {
            mergedLine.setElement(corpusElement, corpusElementValue);
          }
        }
      }

      if (LOG.isTraceEnabled())
        LOG.trace("Merged line: " + mergedLine.getElements());

      return new MergeAction(mergedLine, linesToMerge);
    }

    @Override
    public String toString() {
      return "MergeSentenceRule [linePlaceholders=" + Arrays.toString(linePlaceholders) + ", conditions=" + conditions + ", results=" + results + ", pattern="
          + pattern + "]";
    }
  }
}
