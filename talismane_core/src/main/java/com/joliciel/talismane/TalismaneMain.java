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
package com.joliciel.talismane;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Talismane.BuiltInTemplate;
import com.joliciel.talismane.Talismane.Command;
import com.joliciel.talismane.Talismane.Mode;
import com.joliciel.talismane.Talismane.Module;
import com.joliciel.talismane.Talismane.ProcessingOption;
import com.joliciel.talismane.languageDetector.LanguageDetector;
import com.joliciel.talismane.languageDetector.LanguageDetectorProcessor;
import com.joliciel.talismane.languageDetector.LanguageDetectorTrainer;
import com.joliciel.talismane.lexicon.Diacriticizer;
import com.joliciel.talismane.lexicon.LexiconDeserializer;
import com.joliciel.talismane.lexicon.LexiconReader;
import com.joliciel.talismane.machineLearning.MachineLearningAlgorithm;
import com.joliciel.talismane.parser.ParseComparator;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.parser.Parser.PredictTransitions;
import com.joliciel.talismane.parser.ParserAnnotatedCorpusReader;
import com.joliciel.talismane.parser.ParserEvaluator;
import com.joliciel.talismane.parser.ParserTrainer;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagComparator;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.posTagger.PosTaggerEvaluator;
import com.joliciel.talismane.posTagger.PosTaggerTrainer;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.sentenceAnnotators.SentenceAnnotatorLoadException;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorEvaluator;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorTrainer;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.tokeniser.TokenComparator;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserEvaluator;
import com.joliciel.talismane.tokeniser.patterns.PatternTokeniserTrainer;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.WeightedOutcome;
import com.joliciel.talismane.utils.io.DirectoryReader;
import com.joliciel.talismane.utils.io.DirectoryWriter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * Direct entry point for Talismane from the command line.
 * 
 * @author Assaf Urieli
 *
 */
public class TalismaneMain {
  private static final Logger LOG = LoggerFactory.getLogger(TalismaneMain.class);

  private final Config config;

