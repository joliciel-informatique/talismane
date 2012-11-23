package com.joliciel.talismane.tokeniser;

/**
 * A corpus reader that expects one token per line,
 * and analyses the line content based on a regex.<br/>
 * The regex needs to contain a capturing group indicated by the following strings:<br/>
 * <li>%TOKEN%: the token</li>
 * It can optionally contain the following capturing groups as well:<br/>
 * <li>%FILENAME%: the file containing the token</li>
 * <li>%ROW%: the row containing the token</li>
 * <li>%COLUMN%: the column containing the token</li>
 * The strings will (.*) for the token, and (.+) for all others.
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
	 * @return
	 */
	public String getRegex();
	public void setRegex(String regex);

}