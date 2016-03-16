package com.joliciel.talismane.tokeniser;

import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;

/**
 * A corpus reader that expects one token per line,
 * and analyses the line content based on a regex.<br/>
 * The regex needs to contain a capturing group indicated by the following strings:<br/>
 * <li>%TOKEN%: the token - note that we assume CoNLL formatting (with underscores for spaces and for empty tokens). The sequence &amp;und; should be used for true underscores.</li>
 * It can optionally contain the following capturing groups as well:<br/>
 * <li>%FILENAME%: the file containing the token</li>
 * <li>%ROW%: the row containing the token</li>
 * <li>%COLUMN%: the column containing the token</li>
 * The token placeholder will be replaced by (.*). Other placeholders will be replaced by (.+) meaning no empty strings allowed.
 * @author Assaf Urieli
 *
 */
public interface TokenRegexBasedCorpusReader extends TokeniserAnnotatedCorpusReader {
	/**
	 * The default regex (if none is set).
	 */
	public static final String DEFAULT_REGEX = ".+\\t%TOKEN%";
	
	/**
	 * The regex used to find the tokens.
	 */
	public String getRegex();
	public void setRegex(String regex);
	
	/**
	 * If provided, will assign sentences with the original white space to the token sequences.
	 */
	public SentenceDetectorAnnotatedCorpusReader getSentenceReader();
	public void setSentenceReader(
			SentenceDetectorAnnotatedCorpusReader sentenceReader);

}