  public static void main(String[] args) throws Exception {
    if (args.length > 0) {
      Set<String> argSet = new HashSet<>(Arrays.asList(args));
      if (argSet.contains("--serializeLexicon")) {
        LexiconReader.main(args);
        return;
      }
      if (argSet.contains("--testLexicon")) {
        LexiconDeserializer.main(args);
        return;
      }
      if (argSet.contains("--serializeDiacriticizer")) {
        Diacriticizer.main(args);
        return;
      }
      if (argSet.contains("--testDiacriticizer")) {
        Diacriticizer.main(args);
        return;
      }
    }

    OptionParser parser = new OptionParser();

    parser.accepts("analyse", "analyse text");
    parser.accepts("train", "train model").availableUnless("analyse");
    parser.accepts("evaluate", "evaluate annotated corpus").availableUnless("analyse", "train");
    parser.accepts("compare", "compare two annotated corpora").availableUnless("analyse", "train", "evaluate");
    parser.accepts("process", "process annotated corpus").availableUnless("analyse", "train", "evaluate", "compare");
    parser.acceptsAll(Arrays.asList("?", "help"), "show help").availableUnless("analyse", "train", "evaluate", "compare", "process").forHelp();

    OptionSpec<Module> moduleOption = parser.accepts("module", "training / evaluation / processing module: " + Arrays.toString(Module.values()))
        .availableIf("train", "evaluate", "compare", "process").withRequiredArg().ofType(Module.class);
    OptionSpec<Module> startModuleOption = parser.accepts("startModule", "where to start analysis (or evaluation): " + Arrays.toString(Module.values()))
        .availableIf("analyse", "evaluate").withRequiredArg().ofType(Module.class);
    OptionSpec<Module> endModuleOption = parser.accepts("endModule", "where to end analysis: " + Arrays.toString(Module.values())).availableIf("analyse")
        .withRequiredArg().ofType(Module.class);

    OptionSpec<Mode> modeOption = parser.accepts("mode", "execution mode: " + Arrays.toString(Mode.values())).availableIf("analyse").withRequiredArg()
        .ofType(Mode.class);
    OptionSpec<Integer> portOption = parser.accepts("port", "which port to listen on").availableIf("analyse").withRequiredArg().ofType(Integer.class);

    OptionSpec<File> inFileOption = parser.accepts("inFile", "input file or directory").withRequiredArg().ofType(File.class);
    OptionSpec<File> outFileOption = parser.accepts("outFile", "output file or directory (when inFile is a directory)").withRequiredArg().ofType(File.class);
    OptionSpec<File> outDirOption = parser.accepts("outDir", "output directory (for writing evaluation and analysis files other than the standard output)")
        .withRequiredArg().ofType(File.class);

    OptionSpec<String> inputPatternOption = parser.accepts("inputPattern", "input pattern").withRequiredArg().ofType(String.class);
    OptionSpec<File> inputPatternFileOption = parser.accepts("inputPatternFile", "input pattern file").availableUnless(inputPatternOption).withRequiredArg()
        .ofType(File.class);
    OptionSpec<String> evalPatternOption = parser.accepts("evalPattern", "input pattern for evaluation").availableIf("evaluate", "compare").withRequiredArg()
        .ofType(String.class);
    OptionSpec<File> evalPatternFileOption = parser.accepts("evalPatternFile", "input pattern file for evaluation").availableUnless(evalPatternOption)
        .withRequiredArg().ofType(File.class);

    OptionSpec<String> localeOption = parser.accepts("locale", "locale").withRequiredArg().ofType(String.class);
    OptionSpec<String> encodingOption = parser.accepts("encoding", "encoding for input and output").withRequiredArg().ofType(String.class);
    OptionSpec<String> inputEncodingOption = parser.accepts("inputEncoding", "encoding for input").withRequiredArg().ofType(String.class);
    OptionSpec<String> outputEncodingOption = parser.accepts("outputEncoding", "encoding for output").withRequiredArg().ofType(String.class);

    OptionSpec<File> languageModelOption = parser.accepts("languageModel", "statistical model for language recognition").withRequiredArg().ofType(File.class);
    OptionSpec<File> sentenceModelOption = parser.accepts("sentenceModel", "statistical model for sentence detection").withRequiredArg().ofType(File.class);
    OptionSpec<File> tokeniserModelOption = parser.accepts("tokeniserModel", "statistical model for tokenisation").withRequiredArg().ofType(File.class);
    OptionSpec<File> posTaggerModelOption = parser.accepts("posTaggerModel", "statistical model for pos-tagging").withRequiredArg().ofType(File.class);
    OptionSpec<File> parserModelOption = parser.accepts("parserModel", "statistical model for dependency parsing").withRequiredArg().ofType(File.class);
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
        .availableIf("analyse").withRequiredArg().ofType(String.class);

    OptionSpec<Boolean> processByDefaultOption = parser
        .accepts("processByDefault",
            "If true, the input file is processed from the very start (e.g. TXT files)."
                + "If false, we wait until a text filter tells us to start processing (e.g. XML files).")
        .availableIf("analyse").withRequiredArg().ofType(Boolean.class);

    OptionSpec<Integer> blockSizeOption = parser
        .accepts("blockSize", "The block size to use when applying filters - if a text filter regex goes beyond the blocksize, Talismane will fail.")
        .availableIf("analyse").withRequiredArg().ofType(Integer.class);

    OptionSpec<Integer> sentenceCountOption = parser.accepts("sentenceCount", "max sentences to process").withRequiredArg().ofType(Integer.class);
    OptionSpec<Integer> startSentenceOption = parser.accepts("startSentence", "first sentence index to process").withRequiredArg().ofType(Integer.class);
    OptionSpec<Integer> crossValidationSizeOption = parser.accepts("crossValidationSize", "number of cross-validation folds").availableIf("train", "evaluate")
        .withRequiredArg().ofType(Integer.class);
    OptionSpec<Integer> includeIndexOption = parser.accepts("includeIndex", "cross-validation index to include for evaluation").availableIf("evaluate")
        .withRequiredArg().ofType(Integer.class);
    OptionSpec<Integer> excludeIndexOption = parser.accepts("excludeIndex", "cross-validation index to exclude for training").availableIf("train")
        .withRequiredArg().ofType(Integer.class);

    OptionSpec<BuiltInTemplate> builtInTemplateOption = parser
        .accepts("builtInTemplate", "pre-defined output template: " + Arrays.toString(BuiltInTemplate.values())).availableUnless("train").withRequiredArg()
        .ofType(BuiltInTemplate.class);

    OptionSpec<File> templateOption = parser.accepts("template", "user-defined template for output").availableUnless("train", "builtInTemplate")
        .withRequiredArg().ofType(File.class);

    OptionSpec<File> posTaggerRulesOption = parser.accepts("posTaggerRules", "semi-colon delimited list of files containing pos-tagger rules")
        .availableIf("analyse", "evaluate").withRequiredArg().ofType(File.class).withValuesSeparatedBy(';');
    OptionSpec<File> parserRulesOption = parser.accepts("parserRules", "semi-colon delimited list of files containing parser rules")
        .availableIf("analyse", "evaluate").withRequiredArg().ofType(File.class).withValuesSeparatedBy(';');

    OptionSpec<String> suffixOption = parser.accepts("suffix", "suffix to all output files").withRequiredArg().ofType(String.class);
    OptionSpec<String> outputDividerOption = parser
        .accepts("outputDivider", "a string to insert between sections marked for output (e.g. XML tags to be kept in the analysed output)."
            + " The String NEWLINE is interpreted as \"\n\". Otherwise, used literally.")
        .availableIf("analyse").withRequiredArg().ofType(String.class);

    OptionSpec<Integer> beamWidthOption = parser.accepts("beamWidth", "beam width in pos-tagger and parser beam search").availableIf("analyse", "evaluate")
        .withRequiredArg().ofType(Integer.class);
    OptionSpec<Integer> tokeniserBeamWidthOption = parser.accepts("tokeniserBeamWidth", "beam width in tokeniser beam search")
        .availableIf("analyse", "evaluate").withRequiredArg().ofType(Integer.class);
    OptionSpec<Boolean> propagateBeamOption = parser.accepts("propagateBeam", "should we propagate the pos-tagger beam to the parser")
        .availableIf("analyse", "evaluate").withRequiredArg().ofType(Boolean.class);

    OptionSpec<Integer> maxParseAnalysisTimeOption = parser
        .accepts("maxParseAnalysisTime", "how long we will attempt to parse a sentence before leaving the parse as is, in seconds")
        .availableIf("analyse", "evaluate").withRequiredArg().ofType(Integer.class);
    OptionSpec<Integer> minFreeMemoryOption = parser.accepts("minFreeMemory", "minimum amount of remaining free memory to continue a parse, in kilobytes")
        .availableIf("analyse", "evaluate").withRequiredArg().ofType(Integer.class);
    OptionSpec<Boolean> earlyStopOption = parser.accepts("earlyStop", "stop as soon as the beam contains n terminal configurations")
        .availableIf("analyse", "evaluate").withRequiredArg().ofType(Boolean.class);

    OptionSpec<File> evalFileOption = parser.accepts("evalFile", "evaluation corpus file").availableIf("evaluate", "compare").withRequiredArg()
        .ofType(File.class);

    OptionSpec<String> csvSeparatorOption = parser.accepts("csvSeparator", "CSV file separator in output").withRequiredArg().ofType(String.class);
    OptionSpec<String> csvEncodingOption = parser.accepts("csvEncoding", "CSV file encoding in output").withRequiredArg().ofType(String.class);
    OptionSpec<String> csvLocaleOption = parser.accepts("csvLocale", "CSV file locale in output").withRequiredArg().ofType(String.class);

    OptionSpec<Boolean> includeUnknownWordResultsOption = parser
        .accepts("includeUnknownWordResults",
            "if true, will add files ending with \"_unknown.csv\" and \"_known.csv\" splitting pos-tagging f-scores into known and unknown words")
        .availableIf("evaluate").withRequiredArg().ofType(Boolean.class);
    OptionSpec<Boolean> includeLexiconCoverageOption = parser
        .accepts("includeUnknownWordResults", "if true, will add a file ending with \".lexiconCoverage.csv\" giving lexicon word coverage")
        .availableIf("evaluate").withRequiredArg().ofType(Boolean.class);

    OptionSpec<Boolean> labeledEvaluationOption = parser
        .accepts("labeledEvaluation", "if true, takes both governor and dependency label into account when determining errors").availableIf("evaluate")
        .withRequiredArg().ofType(Boolean.class);

    OptionSpec<ProcessingOption> processingOption = parser.accepts("option", "process command option: " + Arrays.toString(ProcessingOption.values()))
        .availableIf("process").withRequiredArg().ofType(ProcessingOption.class);
    OptionSpec<File> lexicalEntryRegexOption = parser.accepts("lexicalEntryRegex", "file describing regex for reading lexical entries in the corpus")
        .withRequiredArg().ofType(File.class);

    OptionSpec<File> featuresOption = parser.accepts("features", "a file containing the training feature descriptors").availableIf("train", "process")
        .withRequiredArg().ofType(File.class);
    OptionSpec<File> tokeniserPatternsOption = parser.accepts("tokeniserPatterns", "a file containing the patterns for tokeniser training")
        .availableIf("train", "process").withRequiredArg().ofType(File.class);
    OptionSpec<File> sentenceFileOption = parser
        .accepts("sentenceFile", "the text of sentences represented by the tokenised input is provided by this file, one sentence per line").withRequiredArg()
        .ofType(File.class);
    OptionSpec<File> languageCorpusMapOption = parser
        .accepts("languageCorpusMap", "a file giving a mapping of languages to corpora for langauge-detection training").availableIf("train").withRequiredArg()
        .ofType(File.class);
    OptionSpec<PredictTransitions> predictTransitionsOption = parser.accepts("predictTransitions",
        "should the transitions leading to the corpus dependencies be predicted - normally only required for training (leave at \"depends\"). Options are: "
            + Arrays.toString(PredictTransitions.values()))
        .withRequiredArg().ofType(PredictTransitions.class);
    OptionSpec<String> testWordsOption = parser.accepts("testWords", "comma-delimited test words for pos-tagger feature tester").availableIf("process")
        .withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');

    OptionSpec<MachineLearningAlgorithm> algorithmOption = parser
        .accepts("algorithm", "machine learning algorithm: " + Arrays.toString(MachineLearningAlgorithm.values())).availableIf("train").withRequiredArg()
        .ofType(MachineLearningAlgorithm.class);
    OptionSpec<Integer> cutoffOption = parser.accepts("cutoff", "in how many distinct events should a feature appear in order to get included in the model?")
        .availableIf("train").withRequiredArg().ofType(Integer.class);
    OptionSpec<Double> linearSVMEpsilonOption = parser.accepts("linearSVMEpsilon", "parameter epsilon, typical values are 0.01, 0.05, 0.1, 0.5")
        .availableIf("train").withRequiredArg().ofType(Double.class);
    OptionSpec<Double> linearSVMCostOption = parser.accepts("linearSVMCost", "parameter C, typical values are powers of 2, from 2^-5 to 2^5")
        .availableIf("train").withRequiredArg().ofType(Double.class);
    OptionSpec<Boolean> oneVsRestOption = parser
        .accepts("oneVsRest", "should we treat each outcome explicity as one vs. rest, allowing for an event to have multiple outcomes?").availableIf("train")
        .withRequiredArg().ofType(Boolean.class);
    OptionSpec<Integer> iterationsOption = parser.accepts("iterations", "the number of training iterations (MaxEnt, Perceptron)").availableIf("train")
        .withRequiredArg().ofType(Integer.class);

    OptionSpec<File> logConfigFileSpec = parser.accepts("logConfigFile", "logback configuration file").withRequiredArg().ofType(File.class);

    OptionSet options = parser.parse(args);
    if (args.length == 0 || options.has("help")) {
      parser.printHelpOn(System.out);
      return;
    }

    Map<String, Object> values = new HashMap<>();
    if (options.has("analyse"))
      values.put("talismane.core.command", Command.analyse.name());
    if (options.has("train"))
      values.put("talismane.core.command", Command.train.name());
    if (options.has("evaluate"))
      values.put("talismane.core.command", Command.evaluate.name());
    if (options.has("compare"))
      values.put("talismane.core.command", Command.compare.name());
    if (options.has("process"))
      values.put("talismane.core.command", Command.process.name());
    if (options.has(moduleOption))
      values.put("talismane.core.module", options.valueOf(moduleOption).name());
    if (options.has(startModuleOption)) {
      values.put("talismane.core.analysis.start-module", options.valueOf(startModuleOption).name());
      values.put("talismane.core.pos-tagger.evaluate.start-module", options.valueOf(startModuleOption).name());
      values.put("talismane.core.parser.evaluate.start-module", options.valueOf(startModuleOption).name());
    }
    if (options.has(endModuleOption))
      values.put("talismane.core.analysis.end-module", options.valueOf(endModuleOption).name());
    if (options.has(modeOption))
      values.put("talismane.core.mode", options.valueOf(modeOption).name());
    if (options.has(portOption))
      values.put("talismane.core.port", options.valueOf(portOption));

    if (options.has(localeOption))
      values.put("talismane.core.locale", options.valueOf(localeOption));
    if (options.has(encodingOption))
      values.put("talismane.core.encoding", options.valueOf(encodingOption));
    if (options.has(inputEncodingOption))
      values.put("talismane.core.input-encoding", options.valueOf(inputEncodingOption));
    if (options.has(outputEncodingOption))
      values.put("talismane.core.output-encoding", options.valueOf(outputEncodingOption));
    if (options.has(languageModelOption))
      values.put("talismane.core.language-detector.model", options.valueOf(languageModelOption).getPath());
    if (options.has(sentenceModelOption))
      values.put("talismane.core.sentence-detector.model", options.valueOf(sentenceModelOption).getPath());
    if (options.has(tokeniserModelOption))
      values.put("talismane.core.tokeniser.model", options.valueOf(tokeniserModelOption).getPath());
    if (options.has(posTaggerModelOption))
      values.put("talismane.core.pos-tagger.model", options.valueOf(posTaggerModelOption).getPath());
    if (options.has(parserModelOption))
      values.put("talismane.core.parser.model", options.valueOf(parserModelOption).getPath());

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

    if (options.has(crossValidationSizeOption))
      for (String inputLocation : inputLocations)
        values.put(inputLocation + ".cross-validation.fold-count", options.valueOf(crossValidationSizeOption));
    if (options.has(includeIndexOption))
      for (String inputLocation : inputLocations)
        values.put(inputLocation + ".cross-validation.include-index", options.valueOf(includeIndexOption));

    if (options.has(excludeIndexOption))
      for (String inputLocation : inputLocations)
        values.put(inputLocation + ".cross-validation.exclude-index", options.valueOf(excludeIndexOption));

    if (options.has(builtInTemplateOption))
      for (String outputLocation : outputLocations)
        values.put(outputLocation + ".built-in-template", options.valueOf(builtInTemplateOption).name());

    if (options.has(templateOption))
      for (String outputLocation : outputLocations)
        values.put(outputLocation + ".template", options.valueOf(templateOption).getPath());

    if (options.has(posTaggerRulesOption)) {
      List<String> posTaggerRulePaths = options.valuesOf(posTaggerRulesOption).stream().map(f -> f.getPath()).collect(Collectors.toList());
      values.put("talismane.core.pos-tagger.rules", posTaggerRulePaths);
    }
    if (options.has(parserRulesOption)) {
      List<String> parserRulePaths = options.valuesOf(parserRulesOption).stream().map(f -> f.getPath()).collect(Collectors.toList());
      values.put("talismane.core.parser.rules", parserRulePaths);
    }

    if (options.has(suffixOption))
      values.put("talismane.core.suffix", options.valueOf(suffixOption));
    if (options.has(outputDividerOption))
      for (String outputLocation : outputLocations)
        values.put(outputLocation + ".output-divider", options.valueOf(outputDividerOption));

    if (options.has(beamWidthOption)) {
      values.put("talismane.core.pos-tagger.beam-width", options.valueOf(beamWidthOption));
      values.put("talismane.core.parser.beam-width", options.valueOf(beamWidthOption));
    }
    if (options.has(tokeniserBeamWidthOption))
      values.put("talismane.core.tokeniser.beam-width", options.valueOf(tokeniserBeamWidthOption));
    if (options.has(propagateBeamOption))
      values.put("talismane.core.parser.propagate-pos-tagger-beam", options.valueOf(propagateBeamOption));

    if (options.has(maxParseAnalysisTimeOption))
      values.put("talismane.core.parser.max-analysis-time", options.valueOf(maxParseAnalysisTimeOption));
    if (options.has(minFreeMemoryOption))
      values.put("talismane.core.parser.min-free-memory", options.valueOf(minFreeMemoryOption));
    if (options.has(earlyStopOption))
      values.put("talismane.core.parser.early-stop", options.valueOf(earlyStopOption));

    if (options.has(inputPatternFileOption) || options.has(inputPatternOption)) {
      String inputRegex = null;
      if (options.has(inputPatternFileOption)) {
        InputStream inputPatternFile = new FileInputStream(options.valueOf(inputPatternFileOption));
        try (Scanner inputPatternScanner = new Scanner(new BufferedReader(new InputStreamReader(inputPatternFile, "UTF-8")))) {
          if (inputPatternScanner.hasNextLine()) {
            inputRegex = inputPatternScanner.nextLine();
          }
        }
        if (inputRegex == null)
          throw new TalismaneException("No input pattern found in " + options.valueOf(inputPatternFileOption).getPath());
      } else {
        inputRegex = options.valueOf(inputPatternOption);
      }

      for (String inputLocation : inputLocations)
        values.put(inputLocation + ".input-pattern", inputRegex);
    }

    if (options.has(evalPatternFileOption) || options.has(evalPatternOption)) {
      String evalRegex = null;

      if (options.has(evalPatternFileOption)) {
        InputStream evalPatternFile = new FileInputStream(options.valueOf(evalPatternFileOption));
        try (Scanner evalPatternScanner = new Scanner(new BufferedReader(new InputStreamReader(evalPatternFile, "UTF-8")))) {
          if (evalPatternScanner.hasNextLine()) {
            evalRegex = evalPatternScanner.nextLine();
          }
        }
        if (evalRegex == null)
          throw new TalismaneException("No eval pattern found in " + options.valueOf(evalPatternFileOption).getPath());
      } else {
        evalRegex = options.valueOf(evalPatternOption);
      }
      values.put("talismane.core.sentence-detector.evaluate.input-pattern", evalRegex);
      values.put("talismane.core.tokeniser.evaluate.input-pattern", evalRegex);
      values.put("talismane.core.pos-tagger.evaluate.input-pattern", evalRegex);
      values.put("talismane.core.parser.evaluate.input-pattern", evalRegex);
    }

    if (options.has(csvSeparatorOption))
      values.put("talismane.core.csv.separator", options.valueOf(csvSeparatorOption));
    if (options.has(csvEncodingOption))
      values.put("talismane.core.csv.encoding", options.valueOf(csvEncodingOption));
    if (options.has(csvLocaleOption))
      values.put("talismane.core.csv.locale", options.valueOf(csvLocaleOption));

    if (options.has(includeUnknownWordResultsOption))
      values.put("talismane.core.pos-tagger.evaluate.include-unknown-word-results", options.valueOf(includeUnknownWordResultsOption));
    if (options.has(includeLexiconCoverageOption))
      values.put("talismane.core.pos-tagger.evaluate.include-lexicon-coverage", options.valueOf(includeLexiconCoverageOption));

    if (options.has(labeledEvaluationOption))
      values.put("talismane.core.parser.evaluate.labeled-evaluation", options.valueOf(labeledEvaluationOption));
    if (options.has(processingOption))
      values.put("talismane.core.output.option", options.valueOf(processingOption).name());
    if (options.has(lexicalEntryRegexOption)) {
      values.put("talismane.core.pos-tagger.input.corpus-lexical-entry-regex", options.valueOf(lexicalEntryRegexOption).getPath());
      values.put("talismane.core.parser.input.corpus-lexical-entry-regex", options.valueOf(lexicalEntryRegexOption).getPath());
    }

    if (options.has(featuresOption)) {
      values.put("talismane.core.language-detector.train.features", options.valueOf(featuresOption).getPath());
      values.put("talismane.core.sentence-detector.train.features", options.valueOf(featuresOption).getPath());
      values.put("talismane.core.tokeniser.train.features", options.valueOf(featuresOption).getPath());
      values.put("talismane.core.pos-tagger.train.features", options.valueOf(featuresOption).getPath());
      values.put("talismane.core.parser.train.features", options.valueOf(featuresOption).getPath());
    }
    if (options.has(tokeniserPatternsOption))
      values.put("talismane.core.tokeniser.train.patterns", options.valueOf(tokeniserPatternsOption).getPath());
    if (options.has(sentenceFileOption)) {
      values.put("talismane.core.tokeniser.input.sentence-file", options.valueOf(sentenceFileOption).getPath());
      values.put("talismane.core.pos-tagger.input.sentence-file", options.valueOf(sentenceFileOption).getPath());
      values.put("talismane.core.parser.input.sentence-file", options.valueOf(sentenceFileOption).getPath());
    }
    if (options.has(languageCorpusMapOption))
      values.put("talismane.core.language-detector.train.language-corpus-map", options.valueOf(languageCorpusMapOption).getPath());
    if (options.has(predictTransitionsOption))
      values.put("talismane.core.parser.input.predict-transitions", options.valueOf(predictTransitionsOption));
    if (options.has(testWordsOption))
      values.put("talismane.core.pos-tagger.output.test-words", options.valuesOf(testWordsOption));

    if (options.has(algorithmOption)) {
      values.put("talismane.machine-learning.algorithm", options.valueOf(algorithmOption).name());
      values.put("talismane.core.language-detector.train.machine-learning.algorithm", options.valueOf(algorithmOption).name());
      values.put("talismane.core.sentence-detector.train.machine-learning.algorithm", options.valueOf(algorithmOption).name());
      values.put("talismane.core.tokeniser.train.machine-learning.algorithm", options.valueOf(algorithmOption).name());
      values.put("talismane.core.pos-tagger.train.machine-learning.algorithm", options.valueOf(algorithmOption).name());
      values.put("talismane.core.parser.train.machine-learning.algorithm", options.valueOf(algorithmOption).name());
    }
    if (options.has(cutoffOption)) {
      values.put("talismane.machine-learning.cutoff", options.valueOf(cutoffOption));
      values.put("talismane.core.language-detector.train.machine-learning.cutoff", options.valueOf(cutoffOption));
      values.put("talismane.core.sentence-detector.train.machine-learning.cutoff", options.valueOf(cutoffOption));
      values.put("talismane.core.tokeniser.train.machine-learning.cutoff", options.valueOf(cutoffOption));
      values.put("talismane.core.pos-tagger.train.machine-learning.cutoff", options.valueOf(cutoffOption));
      values.put("talismane.core.parser.train.machine-learning.cutoff", options.valueOf(cutoffOption));
    }
    if (options.has(linearSVMEpsilonOption)) {
      values.put("talismane.machine-learning.LinearSVM.epsilon", options.valueOf(linearSVMEpsilonOption));
      values.put("talismane.core.language-detector.train.machine-learning.LinearSVM.epsilon", options.valueOf(linearSVMEpsilonOption));
      values.put("talismane.core.sentence-detector.train.machine-learning.LinearSVM.epsilon", options.valueOf(linearSVMEpsilonOption));
      values.put("talismane.core.tokeniser.train.machine-learning.LinearSVM.epsilon", options.valueOf(linearSVMEpsilonOption));
      values.put("talismane.core.pos-tagger.train.machine-learning.LinearSVM.epsilon", options.valueOf(linearSVMEpsilonOption));
      values.put("talismane.core.parser.train.machine-learning.LinearSVM.epsilon", options.valueOf(linearSVMEpsilonOption));
    }
    if (options.has(linearSVMCostOption)) {
      values.put("talismane.machine-learning.LinearSVM.cost", options.valueOf(linearSVMCostOption));
      values.put("talismane.core.language-detector.train.machine-learning.LinearSVM.cost", options.valueOf(linearSVMCostOption));
      values.put("talismane.core.sentence-detector.train.machine-learning.LinearSVM.cost", options.valueOf(linearSVMCostOption));
      values.put("talismane.core.tokeniser.train.machine-learning.LinearSVM.cost", options.valueOf(linearSVMCostOption));
      values.put("talismane.core.pos-tagger.train.machine-learning.LinearSVM.cost", options.valueOf(linearSVMCostOption));
      values.put("talismane.core.parser.train.machine-learning.LinearSVM.cost", options.valueOf(linearSVMCostOption));
    }
    if (options.has(oneVsRestOption)) {
      values.put("talismane.machine-learning.LinearSVM.one-vs-rest", options.valueOf(oneVsRestOption));
      values.put("talismane.core.language-detector.train.machine-learning.LinearSVM.one-vs-rest", options.valueOf(oneVsRestOption));
      values.put("talismane.core.sentence-detector.train.machine-learning.LinearSVM.one-vs-rest", options.valueOf(oneVsRestOption));
      values.put("talismane.core.tokeniser.train.machine-learning.LinearSVM.one-vs-rest", options.valueOf(oneVsRestOption));
      values.put("talismane.core.pos-tagger.train.machine-learning.LinearSVM.one-vs-rest", options.valueOf(oneVsRestOption));
      values.put("talismane.core.parser.train.machine-learning.LinearSVM.one-vs-rest", options.valueOf(oneVsRestOption));
    }
    if (options.has(iterationsOption)) {
      values.put("talismane.machine-learning.iterations", options.valueOf(iterationsOption));
      values.put("talismane.core.language-detector.train.machine-learning.iterations", options.valueOf(iterationsOption));
      values.put("talismane.core.sentence-detector.train.machine-learning.iterations", options.valueOf(iterationsOption));
      values.put("talismane.core.tokeniser.train.machine-learning.iterations", options.valueOf(iterationsOption));
      values.put("talismane.core.pos-tagger.train.machine-learning.iterations", options.valueOf(iterationsOption));
      values.put("talismane.core.parser.train.machine-learning.iterations", options.valueOf(iterationsOption));
    }

    if (options.has(logConfigFileSpec))
      LogUtils.configureLogging(options.valueOf(logConfigFileSpec));

    File inFile = null;
    File outFile = null;
    File outDir = null;
    if (options.has(inFileOption))
      inFile = options.valueOf(inFileOption);

    if (options.has(outFileOption))
      outFile = options.valueOf(outFileOption);
    if (options.has(outDirOption))
      outDir = options.valueOf(outDirOption);

    File evalFile = inFile;
    if (options.has(evalFileOption))
      evalFile = options.valueOf(evalFileOption);

    Config config = ConfigFactory.parseMap(values).withFallback(ConfigFactory.load());
    TalismaneMain talismaneMain = new TalismaneMain(config);
    talismaneMain.execute(inFile, outFile, outDir, evalFile);
  }

