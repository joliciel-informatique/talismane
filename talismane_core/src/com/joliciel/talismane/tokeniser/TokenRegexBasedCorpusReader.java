package com.joliciel.talismane.tokeniser;

/**
 * A corpus reader that expects one token per line,
 * and analyses the line content based on a regex.<br/>
 * The regex needs to contain a single capturing group (in parentheses), representing the token to be captured.<br/>
 * @author Assaf Urieli
 *
 */
public interface TokenRegexBasedCorpusReader extends TokeniserAnnotatedCorpusReader {
	/**
	 * The default regex (if none is set).
	 */
	public static final String DEFAULT_REGEX = ".+\\t(.+)";
	
	/**
	 * The regex used to find the tokens.
	 * @return
	 */
	public String getRegex();
	public void setRegex(String regex);

}