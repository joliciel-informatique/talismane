package com.joliciel.talismane.tokeniser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.LinguisticRules;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.corpus.AbstractAnnotatedCorpusReader;
import com.joliciel.talismane.corpus.CorpusLine;
import com.joliciel.talismane.corpus.CorpusLine.CorpusElement;
import com.joliciel.talismane.corpus.CorpusLineReader;
import com.joliciel.talismane.corpus.CorpusRule;
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
 * {@link CorpusLineReader}.<br/>
 * 
 * The following placeholders are required:<br/>
 * {@link CorpusElement#TOKEN} <br/>
 * These are included surrounded by % signs on both sides, and without the
 * prefix "CorpusElement."<br/>
 * 
 * Example (note that the regex is applied to one line, so no endline is
 * necessary):
 * 
 * <pre>
 * .+\t%TOKEN%
 * </pre>
 * 
 * @author Assaf Urieli
 *
 */
public class TokenRegexBasedCorpusReader extends AbstractAnnotatedCorpusReader implements TokeniserAnnotatedCorpusReader, CurrentFileObserver {
	private static final Logger LOG = LoggerFactory.getLogger(TokenRegexBasedCorpusReader.class);

	protected PretokenisedSequence tokenSequence = null;

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

	/**
	 * Similar to
	 * {@link TokenRegexBasedCorpusReader#TokenRegexBasedCorpusReader(String,Reader,Config,TalismaneSession)}
	 * , but reads the regex from the setting:
	 * <ul>
	 * <li>preannotated-pattern</li>
	 * </ul>
	 * 
	 * @throws TalismaneException
	 */
	public TokenRegexBasedCorpusReader(Reader reader, Config config, TalismaneSession session) throws IOException, TalismaneException {
		this(config.getString("preannotated-pattern"), reader, config, session);
	}

	/**
	 * Add attributes as specified in the config to the corpus reader.
	 * Recognises the attributes:
	 * <ul>
	 * <li>sentence-file: where to read the correctly formatted sentences</li>
	 * <li>corpus-lexical-entry-regex: how to read the lexical entries, see
	 * {@link RegexLexicalEntryReader}</li>
	 * </ul>
	 * 
	 * @param config
	 *            the local config for this corpus reader (local namespace)
	 * @throws TalismaneException
	 */
	public TokenRegexBasedCorpusReader(String regex, Reader reader, Config config, TalismaneSession session) throws IOException, TalismaneException {
		super(config, session);
		this.regex = regex;
		this.scanner = new Scanner(reader);

		String configPath = "sentence-file";
		if (config.hasPath(configPath)) {
			InputStream sentenceReaderFile = ConfigUtils.getFileFromConfig(config, configPath);
			Reader sentenceFileReader = new BufferedReader(new InputStreamReader(sentenceReaderFile, session.getInputCharset()));
			SentenceDetectorAnnotatedCorpusReader sentenceReader = new SentencePerLineCorpusReader(sentenceFileReader, config, session);
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

		configPath = "corpus-rules";
		List<CorpusRule> corpusRules = new ArrayList<>();
		if (config.hasPath(configPath)) {
			List<? extends Config> ruleConfigs = config.getConfigList(configPath);
			for (Config ruleConfig : ruleConfigs) {
				CorpusRule corpusRule = new CorpusRule(ruleConfig);
				corpusRules.add(corpusRule);
			}
		}
		this.corpusLineReader = new CorpusLineReader(regex, this.getRequiredElements(), corpusRules, lexicalEntryReader, session);
	}

	protected CorpusElement[] getRequiredElements() {
		return new CorpusElement[] { CorpusElement.TOKEN };
	}

	@Override
	public boolean hasNextSentence() throws TalismaneException {
		if (this.getMaxSentenceCount() > 0 && sentenceCount >= this.getMaxSentenceCount()) {
			// we've reached the end, do nothing
		} else {
			while (tokenSequence == null) {
				List<CorpusLine> dataLines = new ArrayList<>();
				if (!this.hasNextLine())
					break;
				while ((this.hasNextLine() || dataLines.size() > 0) && tokenSequence == null) {
					String line = "";
					if (this.hasNextLine())
						line = this.nextLine().replace("\r", "");
					lineNumber++;
					if (line.length() > 0) {
						CorpusLine dataLine = corpusLineReader.read(line, lineNumber);

						dataLines.add(dataLine);

						if (this.lexicalEntryReader != null) {
							WritableLexicalEntry lexicalEntry = new CompactLexicalEntry(lexicalEntrySupport);
							this.lexicalEntryReader.readEntry(line, lexicalEntry);
							dataLine.setLexicalEntry(lexicalEntry);
						}
					} else {
						if (dataLines.size() == 0)
							continue;

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
							dataLines = new ArrayList<>();
							continue;
						}

						this.processSentence(dataLines);
					}
				}
			}
		}
		return (tokenSequence != null);
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

	protected void processSentence(List<CorpusLine> corpusLines) throws TalismaneException {
		Sentence sentence = null;
		if (sentenceReader != null && sentenceReader.hasNextSentence()) {
			sentence = sentenceReader.nextSentence();
		} else {
			LinguisticRules rules = session.getLinguisticRules();
			if (rules == null)
				throw new TalismaneException("Linguistic rules have not been set.");

			String text = "";
			for (CorpusLine corpusLine : corpusLines) {
				String word = corpusLine.getElement(CorpusElement.TOKEN);
				// check if a space should be added before this
				// token

				if (rules.shouldAddSpace(text, word))
					text += " ";
				text += word;
			}
			sentence = new Sentence(text, session);
		}

		for (SentenceAnnotator tokenFilter : session.getSentenceAnnotators()) {
			tokenFilter.annotate(sentence);
		}

		tokenSequence = new PretokenisedSequence(sentence, session);
		for (CorpusLine corpusLine : corpusLines) {
			this.convertToToken(tokenSequence, corpusLine);
		}
		tokenSequence.cleanSlate();
	}

	@Override
	public TokenSequence nextTokenSequence() throws TalismaneException {
		TokenSequence nextSentence = null;
		if (this.hasNextSentence()) {
			nextSentence = tokenSequence;
			this.clearSentence();
		}
		return nextSentence;
	}

	protected void clearSentence() {
		this.tokenSequence = null;
	}

	/**
	 * The regex used to find the tokens.
	 */
	public String getRegex() {
		return regex;
	}

	@Override
	public Map<String, String> getCharacteristics() {
		return super.getCharacteristics();
	}

	@Override
	public Sentence nextSentence() throws TalismaneException {
		return this.nextTokenSequence().getSentence();
	}

	@Override
	public boolean isNewParagraph() {
		return false;
	}

	protected CorpusLineReader getCorpusLineReader() {
		return corpusLineReader;
	}

	protected File getCurrentFile() {
		return currentFile;
	}

	/**
	 * Convert a data line into a token, and add it to the provided token
	 * sequence.
	 * 
	 * @throws TalismaneException
	 */
	protected Token convertToToken(PretokenisedSequence tokenSequence, CorpusLine corpusLine) throws TalismaneException {
		Token token = tokenSequence.addToken(corpusLine.getElement(CorpusElement.TOKEN));
		if (corpusLine.hasElement(CorpusElement.FILENAME))
			token.setFileName(corpusLine.getElement(CorpusElement.FILENAME));
		if (corpusLine.hasElement(CorpusElement.ROW))
			token.setLineNumber(Integer.parseInt(corpusLine.getElement(CorpusElement.ROW)));
		if (corpusLine.hasElement(CorpusElement.COLUMN))
			token.setColumnNumber(Integer.parseInt(corpusLine.getElement(CorpusElement.COLUMN)));
		if (corpusLine.hasElement(CorpusElement.END_ROW))
			token.setLineNumberEnd(Integer.parseInt(corpusLine.getElement(CorpusElement.END_ROW)));
		if (corpusLine.hasElement(CorpusElement.END_COLUMN))
			token.setColumnNumberEnd(Integer.parseInt(corpusLine.getElement(CorpusElement.END_COLUMN)));
		return token;
	}
}
