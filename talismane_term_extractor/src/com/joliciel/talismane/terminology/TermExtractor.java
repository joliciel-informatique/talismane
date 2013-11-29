package com.joliciel.talismane.terminology;

import java.util.Set;

import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.posTagger.PosTag;

public interface TermExtractor extends ParseConfigurationProcessor {
	/**
	 * This term extractor's terminology base.
	 * @return
	 */
	public TerminologyBase getTerminologyBase();

	public int getMaxDepth();

	public void setMaxDepth(int maxDepth);

	public Set<String> getZeroDepthLabels();

	public void setZeroDepthLabels(Set<String> zeroDepthLabels);

	public String getOutFilePath();

	public void setOutFilePath(String outFilePath);

	public Set<PosTag> getIncludeChildren();

	public Set<PosTag> getIncludeWithParent();

	public void addTermObserver(TermObserver termObserver);

}