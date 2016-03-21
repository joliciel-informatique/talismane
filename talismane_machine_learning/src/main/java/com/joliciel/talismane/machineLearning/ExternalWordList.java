package com.joliciel.talismane.machineLearning;

import java.io.Serializable;
import java.util.List;

/**
 * A word-list stored in an external resource.
 * @author Assaf Urieli
 *
 */
public interface ExternalWordList extends Serializable {
	/**
	 * A unique name for this resource.
	 */
	public String getName();
	
	/**
	 * The word list itself.
	 */
	List<String> getWordList();
}
