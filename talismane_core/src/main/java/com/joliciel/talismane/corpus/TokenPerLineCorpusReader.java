package com.joliciel.talismane.corpus;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.LinguisticRules;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.corpus.CorpusLine.CorpusElement;
import com.joliciel.talismane.corpus.CorpusSentenceRule.MergeAction;
import com.joliciel.talismane.lexicon.CompactLexicalEntry;
import com.joliciel.talismane.lexicon.CompactLexicalEntrySupport;
import com.joliciel.talismane.lexicon.LexicalEntryReader;
import com.joliciel.talismane.lexicon.RegexLexicalEntryReader;
import com.joliciel.talismane.lexicon.WritableLexicalEntry;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.sentenceAnnotators.SentenceAnnotator;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.sentenceDetector.SentencePerLineCorpusReader;
import com.joliciel.talismane.utils.ConfigUtils;
import com.joliciel.talismane.utils.io.CurrentFileObserver;
import com.typesafe.config.Config;

/**
 * A corpus reader that expects one token per line, and analyses the line
 * content based on a regex supplied during construction, via a
 * {@link CorpusLineReader}.
 * 
 * @author Assaf Urieli
 *
 */

public class TokenPerLineCorpusReader extends AbstractAnnotatedCorpusReader implements CurrentFileObserver {
  private static final Logger LOG = LoggerFactory.getLogger(TokenPerLineCorpusReader.class);

  private int lineNumber = 0;
  private int sentenceCount = 0;

  private final String regex;
  private final Scanner scanner;
  private final CompactLexicalEntrySupport lexicalEntrySupport = new CompactLexicalEntrySupport("");
  private final CorpusLineReader corpusLineReader;
  private File currentFile;

  private final LexicalEntryReader lexicalEntryReader;

  private final SentenceDetectorAnnotatedCorpusReader sentenceReader;

  private boolean needsToReturnBlankLine = false;
  private final List<Pattern> skipLinePatterns;
  private final List<CorpusSentenceRule> sentenceRules;

  private List<CorpusLine> sentenceLines = null;

  /**
   * Add attributes as specified in the config to the corpus reader. Recognises
   * the attributes:<br>
   * - input-pattern: the pattern to match corpus line elements, see class
   * description.<br>
   * - sentence-file: where to read the correctly formatted sentences<br>
   * - corpus-lexical-entry-regex: how to read the lexical entries, see
   * {@link RegexLexicalEntryReader}<br>
   * 
   * @param config
   *          the local config for this corpus reader (local namespace)
   */
  public TokenPerLineCorpusReader(Reader reader, Config config, String sessionId) throws IOException, TalismaneException {
    super(config, sessionId);
    this.regex = config.getString("input-pattern");
    this.scanner = new Scanner(reader);

    String configPath = "sentence-file";
    if (config.hasPath(configPath)) {
      InputStream sentenceReaderFile = ConfigUtils.getFileFromConfig(config, configPath);
      Reader sentenceFileReader = new BufferedReader(new InputStreamReader(sentenceReaderFile, TalismaneSession.get(sessionId).getInputCharset()));
      SentenceDetectorAnnotatedCorpusReader sentenceReader = new SentencePerLineCorpusReader(sentenceFileReader, config, sessionId);
      this.sentenceReader = sentenceReader;
    } else {
      this.sentenceReader = null;
    }

    configPath = "corpus-lexical-entry-regex";
    if (config.hasPath(configPath)) {
      InputStream lexiconRegexFile = ConfigUtils.getFileFromConfig(config, configPath);
      Scanner regexScanner = new Scanner(new BufferedReader(new InputStreamReader(lexiconRegexFile, "UTF-8")));
      this.lexicalEntryReader = new RegexLexicalEntryReader(regexScanner);
    } else {
      this.lexicalEntryReader = null;
    }

    configPath = "line-rules";
    List<CorpusRule> corpusRules = new ArrayList<>();
    List<? extends Config> ruleConfigs = config.getConfigList(configPath);
    for (Config ruleConfig : ruleConfigs) {
      CorpusRule corpusRule = new CorpusRule(ruleConfig);
      corpusRules.add(corpusRule);
    }
    this.corpusLineReader = new CorpusLineReader(regex, this.getRequiredElements(), corpusRules, lexicalEntryReader, sessionId);

    configPath = "sentence-rules";
    sentenceRules = new ArrayList<>();
    List<? extends Config> sentenceRuleConfigs = config.getConfigList(configPath);
    for (Config ruleConfig : sentenceRuleConfigs) {
      CorpusSentenceRule sentenceRule = CorpusSentenceRule.getRule(ruleConfig);
      sentenceRules.add(sentenceRule);
    }

    List<String> skipLineRegexes = config.getStringList("skip-line-patterns");
    skipLinePatterns = new ArrayList<>();
    for (String skipLineRegex : skipLineRegexes) {
      skipLinePatterns.add(Pattern.compile(skipLineRegex));
    }
  }

