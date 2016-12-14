package com.joliciel.talismane.sentenceAnnotators;

import java.util.List;

/**
 * Within a list of tokens, replaces tokens with alternate text for analysis.
 * Must neither add nor remove any tokens from the list. <br/>
 * Must have a no-parameter constructor.
 * 
 * @author Assaf Urieli
 *
 */
public interface TextReplacer {
	public void replace(List<String> tokens);
}