  public TalismaneMain(Config config) {
    this.config = config;
  }

  /**
   * Execute Talismane based on the configuration provided.
   * 
   * @param inFile
   *          The file or directory to analyse
   * @param outFile
   *          The file or directory to write the analysis.
   * @param outDir
   *          The directory for writing additional output files (other than the
   *          main analysis).
   * @param evalFile
   * @throws IOException
   * @throws ReflectiveOperationException
   * @throws TalismaneException
   *           if attempt is made to start and end on two unsuported modules.
   * @throws SentenceAnnotatorLoadException
   */
  public void execute(File inFile, File outFile, File outDir, File evalFile)
      throws IOException, ReflectiveOperationException, TalismaneException, SentenceAnnotatorLoadException {
    if (LOG.isTraceEnabled())
      LOG.trace(config.root().render());
    long startTime = System.currentTimeMillis();
    String sessionId = "";
    TalismaneSession session = new TalismaneSession(config, sessionId);
    session.setFileForBasename(inFile);

    try {
      switch (session.getCommand()) {
      case analyse: {
        Module startModule = Module.valueOf(config.getString("talismane.core.analysis.start-module"));
        Module endModule = Module.valueOf(config.getString("talismane.core.analysis.end-module"));
        Reader reader = getReader(inFile, true, session);
        Writer writer = getWriter(outFile, inFile, session);

        if (startModule == Module.languageDetector) {
          if (endModule != Module.languageDetector)
            throw new TalismaneException(
                "Talismane does not currently support analysis starting with " + startModule.name() + " and ending with another module.");

          LanguageDetector languageDetector = LanguageDetector.getInstance(session);
          LanguageDetectorProcessor processor = LanguageDetectorProcessor.getProcessor(writer, session);

          SentenceDetectorAnnotatedCorpusReader corpusReader = SentenceDetectorAnnotatedCorpusReader.getCorpusReader(reader,
              config.getConfig("talismane.core.language-detector.input"), session);
          while (corpusReader.hasNextSentence()) {
            String sentence = corpusReader.nextSentence().getText().toString();

            List<WeightedOutcome<Locale>> results = languageDetector.detectLanguages(sentence);
            processor.onNextText(sentence, results);
          }
        } else {
          Talismane talismane = new Talismane(writer, outDir, session);
          talismane.analyse(reader);
        }

        break;
      }
      case train: {
        Reader reader = getReader(inFile, false, session);
        switch (session.getModule()) {
        case languageDetector: {
          LanguageDetectorTrainer trainer = new LanguageDetectorTrainer(session);
          trainer.train();
          break;
        }
        case sentenceDetector: {
          SentenceDetectorTrainer trainer = new SentenceDetectorTrainer(reader, session);
          trainer.train();
          break;
        }
        case tokeniser: {
          PatternTokeniserTrainer trainer = new PatternTokeniserTrainer(reader, session);
          trainer.train();
          break;
        }
        case posTagger: {
          PosTaggerTrainer trainer = new PosTaggerTrainer(reader, session);
          trainer.train();
          break;
        }
        case parser: {
          ParserTrainer trainer = new ParserTrainer(reader, session);
          trainer.train();
          break;
        }
        }
        break;
      }
      case evaluate: {
        Reader reader = getReader(inFile, true, session);

        switch (session.getModule()) {
        case sentenceDetector: {
          SentenceDetectorEvaluator evaluator = new SentenceDetectorEvaluator(reader, outDir, session);
          evaluator.evaluate();
          break;
        }
        case tokeniser: {
          TokeniserEvaluator evaluator = new TokeniserEvaluator(reader, outDir, session);
          evaluator.evaluate();
          break;
        }
        case posTagger: {
          PosTaggerEvaluator evaluator = new PosTaggerEvaluator(reader, outDir, session);
          evaluator.evaluate();
          break;
        }
        case parser: {
          ParserEvaluator evaluator = new ParserEvaluator(reader, outDir, session);
          evaluator.evaluate();
          break;
        }
        default:
          throw new TalismaneException("Command '" + session.getCommand() + "' does not yet support module: " + session.getModule());
        }
        break;
      }
      case compare: {
        Reader reader = getReader(inFile, true, session);
        Reader evalReader = getReader(evalFile, true, session);
        switch (session.getModule()) {
        case tokeniser: {
          TokenComparator comparator = new TokenComparator(reader, evalReader, outDir, session);
          comparator.compare();
          break;
        }
        case posTagger: {
          PosTagComparator comparator = new PosTagComparator(reader, evalReader, outDir, session);
          comparator.evaluate();
          break;
        }
        case parser: {
          ParseComparator comparator = new ParseComparator(reader, evalReader, outDir, session);
          comparator.evaluate();
          break;
        }
        default:
          throw new TalismaneException("Command '" + session.getCommand() + "' does not yet support module: " + session.getModule());
        }
        break;
      }
      case process: {
        Reader reader = getReader(inFile, true, session);
        Writer writer = getWriter(outFile, inFile, session);
        IOException ioException = null;
        switch (session.getModule()) {
        case sentenceDetector: {
          List<SentenceProcessor> processors = SentenceProcessor.getProcessors(writer, outDir, session);

          try {
            SentenceDetectorAnnotatedCorpusReader corpusReader = SentenceDetectorAnnotatedCorpusReader.getCorpusReader(reader,
                config.getConfig("talismane.core.sentence-detector.input"), session);
            while (corpusReader.hasNextSentence()) {
              Sentence sentence = corpusReader.nextSentence();
              for (SentenceProcessor processor : processors)
                processor.onNextSentence(sentence);
            }
          } finally {
            for (SentenceProcessor processor : processors) {
              try {
                processor.close();
              } catch (IOException e) {
                LogUtils.logError(LOG, e);
                ioException = e;
              }
            }
          }
          break;
        }
        case tokeniser: {
          List<TokenSequenceProcessor> processors = TokenSequenceProcessor.getProcessors(writer, outDir, session);
          try {
            TokeniserAnnotatedCorpusReader corpusReader = TokeniserAnnotatedCorpusReader.getCorpusReader(reader,
                config.getConfig("talismane.core.tokeniser.input"), session);
            while (corpusReader.hasNextSentence()) {
              TokenSequence tokenSequence = corpusReader.nextTokenSequence();
              for (TokenSequenceProcessor processor : processors)
                processor.onNextTokenSequence(tokenSequence);
            }
          } finally {
            for (TokenSequenceProcessor processor : processors) {
              try {
                processor.close();
              } catch (IOException e) {
                LogUtils.logError(LOG, e);
                ioException = e;
              }
            }
          }
          break;
        }
        case posTagger: {
          List<PosTagSequenceProcessor> processors = PosTagSequenceProcessor.getProcessors(writer, outDir, session);

          try {
            PosTagAnnotatedCorpusReader corpusReader = PosTagAnnotatedCorpusReader.getCorpusReader(reader, config.getConfig("talismane.core.pos-tagger.input"),
                session);
            while (corpusReader.hasNextSentence()) {
              PosTagSequence posTagSequence = corpusReader.nextPosTagSequence();
              for (PosTagSequenceProcessor processor : processors)
                processor.onNextPosTagSequence(posTagSequence);
            }
          } finally {
            for (PosTagSequenceProcessor processor : processors) {
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
          List<ParseConfigurationProcessor> processors = ParseConfigurationProcessor.getProcessors(writer, outDir, session);

          try {
            ParserAnnotatedCorpusReader corpusReader = ParserAnnotatedCorpusReader.getCorpusReader(reader, config.getConfig("talismane.core.parser.input"),
                session);
            while (corpusReader.hasNextSentence()) {
              ParseConfiguration configuration = corpusReader.nextConfiguration();
              for (ParseConfigurationProcessor processor : processors)
                processor.onNextParseConfiguration(configuration);
            }
          } finally {
            for (ParseConfigurationProcessor processor : processors) {
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
        break;
      }
      }

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

  public static Reader getReader(File file, boolean forAnalysis, TalismaneSession session) throws IOException {
    if (file == null)
      return new BufferedReader(new InputStreamReader(System.in, session.getInputCharset()));

    if (!file.exists())
      throw new FileNotFoundException("File does not exist: " + file.getPath());

    if (file.isDirectory()) {
      DirectoryReader directoryReader = new DirectoryReader(file, session.getInputCharset());
      if (forAnalysis)
        directoryReader.setEndOfFileString("\n" + session.getEndBlockCharacter());
      return directoryReader;
    }

    InputStream inFile = new FileInputStream(file);
    return new BufferedReader(new InputStreamReader(inFile, session.getInputCharset()));
  }

  public static Writer getWriter(File outFile, File inFile, TalismaneSession session) throws IOException {
    if (outFile == null)
      return new BufferedWriter(new OutputStreamWriter(System.out, session.getOutputCharset()));

    if (outFile.isDirectory()) {
      outFile.mkdirs();
      DirectoryWriter directoryWriter = new DirectoryWriter(inFile, outFile, session.getSuffix(), session.getOutputCharset());
      return directoryWriter;
    }

    File outDir = outFile.getParentFile();
    if (outDir != null)
      outDir.mkdirs();
    outFile.delete();
    outFile.createNewFile();

    return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), session.getOutputCharset()));

  }
}