  protected CorpusElement[] getRequiredElements() {
    return new CorpusElement[] { CorpusElement.TOKEN };
  }

  private static final class UnprocessedLine {
    private final String line;
    private final int lineNumber;
    private final boolean skip;
    private final List<CorpusSentenceRule> sentenceRules;
    private final List<Matcher> matchers;

    public UnprocessedLine(String line, int lineNumber, boolean skip, List<CorpusSentenceRule> sentenceRules, List<Matcher> matchers) {
      super();
      this.line = line;
      this.lineNumber = lineNumber;
      this.skip = skip;
      this.sentenceRules = sentenceRules;
      this.matchers = matchers;
    }

    @Override
    public String toString() {
      return "UnprocessedLine [line=" + line + ", lineNumber=" + lineNumber + ", skip=" + skip + "]";
    }
  }

  @Override
  public boolean hasNextSentence() throws TalismaneException, IOException {
    if (this.getMaxSentenceCount() > 0 && sentenceCount >= this.getMaxSentenceCount()) {
      // we've reached the end, do nothing
    } else {
      while (sentenceLines == null) {
        List<UnprocessedLine> lines = new ArrayList<>();
        int skippedLineCount = 0;
        if (!this.hasNextLine())
          break;

        while ((this.hasNextLine() || lines.size() > 0) && sentenceLines == null) {
          String line = "";
          if (this.hasNextLine())
            line = this.nextLine().replace("\r", "");
          lineNumber++;

          if (LOG.isTraceEnabled())
            LOG.trace("Line " + lineNumber + ": " + line);

          if (line.length() > 0) {
            boolean skip = false;
            for (Pattern skipLinePattern : skipLinePatterns) {
              if (skipLinePattern.matcher(line).matches()) {
                if (LOG.isTraceEnabled())
                  LOG.trace("Skipping by pattern: " + skipLinePattern.pattern());
                skip = true;
                skippedLineCount++;
                break;
              }
            }
            List<CorpusSentenceRule> myRules = new ArrayList<>();
            List<Matcher> myMatchers = new ArrayList<>();
            for (CorpusSentenceRule sentenceRule : sentenceRules) {
              Matcher matcher = sentenceRule.getPattern().matcher(line);
              if (matcher.matches()) {
                if (LOG.isTraceEnabled())
                  LOG.trace("Matched rule: " + sentenceRule);
                myRules.add(sentenceRule);
                myMatchers.add(matcher);
              }
            }
            UnprocessedLine unprocessedLine = new UnprocessedLine(line, lineNumber, skip, myRules, myMatchers);

            lines.add(unprocessedLine);
          } else {
            if (lines.size() == 0 || lines.size() == skippedLineCount) {
              lines = new ArrayList<>();
              skippedLineCount = 0;
              continue;
            }

            // end of sentence

            boolean includeMe = true;

            // check cross-validation
            if (this.getCrossValidationSize() > 0) {
              if (this.getIncludeIndex() >= 0) {
                if (sentenceCount % this.getCrossValidationSize() != this.getIncludeIndex()) {
                  includeMe = false;
                }
              } else if (this.getExcludeIndex() >= 0) {
                if (sentenceCount % this.getCrossValidationSize() == this.getExcludeIndex()) {
                  includeMe = false;
                }
              }
            }

            if (this.getStartSentence() > sentenceCount) {
              includeMe = false;
            }

            sentenceCount++;
            LOG.debug("sentenceCount: " + sentenceCount);

            if (!includeMe) {
              lines = new ArrayList<>();
              skippedLineCount = 0;
              continue;
            }

            sentenceLines = new ArrayList<>();
            for (UnprocessedLine unprocessedLine : lines) {
              if (!unprocessedLine.skip) {
                CorpusLine corpusLine = corpusLineReader.read(unprocessedLine.line, unprocessedLine.lineNumber);

                sentenceLines.add(corpusLine);

                if (this.lexicalEntryReader != null) {
                  WritableLexicalEntry lexicalEntry = new CompactLexicalEntry(lexicalEntrySupport);
                  this.lexicalEntryReader.readEntry(unprocessedLine.line, lexicalEntry);
                  corpusLine.setLexicalEntry(lexicalEntry);
                }
              }
            }

            List<CorpusSentenceRule.MergeAction> mergeActions = new ArrayList<>();
            for (UnprocessedLine unprocessedLine : lines) {
              if (LOG.isTraceEnabled())
                LOG.trace("Line " + unprocessedLine);
              for (int i = 0; i < unprocessedLine.sentenceRules.size(); i++) {
                CorpusSentenceRule sentenceRule = unprocessedLine.sentenceRules.get(i);
                Matcher matcher = unprocessedLine.matchers.get(i);
                if (LOG.isTraceEnabled())
                  LOG.trace("Testing rule " + sentenceRule);
                CorpusSentenceRule.Action action = sentenceRule.apply(unprocessedLine.line, unprocessedLine.lineNumber, matcher, sentenceLines);
                if (LOG.isTraceEnabled())
                  LOG.trace("Result: " + action);
                if (action != null) {
                  if (action instanceof MergeAction)
                    mergeActions.add((MergeAction) action);
                  break;
                }
              }
            }

            if (mergeActions.size() > 0) {
              List<CorpusLine> newSentenceLines = new ArrayList<>();
              Map<Integer, MergeAction> indexesToMerge = new TreeMap<>();
              for (CorpusSentenceRule.MergeAction mergeAction : mergeActions) {
                for (CorpusLine lineToMerge : mergeAction.getLinesToMerge()) {
                  indexesToMerge.put(lineToMerge.getIndex(), mergeAction);
                }
              }
              int i = 1;

              Iterator<Integer> iIndexToMerge = indexesToMerge.keySet().iterator();
              int nextIndexToMerge = iIndexToMerge.next();
              int linesRemoved = 0;
              Map<Integer, Integer> indexChangeMap = new HashMap<>();
              indexChangeMap.put(0, 0);
              for (CorpusLine corpusLine : sentenceLines) {
                if (i == nextIndexToMerge) {
                  MergeAction mergeAction = indexesToMerge.get(i);
                  if (i == mergeAction.getFirstIndex()) {
                    newSentenceLines.add(mergeAction.getMergedLine());
                    linesRemoved -= 1;
                  }
                  linesRemoved += 1;
                  if (iIndexToMerge.hasNext())
                    nextIndexToMerge = iIndexToMerge.next();
                  else
                    nextIndexToMerge = -1;
                } else {
                  newSentenceLines.add(corpusLine);
                }
                indexChangeMap.put(i, i - linesRemoved);
                i++;
              }

              for (CorpusLine corpusLine : newSentenceLines) {
                corpusLine.setElement(CorpusElement.INDEX, "" + indexChangeMap.get(corpusLine.getIndex()));
                int governorIndex = corpusLine.getGovernorIndex();
                if (governorIndex >= 0)
                  corpusLine.setElement(CorpusElement.GOVERNOR, "" + indexChangeMap.get(corpusLine.getGovernorIndex()));
                int nonProjGovernorIndex = corpusLine.getNonProjGovernorIndex();
                if (nonProjGovernorIndex >= 0)
                  corpusLine.setElement(CorpusElement.NON_PROJ_GOVERNOR, "" + indexChangeMap.get(corpusLine.getNonProjGovernorIndex()));
              }

              sentenceLines = newSentenceLines;
            }

            Sentence sentence = null;
            if (sentenceReader != null && sentenceReader.hasNextSentence()) {
              sentence = sentenceReader.nextSentence();
            } else {
              LinguisticRules rules = TalismaneSession.get(sessionId).getLinguisticRules();
              if (rules == null)
                throw new TalismaneException("Linguistic rules have not been set.");

              String text = "";
              for (CorpusLine corpusLine : sentenceLines) {
                String word = corpusLine.getElement(CorpusElement.TOKEN);
                // check if a space should be added before this
                // token

                if (rules.shouldAddSpace(text, word))
                  text += " ";
                text += word;
              }
              sentence = new Sentence(text, currentFile, sessionId);
            }
            for (SentenceAnnotator sentenceAnnotator : TalismaneSession.get(sessionId).getSentenceAnnotators()) {
              sentenceAnnotator.annotate(sentence);
            }

            this.processSentence(sentence, sentenceLines);
          }
        }
      }
    }
    return (sentenceLines != null);
  }

