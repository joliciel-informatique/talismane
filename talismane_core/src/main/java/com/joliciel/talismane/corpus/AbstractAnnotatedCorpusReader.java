package com.joliciel.talismane.corpus;

import java.util.LinkedHashMap;
import java.util.Map;

import com.joliciel.talismane.TalismaneSession;
import com.typesafe.config.Config;

public abstract class AbstractAnnotatedCorpusReader implements AnnotatedCorpusReader {

	private final int maxSentenceCount;
	private final int startSentence;
	private final int crossValidationSize;
	private final int includeIndex;
	private final int excludeIndex;
	protected final TalismaneSession session;

	/**
	 * Add attributes as specified in the config to the corpus reader.
	 * Recognises the attributes:
	 * <ul>
	 * <li>sentence-count</li>
	 * <li>start-sentence</li>
	 * <li>cross-validation.fold-count</li>
	 * <li>cross-validation.include-index</li>
	 * <li>cross-validation.exclude-index</li>
	 * </ul>
	 * 
	 * @param config
	 *            the local config for this corpus reader (local namespace)
	 */
	public AbstractAnnotatedCorpusReader(Config config, TalismaneSession session) {
		this.session = session;
		this.maxSentenceCount = config.getInt("sentence-count");
		this.startSentence = config.getInt("start-sentence");
		this.crossValidationSize = config.getInt("cross-validation.fold-count");
		this.includeIndex = config.getInt("cross-validation.include-index");
		this.excludeIndex = config.getInt("cross-validation.exclude-index");
	}

	@Override
	public int getMaxSentenceCount() {
		return maxSentenceCount;
	}

	@Override
	public int getStartSentence() {
		return startSentence;
	}

	@Override
	public int getCrossValidationSize() {
		return crossValidationSize;
	}

	@Override
	public int getIncludeIndex() {
		return includeIndex;
	}

	@Override
	public int getExcludeIndex() {
		return excludeIndex;
	}

	@Override
	public Map<String, String> getCharacteristics() {
		Map<String, String> attributes = new LinkedHashMap<String, String>();

		attributes.put("maxSentenceCount", "" + this.getMaxSentenceCount());
		attributes.put("startSentence", "" + this.getStartSentence());
		attributes.put("crossValidationSize", "" + this.getCrossValidationSize());
		attributes.put("includeIndex", "" + this.getIncludeIndex());
		attributes.put("excludeIndex", "" + this.getExcludeIndex());

		return attributes;
	}

}
