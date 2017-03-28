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
package com.joliciel.talismane.extensions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Talismane.Command;
import com.joliciel.talismane.Talismane.Module;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneMain;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.extensions.corpus.CorpusModifier;
import com.joliciel.talismane.extensions.corpus.CorpusProjectifier;
import com.joliciel.talismane.extensions.corpus.CorpusProjectifier.ProjectivationStrategy;
import com.joliciel.talismane.extensions.corpus.CorpusStatistics;
import com.joliciel.talismane.extensions.corpus.PosTaggerStatistics;
import com.joliciel.talismane.extensions.standoff.ConllFileSplitter;
import com.joliciel.talismane.extensions.standoff.StandoffReader;
import com.joliciel.talismane.extensions.standoff.StandoffWriter;
import com.joliciel.talismane.output.FreemarkerTemplateWriter;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.parser.ParserAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.sentenceAnnotators.SentenceAnnotatorLoadException;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.LogUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class Extensions {
  private static final Logger LOG = LoggerFactory.getLogger(Extensions.class);
  String referenceStatsPath = null;
  String corpusRulesPath = null;

  public enum ExtendedCommand {
    toStandoff,
    fromStandoff,
    splitConllFile,
    corpusStatistics,
    posTaggerStatistics,
    modifyCorpus,
    projectify
  }

  public static void main(String[] args) throws Exception {
    if (args.length > 0) {
      Set<String> argSet = new HashSet<>(Arrays.asList(args));
      if (argSet.contains("--" + ExtendedCommand.splitConllFile.name())) {
        ConllFileSplitter.main(args);
        return;
      }
    }

    OptionParser parser = new OptionParser();
    parser.accepts(ExtendedCommand.toStandoff.name(), "convert CoNLL to standoff notation");
    parser.accepts(ExtendedCommand.fromStandoff.name(), "convert standoff to CoNLL");
    parser.accepts(ExtendedCommand.corpusStatistics.name(), "calculate various corpus statistics from an annotated (parsed) corpus");
    parser.accepts(ExtendedCommand.posTaggerStatistics.name(), "calculate various pos-tagger statistics from a pos-tagged corpus");
    parser.accepts(ExtendedCommand.modifyCorpus.name(), "modify various aspects of a dependency annotation");
    parser.accepts(ExtendedCommand.projectify.name(), "automatically projectify a non-projective annotated (parsed) corpus");
    parser.acceptsAll(Arrays.asList("?", "help"), "show help").forHelp();

    OptionSpec<File> inFileOption = parser.accepts("inFile", "input file or directory").withRequiredArg().ofType(File.class);
    OptionSpec<File> outFileOption = parser.accepts("outFile", "output file or directory (when inFile is a directory)").withRequiredArg().ofType(File.class);
    OptionSpec<File> outDirOption = parser.accepts("outDir", "output directory (for writing evaluation and analysis files other than the standard output)")
        .withRequiredArg().ofType(File.class);

    OptionSpec<String> inputPatternOption = parser.accepts("inputPattern", "input pattern").withRequiredArg().ofType(String.class);
    OptionSpec<File> inputPatternFileOption = parser.accepts("inputPatternFile", "input pattern file").availableUnless(inputPatternOption).withRequiredArg()
        .ofType(File.class);

    OptionSpec<String> localeOption = parser.accepts("locale", "locale").withRequiredArg().ofType(String.class);
    OptionSpec<String> encodingOption = parser.accepts("encoding", "encoding for input and output").withRequiredArg().ofType(String.class);
    OptionSpec<String> inputEncodingOption = parser.accepts("inputEncoding", "encoding for input").withRequiredArg().ofType(String.class);
    OptionSpec<String> outputEncodingOption = parser.accepts("outputEncoding", "encoding for output").withRequiredArg().ofType(String.class);

    OptionSpec<File> lexiconOption = parser.accepts("lexicon", "semi-colon delimited list of pre-compiled lexicon files").withRequiredArg().ofType(File.class)
        .withValuesSeparatedBy(';');

    OptionSpec<File> textFiltersOption = parser.accepts("textFilters", "semi-colon delimited list of files containing text filters").withRequiredArg()
        .ofType(File.class).withValuesSeparatedBy(';');
    OptionSpec<File> tokenFiltersOption = parser.accepts("tokenFilters", "semi-colon delimited list of files containing token pre-annotators").withRequiredArg()
        .ofType(File.class).withValuesSeparatedBy(';');
    OptionSpec<File> tokenSequenceFiltersOption = parser.accepts("tokenSequenceFilters", "semi-colon delimited list of files containing token post-annotators")
        .withRequiredArg().ofType(File.class).withValuesSeparatedBy(';');

    OptionSpec<String> newlineOption = parser
        .accepts("newline", "how to handle newlines: " + "options are SPACE (will be replaced by a space) " + "and SENTENCE_BREAK (will break sentences)")
        .withRequiredArg().ofType(String.class);

    OptionSpec<Boolean> processByDefaultOption = parser.accepts("processByDefault", "If true, the input file is processed from the very start (e.g. TXT files)."
        + "If false, we wait until a text filter tells us to start processing (e.g. XML files).").withRequiredArg().ofType(Boolean.class);

    OptionSpec<Integer> blockSizeOption = parser
        .accepts("blockSize", "The block size to use when applying filters - if a text filter regex goes beyond the blocksize, Talismane will fail.")
        .withRequiredArg().ofType(Integer.class);

    OptionSpec<Integer> sentenceCountOption = parser.accepts("sentenceCount", "max sentences to process").withRequiredArg().ofType(Integer.class);
    OptionSpec<Integer> startSentenceOption = parser.accepts("startSentence", "first sentence index to process").withRequiredArg().ofType(Integer.class);

    OptionSpec<String> suffixOption = parser.accepts("suffix", "suffix to all output files").withRequiredArg().ofType(String.class);
    OptionSpec<String> outputDividerOption = parser
        .accepts("outputDivider", "a string to insert between sections marked for output (e.g. XML tags to be kept in the analysed output)."
            + " The String NEWLINE is interpreted as \"\n\". Otherwise, used literally.")
        .withRequiredArg().ofType(String.class);

    OptionSpec<String> csvSeparatorOption = parser.accepts("csvSeparator", "CSV file separator in output").withRequiredArg().ofType(String.class);
    OptionSpec<String> csvEncodingOption = parser.accepts("csvEncoding", "CSV file encoding in output").withRequiredArg().ofType(String.class);
    OptionSpec<String> csvLocaleOption = parser.accepts("csvLocale", "CSV file locale in output").withRequiredArg().ofType(String.class);

    OptionSpec<File> lexicalEntryRegexOption = parser.accepts("lexicalEntryRegex", "file describing regex for reading lexical entries in the corpus")
        .withRequiredArg().ofType(File.class);

    OptionSpec<File> referenceStatsOption = parser.accepts("referenceStats", "file containing stats for a reference corpus")
        .availableIf(ExtendedCommand.corpusStatistics.name(), ExtendedCommand.posTaggerStatistics.name()).withRequiredArg().ofType(File.class);

    OptionSpec<File> corpusRulesOption = parser.accepts("corpusRules", "file containing corpus modification rules")
        .requiredIf(ExtendedCommand.modifyCorpus.name()).withRequiredArg().ofType(File.class);
    OptionSpec<File> logConfigFileSpec = parser.accepts("logConfigFile", "logback configuration file").withRequiredArg().ofType(File.class);

    OptionSpec<String> nonProjectiveArcSuffixOption = parser.accepts("nonProjectiveSuffix", "Suffix to add to non-projective arcs when projectifying")
        .availableIf(ExtendedCommand.projectify.name()).withRequiredArg().ofType(String.class);
    OptionSpec<ProjectivationStrategy> projectierStrategyOption = parser
        .accepts("projectifierStrategy", "Strategy to select the projective head: " + Arrays.toString(ProjectivationStrategy.values()))
        .availableIf(ExtendedCommand.projectify.name()).withRequiredArg().ofType(ProjectivationStrategy.class);

    OptionSet options = parser.parse(args);
    if (args.length == 0 || options.has("help")) {
      parser.printHelpOn(System.out);
      return;
    }

    Map<String, Object> values = new HashMap<>();
    values.put("talismane.core.command", Command.process.name());

    values.put("talismane.core.module", Module.parser.name());
    ExtendedCommand command = null;
    if (options.has(ExtendedCommand.corpusStatistics.name())) {
      command = ExtendedCommand.corpusStatistics;
    } else if (options.has(ExtendedCommand.fromStandoff.name())) {
      command = ExtendedCommand.fromStandoff;
    } else if (options.has(ExtendedCommand.modifyCorpus.name())) {
      command = ExtendedCommand.modifyCorpus;
    } else if (options.has(ExtendedCommand.posTaggerStatistics.name())) {
      command = ExtendedCommand.posTaggerStatistics;
      values.put("talismane.core.module", Module.posTagger.name());
    } else if (options.has(ExtendedCommand.projectify.name())) {
      command = ExtendedCommand.projectify;
    } else if (options.has(ExtendedCommand.toStandoff.name())) {
      command = ExtendedCommand.toStandoff;
    }

    if (options.has(localeOption))
      values.put("talismane.core.locale", options.valueOf(localeOption));
    if (options.has(encodingOption))
      values.put("talismane.core.encoding", options.valueOf(encodingOption));
    if (options.has(inputEncodingOption))
      values.put("talismane.core.input-encoding", options.valueOf(inputEncodingOption));
    if (options.has(outputEncodingOption))
      values.put("talismane.core.output-encoding", options.valueOf(outputEncodingOption));

    if (options.has(inputPatternFileOption)) {
      InputStream inputPatternFile = new FileInputStream(options.valueOf(inputPatternFileOption));
      String inputRegex = "";
      try (Scanner inputPatternScanner = new Scanner(new BufferedReader(new InputStreamReader(inputPatternFile, "UTF-8")))) {
        if (inputPatternScanner.hasNextLine()) {
          inputRegex = inputPatternScanner.nextLine();
        }
      }
      if (inputRegex == null)
        throw new TalismaneException("No input pattern found in " + options.valueOf(inputPatternFileOption).getPath());
      values.put("talismane.core.tokeniser.input.preannotated-pattern", inputRegex);
      values.put("talismane.core.posTagger.input.preannotated-pattern", inputRegex);
      values.put("talismane.core.parser.input.preannotated-pattern", inputRegex);
    } else if (options.has(inputPatternOption)) {
      String inputRegex = options.valueOf(inputPatternOption);
      values.put("talismane.core.tokeniser.input.preannotated-pattern", inputRegex);
      values.put("talismane.core.posTagger.input.preannotated-pattern", inputRegex);
      values.put("talismane.core.parser.input.preannotated-pattern", inputRegex);
    }

    if (options.has(lexiconOption)) {
      List<String> lexiconPaths = options.valuesOf(lexiconOption).stream().map(f -> f.getPath()).collect(Collectors.toList());
      values.put("talismane.core.lexicons", lexiconPaths);
    }

    if (options.has(textFiltersOption)) {
      List<String> textFilterPaths = options.valuesOf(textFiltersOption).stream().map(f -> f.getPath()).collect(Collectors.toList());
      values.put("talismane.core.annotators.text-filters", textFilterPaths);
    }

    if (options.has(tokenFiltersOption)) {
      List<String> tokenFilterPaths = options.valuesOf(tokenFiltersOption).stream().map(f -> f.getPath()).collect(Collectors.toList());
      values.put("talismane.core.annotators.text-annotators", tokenFilterPaths);
    }

    if (options.has(tokenSequenceFiltersOption)) {
      List<String> tokenSequenceFilterPaths = options.valuesOf(tokenSequenceFiltersOption).stream().map(f -> f.getPath()).collect(Collectors.toList());
      values.put("talismane.core.annotators.token-sequence-filters", tokenSequenceFilterPaths);
    }

    List<String> inputLocations = Arrays.asList("talismane.core.input", "talismane.core.language-detector.input", "talismane.core.language-detector.train",
        "talismane.core.language-detector.evaluate", "talismane.core.sentence-detector.input", "talismane.core.sentence-detector.train",
        "talismane.core.sentence-detector.evaluate", "talismane.core.tokeniser.input", "talismane.core.tokeniser.train", "talismane.core.tokeniser.evaluate",
        "talismane.core.pos-tagger.input", "talismane.core.pos-tagger.train", "talismane.core.pos-tagger.evaluate", "talismane.core.parser.input",
        "talismane.core.parser.train", "talismane.core.parser.evaluate");

    List<String> outputLocations = Arrays.asList("talismane.core.output", "talismane.core.language-detector.output", "talismane.core.sentence-detector.output",
        "talismane.core.tokeniser.output", "talismane.core.pos-tagger.output", "talismane.core.parser.output");

    if (options.has(newlineOption))
      values.put("talismane.core.newline", options.valueOf(newlineOption));
    if (options.has(processByDefaultOption))
      values.put("talismane.core.analysis.process-by-default", options.valueOf(processByDefaultOption));
    if (options.has(blockSizeOption))
      values.put("talismane.core.block-size", options.valueOf(blockSizeOption));
    if (options.has(sentenceCountOption))
      for (String inputLocation : inputLocations)
        values.put(inputLocation + ".sentence-count", options.valueOf(sentenceCountOption));

    if (options.has(startSentenceOption))
      for (String inputLocation : inputLocations)
        values.put(inputLocation + ".start-sentence", options.valueOf(startSentenceOption));

    if (options.has(suffixOption))
      values.put("talismane.core.suffix", options.valueOf(suffixOption));
    if (options.has(outputDividerOption))
      for (String outputLocation : outputLocations)
        values.put(outputLocation + ".output-divider", options.valueOf(outputDividerOption));

    if (options.has(csvSeparatorOption))
      values.put("talismane.core.csv.separator", options.valueOf(csvSeparatorOption));
    if (options.has(csvEncodingOption))
      values.put("talismane.core.csv.encoding", options.valueOf(csvEncodingOption));
    if (options.has(csvLocaleOption))
      values.put("talismane.core.csv.locale", options.valueOf(csvLocaleOption));

    if (options.has(lexicalEntryRegexOption)) {
      values.put("talismane.core.pos-tagger.input.corpus-lexical-entry-regex", options.valueOf(lexicalEntryRegexOption).getPath());
      values.put("talismane.core.parser.input.corpus-lexical-entry-regex", options.valueOf(lexicalEntryRegexOption).getPath());
    }

    if (options.has(logConfigFileSpec))
      LogUtils.configureLogging(options.valueOf(logConfigFileSpec));

    if (options.has(nonProjectiveArcSuffixOption))
      values.put("talismane.extensions.projectifier.non-projective-arc-suffix", options.valueOf(nonProjectiveArcSuffixOption));
    if (options.has(projectierStrategyOption))
      values.put("talismane.extensions.projectifier.strategy", options.valueOf(projectierStrategyOption).name());

    Config config = ConfigFactory.parseMap(values).withFallback(ConfigFactory.load());
    Extensions extensions = new Extensions(config, command);

    if (options.has(referenceStatsOption))
      extensions.setReferenceStatsPath(options.valueOf(referenceStatsOption).getPath());
    if (options.has(corpusRulesOption))
      extensions.setCorpusRulesPath(options.valueOf(corpusRulesOption).getPath());

    File inFile = null;
    File outFile = null;
    File outDir = null;
    if (options.has(inFileOption))
      inFile = options.valueOf(inFileOption);

    if (options.has(outFileOption))
      outFile = options.valueOf(outFileOption);
    if (options.has(outDirOption))
      outDir = options.valueOf(outDirOption);

    extensions.execute(inFile, outFile, outDir);
  }

  private final Config config;
  private final ExtendedCommand command;

  public Extensions(Config config, ExtendedCommand command) {
    this.config = config;
    this.command = command;
  }

  public void execute(File inFile, File outFile, File outDir)
      throws IOException, ReflectiveOperationException, TalismaneException, SentenceAnnotatorLoadException {
    if (LOG.isTraceEnabled())
      LOG.trace(config.root().render());
    long startTime = System.currentTimeMillis();
    String sessionId = "";
    TalismaneSession session = new TalismaneSession(config, sessionId);
    session.setFileForBasename(inFile);

    Reader reader = TalismaneMain.getReader(inFile, true, session);
    Writer writer = TalismaneMain.getWriter(outFile, inFile, session);

    if (outFile != null && outFile.getParentFile() != null)
      outFile.getParentFile().mkdirs();

    if (outDir != null)
      outDir.mkdirs();

    ParserAnnotatedCorpusReader parserAnnotatedCorpusReader = null;

    List<ParseConfigurationProcessor> parseConfigurationProcessors = new ArrayList<>();
    List<PosTagSequenceProcessor> posTagSequenceProcessors = new ArrayList<>();
    try {
      switch (command) {
      case toStandoff: {
        File standoffAnnotationFile = new File(outDir, session.getBaseName() + ".ann");
        Writer standoffAnnotationWriter = TalismaneMain.getWriter(standoffAnnotationFile, inFile, session);
        StandoffWriter standoffWriter = new StandoffWriter(standoffAnnotationWriter, session);
        parseConfigurationProcessors.add(standoffWriter);

        File standoffSentenceFile = new File(outDir, session.getBaseName() + ".txt");
        Writer standoffSentenceWriter = TalismaneMain.getWriter(standoffSentenceFile, inFile, session);
        InputStream inputStream = StandoffWriter.class.getResourceAsStream("standoffSentences.ftl");
        Reader templateReader = new BufferedReader(new InputStreamReader(inputStream));
        FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(templateReader, standoffSentenceWriter);
        parseConfigurationProcessors.add(templateWriter);

        break;
      }
      case fromStandoff: {
        StandoffReader standoffReader = new StandoffReader(reader, session.getConfig().getConfig("talismane.core.parser.input"), session);
        parserAnnotatedCorpusReader = standoffReader;
        parseConfigurationProcessors.addAll(ParseConfigurationProcessor.getProcessors(writer, outDir, session));
        break;
      }
      case corpusStatistics: {
        CorpusStatistics stats = new CorpusStatistics(session);

        if (referenceStatsPath != null) {
          File referenceStatsFile = new File(referenceStatsPath);
          CorpusStatistics referenceStats = CorpusStatistics.loadFromFile(referenceStatsFile);
          stats.setReferenceWords(referenceStats.getWords());
          stats.setReferenceLowercaseWords(referenceStats.getLowerCaseWords());
        }

        File csvFile = new File(outDir, session.getBaseName() + "_stats.csv");
        csvFile.delete();
        csvFile.createNewFile();
        Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), "UTF8"));
        stats.setWriter(csvFileWriter);

        File serializationFile = new File(outDir, session.getBaseName() + "_stats.zip");
        serializationFile.delete();
        stats.setSerializationFile(serializationFile);

        parseConfigurationProcessors.add(stats);

        break;
      }
      case posTaggerStatistics: {
        PosTaggerStatistics stats = new PosTaggerStatistics(session);

        if (referenceStatsPath != null) {
          File referenceStatsFile = new File(referenceStatsPath);
          PosTaggerStatistics referenceStats = PosTaggerStatistics.loadFromFile(referenceStatsFile);
          stats.setReferenceWords(referenceStats.getWords());
          stats.setReferenceLowercaseWords(referenceStats.getLowerCaseWords());
        }

        File csvFile = new File(outDir, session.getBaseName() + "_stats.csv");
        csvFile.delete();
        csvFile.createNewFile();
        Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), "UTF8"));
        stats.setWriter(csvFileWriter);

        File serializationFile = new File(outDir, session.getBaseName() + "_stats.zip");
        serializationFile.delete();
        stats.setSerializationFile(serializationFile);

        posTagSequenceProcessors.add(stats);

        break;
      }
      case modifyCorpus: {
        if (corpusRulesPath == null)
          throw new TalismaneException("corpusRules is required for modifyCorpus command");

        List<String> corpusRules = new ArrayList<String>();
        File corpusRulesFile = new File(corpusRulesPath);

        try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(corpusRulesFile), "UTF-8")))) {
          while (scanner.hasNextLine()) {
            corpusRules.add(scanner.nextLine());
          }
        }
        CorpusModifier corpusModifier = new CorpusModifier(corpusRules);

        parseConfigurationProcessors.add(corpusModifier);
        parseConfigurationProcessors.addAll(ParseConfigurationProcessor.getProcessors(writer, outDir, session));
        break;
      }
      case projectify: {
        CorpusProjectifier projectifier = new CorpusProjectifier(config);
        parseConfigurationProcessors.add(projectifier);
        parseConfigurationProcessors.addAll(ParseConfigurationProcessor.getProcessors(writer, outDir, session));
        break;
      }
      default: {
        throw new RuntimeException("Unknown command: " + command);
      }
      }

      IOException ioException = null;

      switch (session.getModule()) {
      case posTagger: {
        try {
          PosTagAnnotatedCorpusReader corpusReader = PosTagAnnotatedCorpusReader.getCorpusReader(reader, config.getConfig("talismane.core.pos-tagger.input"),
              session);
          while (corpusReader.hasNextSentence()) {
            PosTagSequence posTagSequence = corpusReader.nextPosTagSequence();
            for (PosTagSequenceProcessor processor : posTagSequenceProcessors)
              processor.onNextPosTagSequence(posTagSequence);
          }
        } finally {
          for (PosTagSequenceProcessor processor : posTagSequenceProcessors) {
            try {
              processor.onCompleteAnalysis();
              processor.close();
            } catch (IOException e) {
              LogUtils.logError(LOG, e);
              ioException = e;
            }
          }
        }
        break;
      }
      case parser: {
        try {
          ParserAnnotatedCorpusReader corpusReader = parserAnnotatedCorpusReader;
          if (corpusReader == null)
            corpusReader = ParserAnnotatedCorpusReader.getCorpusReader(reader, config.getConfig("talismane.core.parser.input"), session);
          while (corpusReader.hasNextSentence()) {
            ParseConfiguration configuration = corpusReader.nextConfiguration();
            for (ParseConfigurationProcessor processor : parseConfigurationProcessors)
              processor.onNextParseConfiguration(configuration);
          }
        } finally {
          for (ParseConfigurationProcessor processor : parseConfigurationProcessors) {
            try {
              processor.onCompleteParse();
              processor.close();
            } catch (IOException e) {
              LogUtils.logError(LOG, e);
              ioException = e;
            }
          }
        }
        break;
      }
      default:
        throw new TalismaneException("Command '" + session.getCommand() + "' does not yet support module: " + session.getModule());
      }

      if (ioException != null)
        throw ioException;

    } finally {
      long endTime = System.currentTimeMillis();
      long totalTime = endTime - startTime;
      LOG.debug("Total time for Talismane.process(): " + totalTime);

      if (config.getBoolean("talismane.core.output.log-execution-time")) {
        try {
          CSVFormatter CSV = new CSVFormatter();
          Writer csvFileWriter = null;
          File csvFile = new File(outDir, session.getBaseName() + ".stats.csv");

          csvFile.delete();
          csvFile.createNewFile();
          csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), "UTF8"));
          csvFileWriter.write(CSV.format("total time") + CSV.format(totalTime) + "\n");
          csvFileWriter.flush();
          csvFileWriter.close();
        } catch (Exception e) {
          LogUtils.logError(LOG, e);
        }
      }
    }

  }

  public String getReferenceStatsPath() {
    return referenceStatsPath;
  }

  public void setReferenceStatsPath(String referenceStatsPath) {
    this.referenceStatsPath = referenceStatsPath;
  }

  public String getCorpusRulesPath() {
    return corpusRulesPath;
  }

  public void setCorpusRulesPath(String corpusRulesPath) {
    this.corpusRulesPath = corpusRulesPath;
  }

}
