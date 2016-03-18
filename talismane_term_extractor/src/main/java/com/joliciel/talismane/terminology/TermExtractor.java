package com.joliciel.talismane.terminology;

import java.util.Set;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;

/**
 * A parse configuration processor which extracts terms from each parse configuration.
 * @author Assaf Urieli
 *
 */
public interface TermExtractor extends ParseConfigurationProcessor {

	public enum TerminologyProperty {
		/**
		 * {@link TermExtractor#getNominalTags()}
		 */
		nominalTags,
		/**
		 * {@link TermExtractor#getAdjectivalTags()}
		 */
		adjectivalTags,
		/**
		 * {@link TermExtractor#getDeterminentTags()}
		 */
		determinentTags,
		/**
		 * {@link TermExtractor#getPrepositionalTags()}
		 */
		prepositionalTags,
		/**
		 * {@link TermExtractor#getCoordinationLabels()}
		 */
		coordinationLabels,
		/**
		 * {@link TermExtractor#getTermStopTags()}
		 */
		termStopTags,
		/**
		 * {@link TermExtractor#getNonStandaloneTags()}
		 */
		nonStandaloneTags,
		/**
		 * {@link TermExtractor#getNonStandaloneIfHasDependents()}
		 */
		nonStandaloneIfHasDependents,
		/**
		 * {@link TermExtractor#getNonTopLevelLabels()}
		 */
		nonTopLevelLabels,
		/**
		 * {@link TermExtractor#getZeroDepthLabels()}
		 */
		zeroDepthLabels,
		/**
		 * {@link TermExtractor#getMaxDepth()}
		 */
		maxDepth,
		/**
		 * {@link TermExtractor#getLemmaNumber()}
		 */
		lemmaNumber,
		/**
		 * {@link TermExtractor#getLemmaGender()}
		 */
		lemmaGender
	}
	/**
	 * This term extractor's terminology base.
	 */
	public TerminologyBase getTerminologyBase();

	/**
	 * The maximum depth for term extraction, where including a dependent's dependents gives a depth of 2.
	 * Some dependents are considered "zero-depth" and don't add to the depth - such as conjuncts or determinants.
	 */
	public int getMaxDepth();
	public void setMaxDepth(int maxDepth);

	/**
	 * Dependency labels which don't add to the term's depth.
	 */
	public Set<String> getZeroDepthLabels();
	public void setZeroDepthLabels(Set<String> zeroDepthLabels);

	public String getOutFilePath();
	public void setOutFilePath(String outFilePath);

	public void addTermObserver(TermObserver termObserver);

	/**
	 * A list of tags representing direct modifiers for nouns.
	 */
	public abstract void setAdjectivalTags(Set<String> adjectivalTags);
	public abstract Set<String> getAdjectivalTags();

	/**
	 * A list of tags representign nouns - only noun phrases are currently extracted.
	 */
	public abstract void setNominalTags(Set<String> nominalTags);
	public abstract Set<String> getNominalTags();

	/**
	 * A list of tags representing prepositions, or any other POS which
	 * requires an object to be included.
	 */
	public abstract void setPrepositionalTags(Set<String> prepositionalTags);
	public abstract Set<String> getPrepositionalTags();
	
	public TalismaneSession getTalismaneSession();
	public void setTalismaneSession(TalismaneSession talismaneSession);

	/**
	 * Dependency labels representing coordination.
	 */
	public abstract void setCoordinationLabels(Set<String> coordinationLabels);
	public abstract Set<String> getCoordinationLabels();

	/**
	 * A list of tags which are never extracted as terms on their own.
	 */
	public abstract void setNonStandaloneTags(Set<String> nonStandaloneTags);
	public abstract Set<String> getNonStandaloneTags();

	/**
	 * A list of tags which are never extracted as terms on their own if they have any dependents,
	 * e.g. transitive gerunds.
	 */
	public abstract void setNonStandaloneIfHasDependents(Set<String> nonStandaloneIfHasDependents);
	public abstract Set<String> getNonStandaloneIfHasDependents();

	/**
	 * A list of tags representing determinents, e.g. dependents which must be included with
	 * their parent except when the parent is the (lemmatised) term head.
	 */
	public abstract void setDeterminentTags(Set<String> determinentTags);
	public abstract Set<String> getDeterminentTags();

	/**
	 * The string representation of the lemmatised gender for adjectives (e.g. "m" for French
	 * masculine adjectives).
	 */
	public abstract void setLemmaGender(String lemmaGender);
	public abstract String getLemmaGender();

	/**
	 * The string representation of the lemmatised number for nouns (e.g. "s" for French singular nouns).
	 */
	public abstract void setLemmaNumber(String lemmaNumber);
	public abstract String getLemmaNumber();

	/**
	 * A dependent which, as soon as it is reached, causes the expansion to stop, e.g. relative pronouns or conjugated verbs.
	 * It is assumed that no noun phrase containing these should be considered a term.
	 */
	public abstract void setTermStopTags(Set<String> termStopTags);
	public abstract Set<String> getTermStopTags();

	/**
	 * Dependency labels which should never be included with a term governor - e.g. determinants or coordination.
	 * In other words, we accept the term "apple" but not "apple and orange" or "the apple".
	 */
	public abstract void setNonTopLevelLabels(Set<String> nonTopLevelLabels);
	public abstract Set<String> getNonTopLevelLabels();

}