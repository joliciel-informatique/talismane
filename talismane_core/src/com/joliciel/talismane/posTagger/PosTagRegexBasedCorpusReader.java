package com.joliciel.talismane.posTagger;

/**
 * A corpus reader that expects one pos-tagged token per line,
 * and analyses the line content based on a regex supplied during construction.
 * The regex needs to contain two capturing groups: one for the token and one for the postag code.
 * These groups need to be indicated in the regex expression by the strings "TOKEN" and "POSTAG". These values will be replaced
 * by (.*) for the token, and (.+) for the postag.
 * @author Assaf Urieli
 *
 */
public interface PosTagRegexBasedCorpusReader extends PosTagAnnotatedCorpusReader {

	public String getRegex();

}