  private boolean hasNextLine() {
    if (needsToReturnBlankLine)
      return true;
    return this.scanner.hasNextLine();
  }

  private String nextLine() {
    if (needsToReturnBlankLine) {
      needsToReturnBlankLine = false;
      return "";
    }
    return this.scanner.nextLine();
  }

  @Override
  public void onNextFile(File file) {
    currentFile = file;
    lineNumber = 0;
    this.needsToReturnBlankLine = true;
  }

  protected void processSentence(Sentence sentence, List<CorpusLine> corpusLines) throws TalismaneException, IOException {
  }

  public List<CorpusLine> nextCorpusLineList() throws TalismaneException, IOException {
    List<CorpusLine> nextSentence = null;
    if (this.hasNextSentence()) {
      nextSentence = sentenceLines;
      this.clearSentence();
    }
    return nextSentence;
  }

  protected void clearSentence() {
    this.sentenceLines = null;
  }

  /**
   * The regex used to find the corpus line elements.
   */
  public String getRegex() {
    return regex;
  }

  @Override
  public Map<String, String> getCharacteristics() {
    return super.getCharacteristics();
  }

  protected CorpusLineReader getCorpusLineReader() {
    return corpusLineReader;
  }

  protected File getCurrentFile() {
    return currentFile;
  }
}
