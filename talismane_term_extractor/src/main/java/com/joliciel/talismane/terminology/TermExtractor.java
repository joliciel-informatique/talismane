///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.terminology;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalAttribute;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.DependencyNode;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.posTagger.PosTagOpenClassIndicator;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.filters.UppercaseSeriesFilter;

/**
 * Extracts all noun phrases from a given parse configuration.
 * 
 * @author Assaf Urieli
 *
 */
public class TermExtractor implements ParseConfigurationProcessor {

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

	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(TermExtractor.class);

	private static final int DEFAULT_MAX_DEPTH = 8;
	private int maxDepth = DEFAULT_MAX_DEPTH;
	private static final int TOKEN_BUFFER_FOR_CONTEXT = 6;
	private String outFilePath = null;

	private final TalismaneSession talismaneSession;

	private final TerminologyBase terminologyBase;

	private List<TermObserver> termObservers = new ArrayList<TermObserver>();

	private Set<String> zeroDepthLabels = new HashSet<String>();
	private Set<String> termStopTags = new HashSet<String>();
	private Set<String> nonStandaloneTags = new HashSet<String>();
	private Set<String> nonStandaloneIfHasDependents = new HashSet<String>();
	private Set<String> prepositionalTags = new HashSet<String>();
	private Set<String> nominalTags = new HashSet<String>();
	private Set<String> adjectivalTags = new HashSet<String>();
	private Set<String> determinentTags = new HashSet<String>();
	private Set<String> coordinationLabels = new HashSet<String>();
	private Set<String> nonTopLevelLabels = new HashSet<String>();
	private String lemmaNumber = null;
	private String lemmaGender = null;

	public TermExtractor(TerminologyBase terminologyBase, TalismaneSession talismaneSession) {
		this.terminologyBase = terminologyBase;
		this.talismaneSession = talismaneSession;
	}

	public TermExtractor(TerminologyBase terminologyBase, Map<TerminologyProperty, String> terminologyProperties, TalismaneSession talismaneSession) {
		this(terminologyBase, talismaneSession);
		for (TerminologyProperty key : terminologyProperties.keySet()) {
			String value = terminologyProperties.get(key);

			switch (key) {
			case adjectivalTags:
				adjectivalTags = new HashSet<String>(Arrays.asList(value.split(",")));
				break;
			case coordinationLabels:
				coordinationLabels = new HashSet<String>(Arrays.asList(value.split(",")));
				break;
			case determinentTags:
				determinentTags = new HashSet<String>(Arrays.asList(value.split(",")));
				break;
			case lemmaGender:
				lemmaGender = value;
				break;
			case lemmaNumber:
				lemmaNumber = value;
				break;
			case maxDepth:
				maxDepth = Integer.parseInt(value);
				break;
			case nominalTags:
				nominalTags = new HashSet<String>(Arrays.asList(value.split(",")));
				break;
			case nonStandaloneIfHasDependents:
				nonStandaloneIfHasDependents = new HashSet<String>(Arrays.asList(value.split(",")));
				break;
			case nonStandaloneTags:
				nonStandaloneTags = new HashSet<String>(Arrays.asList(value.split(",")));
				break;
			case nonTopLevelLabels:
				nonTopLevelLabels = new HashSet<String>(Arrays.asList(value.split(",")));
				break;
			case prepositionalTags:
				prepositionalTags = new HashSet<String>(Arrays.asList(value.split(",")));
				break;
			case termStopTags:
				termStopTags = new HashSet<String>(Arrays.asList(value.split(",")));
				break;
			case zeroDepthLabels:
				zeroDepthLabels = new HashSet<String>(Arrays.asList(value.split(",")));
				break;
			}
		}
	}

	@Override
	public void onNextParseConfiguration(ParseConfiguration parseConfiguration, Writer writer) {
		for (TermObserver termObserver : termObservers) {
			termObserver.onNewContext(parseConfiguration.getPosTagSequence().getTokenSequence().getText());
		}

		// find all nouns
		List<PosTaggedToken> nouns = new ArrayList<PosTaggedToken>();
		for (PosTaggedToken posTaggedToken : parseConfiguration.getPosTagSequence()) {
			if (nominalTags.contains(posTaggedToken.getTag().getCode())) {
				nouns.add(posTaggedToken);
			}
		}

		Map<PosTaggedToken, List<Expansion>> expansionsPerNoun = new HashMap<PosTaggedToken, List<Expansion>>();
		for (PosTaggedToken noun : nouns) {
			this.getExpansionStrings(noun, parseConfiguration, 0, expansionsPerNoun);
		} // next noun head
	}

	Set<String> getExpansionStrings(PosTaggedToken noun, ParseConfiguration parseConfiguration, int depth,
			Map<PosTaggedToken, List<Expansion>> expansionsPerNoun) {
		Set<String> nounPhrases = null;
		List<Expansion> expansions = this.getExpansions(noun, parseConfiguration, 0, expansionsPerNoun);

		nounPhrases = new TreeSet<String>();

		for (Expansion expansion : expansions) {
			DependencyNode node = expansion.getNode();
			Term term = terminologyBase.findTerm(expansion.display());
			if (term.isNew()) {
				term.setLexicalWordCount(expansion.getLexicalWordCount());
				term.save();
			}

			Token firstToken = node.getFirstToken().getToken();

			Context context = terminologyBase.findContext(term, firstToken.getFileName(), firstToken.getLineNumber(), firstToken.getColumnNumber());

			if (context.isNew()) {
				Token lastToken = node.getLastToken().getToken();

				context.setEndLineNumber(lastToken.getLineNumberEnd());
				context.setEndColumnNumber(lastToken.getColumnNumberEnd());

				int startIndex = firstToken.getIndex();
				startIndex -= TOKEN_BUFFER_FOR_CONTEXT;
				if (startIndex < 0)
					startIndex = 0;

				int endIndex = lastToken.getIndex();
				endIndex += TOKEN_BUFFER_FOR_CONTEXT;
				if (endIndex >= parseConfiguration.getPosTagSequence().getTokenSequence().size())
					endIndex = parseConfiguration.getPosTagSequence().getTokenSequence().size() - 1;

				Token startToken = parseConfiguration.getPosTagSequence().getTokenSequence().get(startIndex);
				Token endToken = parseConfiguration.getPosTagSequence().getTokenSequence().get(endIndex);
				String textSegment = parseConfiguration.getPosTagSequence().getTokenSequence().getSentence().getText().substring(startToken.getStartIndex(),
						endToken.getEndIndex());
				context.setTextSegment(textSegment);
				context.save();
			}

			term.addContext(context);

			for (TermObserver termObserver : termObservers) {
				termObserver.onNewTerm(term);
			}

			for (Expansion parent : expansion.getParents()) {
				Term parentTerm = terminologyBase.findTerm(parent.display());
				if (parentTerm.isNew()) {
					parentTerm.setLexicalWordCount(parent.getLexicalWordCount());
				}
				parentTerm.addExpansion(term);
				parentTerm.save();
			}

			for (Expansion child : expansion.getChildren()) {
				Term childTerm = terminologyBase.findTerm(child.display());
				if (childTerm.isNew()) {
					childTerm.setLexicalWordCount(child.getLexicalWordCount());
				}
				childTerm.addHead(term);
				childTerm.save();
			}

			terminologyBase.commit();

			String nounPhrase = expansion.display();
			nounPhrases.add(nounPhrase);
		}
		return nounPhrases;
	}

	/**
	 * Get all expansions for this node recursively. Note: we assume in here
	 * that coordinated structures are first-conjunct governed.
	 */
	List<Expansion> getExpansions(PosTaggedToken posTaggedToken, ParseConfiguration parseConfiguration, int depth,
			Map<PosTaggedToken, List<Expansion>> expansionsPerNoun) {
		List<Expansion> expansions = new ArrayList<Expansion>();

		List<Expansion> myExpansions = expansionsPerNoun.get(posTaggedToken);
		if (myExpansions == null) {
			myExpansions = new ArrayList<Expansion>();

			List<PosTaggedToken> dependents = parseConfiguration.getDependents(posTaggedToken);

			DependencyNode kernel = parseConfiguration.getDetachedDependencyNode(posTaggedToken);

			// only add the kernel on its own if it meets certain criteria
			String posTagCode = posTaggedToken.getTag().getCode();
			int numDependents = dependents.size();
			if (!nonStandaloneTags.contains(posTagCode) && !(numDependents > 0 && nonStandaloneIfHasDependents.contains(posTagCode))) {
				myExpansions.add(new Expansion(this, kernel));
			}

			// add the various dependents one at a time, until we hit a
			// dependent that shouldn't be included
			List<PosTaggedToken> leftHandDependents = new ArrayList<PosTaggedToken>();
			List<PosTaggedToken> rightHandDependents = new ArrayList<PosTaggedToken>();
			List<List<Expansion>> leftHandExpansionList = new ArrayList<List<Expansion>>();
			List<List<Expansion>> rightHandExpansionList = new ArrayList<List<Expansion>>();

			for (PosTaggedToken dependent : dependents) {
				// stop when we hit conjugated verbs or pronouns
				// current assumption is these will always be "to the right" of
				// the term candidate
				if (termStopTags.contains(posTagCode)) {
					break;
				}

				// recursively get the expansions for each dependent, and store
				// them either to the left or to the right
				List<Expansion> dependentExpansions = this.getExpansions(dependent, parseConfiguration, depth + 1, expansionsPerNoun);

				if (dependentExpansions.size() > 0) {
					if (dependent.getIndex() < posTaggedToken.getIndex()) {
						leftHandDependents.add(0, dependent);
						leftHandExpansionList.add(0, dependentExpansions);
					} else {
						rightHandDependents.add(dependent);
						rightHandExpansionList.add(dependentExpansions);
					}
				}
			}

			// add expansions from left and right side individually
			for (int i = 0; i < 2; i++) {
				List<List<Expansion>> oneSideExpansionList = leftHandExpansionList;
				if (i == 1)
					oneSideExpansionList = rightHandExpansionList;
				DependencyNode currentNode = kernel.cloneNode();
				DependencyNode biggestNode = null;
				for (List<Expansion> dependentExpansions : oneSideExpansionList) {
					for (Expansion dependentExpansion : dependentExpansions) {
						DependencyNode dependentNode = dependentExpansion.getNode();
						DependencyNode newNode = currentNode.cloneNode();
						newNode.addDependent(dependentNode);

						if (newNode.isContiguous()) {
							int perceivedDepth = newNode.getPerceivedDepth(this.getZeroDepthLabels());

							if (perceivedDepth <= this.getMaxDepth()) {
								Expansion expansion = new Expansion(this, newNode);
								myExpansions.add(expansion);
							}

							biggestNode = newNode;
						}
					}
					if (biggestNode == null)
						break;

					currentNode = biggestNode;
				}
			}

			// add dependents from both sides in combination
			if (leftHandExpansionList.size() > 0 && rightHandExpansionList.size() > 0) {
				// have both right and left-hand expansions
				DependencyNode currentLeftNode = kernel.cloneNode();
				DependencyNode biggestLeftNode = null;
				for (List<Expansion> leftExpansions : leftHandExpansionList) {
					for (Expansion leftExpansion : leftExpansions) {
						DependencyNode leftNode = leftExpansion.getNode();
						DependencyNode currentNode = currentLeftNode.cloneNode();
						currentNode.addDependent(leftNode);
						DependencyNode biggestRightNode = null;
						for (List<Expansion> rightExpansions : rightHandExpansionList) {
							for (Expansion rightExpansion : rightExpansions) {
								DependencyNode rightNode = rightExpansion.getNode();
								DependencyNode newNode = currentNode.cloneNode();
								newNode.addDependent(rightNode);

								if (newNode.isContiguous()) {
									int perceivedDepth = newNode.getPerceivedDepth(this.getZeroDepthLabels());
									if (perceivedDepth <= this.getMaxDepth()) {
										Expansion expansion = new Expansion(this, newNode);
										myExpansions.add(expansion);
									}
									biggestRightNode = rightExpansion.getNode();
								}
							}

							if (biggestRightNode == null)
								break;
							currentNode = currentNode.cloneNode();
							currentNode.addDependent(biggestRightNode);
						}
						biggestLeftNode = leftExpansion.getNode();
					}
					currentLeftNode = currentLeftNode.cloneNode();
					currentLeftNode.addDependent(biggestLeftNode);
				}
			}

			expansionsPerNoun.put(posTaggedToken, myExpansions);
		} // expansions have not yet been calculated for this token

		if (depth == 0) {
			// if it's top-level, we don't want the coordinating structure, nor
			// the determinant
			expansions = new ArrayList<Expansion>();
			for (Expansion expansion : myExpansions) {
				boolean include = true;
				for (DependencyNode child : expansion.getNode().getDependents()) {
					if (nonTopLevelLabels.contains(child.getLabel())) {
						include = false;
						break;
					}
				}
				if (include)
					expansions.add(expansion);
			}
		} else {
			expansions = myExpansions;
		}

		return expansions;
	}

	public static class Expansion {
		String text = null;
		DependencyNode node = null;
		TermExtractor termExtractor;
		TalismaneSession talismaneSession;
		PosTaggerLexicon lexicon;
		List<Expansion> children = null;
		List<Expansion> parents = null;
		Set<PosTaggedToken> tokenSet = null;

		public Expansion(TermExtractor termExtractor, DependencyNode node) {
			super();
			this.node = node;
			this.termExtractor = termExtractor;
			this.talismaneSession = termExtractor.getTalismaneSession();
			this.lexicon = talismaneSession.getMergedLexicon();
		}

		public List<Expansion> getChildren() {
			if (this.children == null) {
				children = new ArrayList<Expansion>();
				for (DependencyNode child : node.getDependents()) {
					String posTagCode = child.getPosTaggedToken().getTag().getCode();
					if (termExtractor.getPrepositionalTags().contains(posTagCode)) {
						if (child.getDependents().size() > 0) {
							DependencyNode realChild = child.getDependents().iterator().next().cloneNode();
							Expansion realChildExpansion = new Expansion(termExtractor, realChild);
							if (realChildExpansion.display() != null && realChildExpansion.display().length() > 0)
								children.add(new Expansion(termExtractor, realChild));
						}
					} else if (termExtractor.getNominalTags().contains(posTagCode)) {
						Expansion childExpansion = new Expansion(termExtractor, child);
						if (childExpansion.display() != null && childExpansion.display().length() > 0)
							children.add(new Expansion(termExtractor, child));
					}
				}
			}
			return children;
		}

		public List<Expansion> getParents() {
			if (this.parents == null) {
				parents = new ArrayList<>();
				List<DependencyNode> leftDependents = new ArrayList<DependencyNode>();
				List<DependencyNode> rightDependents = new ArrayList<DependencyNode>();
				for (DependencyNode child : node.getDependents()) {
					if (child.getPosTaggedToken().getIndex() < node.getPosTaggedToken().getIndex()) {
						leftDependents.add(child);
					} else {
						rightDependents.add(child);
					}
				}
				if (leftDependents.size() > 0) {
					DependencyNode leftParent = node.cloneNode();
					leftParent.removeNode(leftDependents.get(0));
					parents.add(new Expansion(termExtractor, leftParent));
				}
				if (rightDependents.size() > 0) {
					DependencyNode rightParent = node.cloneNode();
					rightParent.removeNode(rightDependents.get(rightDependents.size() - 1));
					parents.add(new Expansion(termExtractor, rightParent));
				}
			}
			return parents;
		}

		public DependencyNode getNode() {
			return node;
		}

		public String display() {
			if (text == null) {
				DependencyNode startNode = node;
				String posTagCode = node.getPosTaggedToken().getTag().getCode();
				if (!(termExtractor.getNominalTags().contains(posTagCode))) {
					return null;
				}

				Set<PosTaggedToken> tokensToDisplay = this.getTokenSet();

				// if top level, return lemma for noun, and bring modifying
				// adjectives to lemmatised form
				LexicalEntry headNounEntry = startNode.getPosTaggedToken().getLexicalEntry();
				boolean lemmatiseHead = false;
				String headNounGender = termExtractor.getLemmaGender();

				if (headNounEntry != null) {
					if (headNounEntry.hasAttribute(LexicalAttribute.Number) && !headNounEntry.getNumber().contains(termExtractor.getLemmaNumber())) {
						lemmatiseHead = true;
						if (headNounEntry.hasAttribute(LexicalAttribute.Gender) && headNounEntry.getGender().size() == 1)
							headNounGender = headNounEntry.getGender().get(0);
					}
				}

				Token lastToken = null;
				String sentence = startNode.getParseConfiguration().getPosTagSequence().getTokenSequence().getText();
				StringBuilder stringBuilder = new StringBuilder();
				for (PosTaggedToken posTaggedToken : tokensToDisplay) {
					Token currentToken = posTaggedToken.getToken();
					String tokenText = currentToken.getOriginalText();
					if (Character.isUpperCase(tokenText.charAt(0))) {
						// lowercase any known words
						tokenText = UppercaseSeriesFilter.getKnownWord(talismaneSession, tokenText);
					}

					if (lemmatiseHead && posTaggedToken.equals(startNode.getPosTaggedToken())) {
						List<? extends LexicalEntry> lemmaFormEntries = lexicon.getEntriesMatchingCriteria(headNounEntry, node.getPosTaggedToken().getTag(),
								null, termExtractor.getLemmaNumber());
						LexicalEntry lemmaFormEntry = null;
						if (lemmaFormEntries.size() > 0)
							lemmaFormEntry = lemmaFormEntries.get(0);

						if (lemmaFormEntry != null)
							tokenText = lemmaFormEntry.getWord();
					} else if (lemmatiseHead && termExtractor.getAdjectivalTags().contains(posTaggedToken.getTag().getCode())) {

						boolean lemmatiseModifier = false;
						if (node.getPosTaggedToken().equals(startNode.getParseConfiguration().getHead(posTaggedToken))) {
							lemmatiseModifier = true;
						} else {
							// handle coordination as well - find the parent of
							// the entire structure
							DependencyArc arc = startNode.getParseConfiguration().getGoverningDependency(posTaggedToken);
							if (arc != null) {
								PosTaggedToken parent = arc.getHead();
								while (arc != null && termExtractor.getCoordinationLabels().contains(arc.getLabel())) {
									arc = startNode.getParseConfiguration().getGoverningDependency(parent);
									parent = arc.getHead();
								}
								if (node.getPosTaggedToken().equals(parent))
									lemmatiseModifier = true;
							}
						}
						if (lemmatiseModifier) {
							LexicalEntry pluralEntry = posTaggedToken.getLexicalEntry();
							if (pluralEntry != null && !pluralEntry.getNumber().contains(termExtractor.getLemmaNumber())) {
								List<? extends LexicalEntry> lemmaFormEntries = lexicon.getEntriesMatchingCriteria(pluralEntry, posTaggedToken.getTag(),
										headNounGender, termExtractor.getLemmaNumber());
								LexicalEntry lemmaFormEntry = null;
								if (lemmaFormEntries.size() > 0)
									lemmaFormEntry = lemmaFormEntries.get(0);

								if (lemmaFormEntry != null)
									tokenText = lemmaFormEntry.getWord();
							}

							if (pluralEntry == null) {
								tokenText = this.talismaneSession.getLinguisticRules().makeAdjectiveSingular(tokenText);
							}
						}
					} // is this some sort of plural entry that needs to be
						// singularised?

					if (lastToken == null) {
						stringBuilder.append(tokenText);
					} else if (currentToken.getIndex() - lastToken.getIndex() == 1) {
						stringBuilder.append(sentence.substring(lastToken.getEndIndex(), currentToken.getStartIndex()));
						stringBuilder.append(tokenText);
					} else {
						stringBuilder.append(" ");
						stringBuilder.append(tokenText);
					}
					lastToken = currentToken;
				}
				text = stringBuilder.toString().trim();
			}
			return text;
		}

		@Override
		public String toString() {
			return this.node.toString();
		}

		Set<PosTaggedToken> getTokenSet() {
			if (tokenSet == null) {
				tokenSet = new TreeSet<PosTaggedToken>();
				this.collectNodesForDisplay(this.node, tokenSet, 1);
			}
			return tokenSet;
		}

		void collectNodesForDisplay(DependencyNode node, Set<PosTaggedToken> tokensToDisplay, int depth) {
			if (this.shouldDisplay(node, depth))
				tokensToDisplay.add(node.getPosTaggedToken());
			for (DependencyNode child : node.getDependents()) {
				String posTagCode = child.getPosTaggedToken().getTag().getCode();
				int newDepth = depth + 1;
				if (termExtractor.getDeterminentTags().contains(posTagCode))
					newDepth = depth;
				this.collectNodesForDisplay(child, tokensToDisplay, newDepth);
			}
		}

		boolean shouldDisplay(DependencyNode node, int depth) {
			String posTagCode = node.getPosTaggedToken().getTag().getCode();
			if (depth == 1 && termExtractor.getDeterminentTags().contains(posTagCode)) {
				return false;
			}

			int numRealDependents = node.getParseConfiguration().getDependents(node.getPosTaggedToken()).size();
			int numAttachedDependents = node.getDependents().size();
			if (numAttachedDependents == 0 && (termExtractor.getNonStandaloneTags().contains(posTagCode))
					|| (numRealDependents > 0 && termExtractor.getNonStandaloneIfHasDependents().contains(posTagCode))) {
				return false;
			}
			return true;
		}

		public int getLexicalWordCount() {
			int lexicalWordCount = 0;
			for (PosTaggedToken posTaggedToken : this.getTokenSet()) {
				if (posTaggedToken.getTag().getOpenClassIndicator() == PosTagOpenClassIndicator.OPEN) {
					lexicalWordCount++;
				}
			}
			return lexicalWordCount;
		}
	}

	/**
	 * This term extractor's terminology base.
	 */
	public TerminologyBase getTerminologyBase() {
		return terminologyBase;
	}

	@Override
	public void onCompleteParse() {
		// nothing to do for now
	}

	/**
	 * The maximum depth for term extraction, where including a dependent's
	 * dependents gives a depth of 2. Some dependents are considered
	 * "zero-depth" and don't add to the depth - such as conjuncts or
	 * determinants.
	 */
	public int getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	/**
	 * Dependency labels which don't add to the term's depth.
	 */
	public Set<String> getZeroDepthLabels() {
		if (zeroDepthLabels == null) {
			zeroDepthLabels = new HashSet<String>();
		}
		return zeroDepthLabels;
	}

	public void setZeroDepthLabels(Set<String> zeroDepthLabels) {
		this.zeroDepthLabels = zeroDepthLabels;
	}

	public String getOutFilePath() {
		return outFilePath;
	}

	public void setOutFilePath(String outFilePath) {
		this.outFilePath = outFilePath;
	}

	public void addTermObserver(TermObserver termObserver) {
		this.termObservers.add(termObserver);
	}

	/**
	 * A list of tags representing prepositions, or any other POS which requires
	 * an object to be included.
	 */
	public Set<String> getPrepositionalTags() {
		return prepositionalTags;
	}

	public void setPrepositionalTags(Set<String> prepositionalTags) {
		this.prepositionalTags = prepositionalTags;
	}

	/**
	 * A list of tags representing nouns - only noun phrases are currently
	 * extracted.
	 */
	public Set<String> getNominalTags() {
		return nominalTags;
	}

	public void setNominalTags(Set<String> nominalTags) {
		this.nominalTags = nominalTags;
	}

	/**
	 * A list of tags representing direct modifiers for nouns.
	 */
	public Set<String> getAdjectivalTags() {
		return adjectivalTags;
	}

	public void setAdjectivalTags(Set<String> adjectivalTags) {
		this.adjectivalTags = adjectivalTags;
	}

	public TalismaneSession getTalismaneSession() {
		return talismaneSession;
	}

	/**
	 * A list of tags which are never extracted as terms on their own.
	 */
	public Set<String> getNonStandaloneTags() {
		return nonStandaloneTags;
	}

	public void setNonStandaloneTags(Set<String> nonStandaloneTags) {
		this.nonStandaloneTags = nonStandaloneTags;
	}

	/**
	 * A list of tags which are never extracted as terms on their own if they
	 * have any dependents, e.g. transitive gerunds.
	 */
	public Set<String> getNonStandaloneIfHasDependents() {
		return nonStandaloneIfHasDependents;
	}

	public void setNonStandaloneIfHasDependents(Set<String> nonStandaloneIfHasDependents) {
		this.nonStandaloneIfHasDependents = nonStandaloneIfHasDependents;
	}

	/**
	 * Dependency labels representing coordination.
	 */
	public Set<String> getCoordinationLabels() {
		return coordinationLabels;
	}

	public void setCoordinationLabels(Set<String> coordinationLabels) {
		this.coordinationLabels = coordinationLabels;
	}

	/**
	 * A list of tags representing determinents, e.g. dependents which must be
	 * included with their parent except when the parent is the (lemmatised)
	 * term head.
	 */
	public Set<String> getDeterminentTags() {
		return determinentTags;
	}

	public void setDeterminentTags(Set<String> determinentTags) {
		this.determinentTags = determinentTags;
	}

	/**
	 * The string representation of the lemmatised number for nouns (e.g. "s"
	 * for French singular nouns).
	 */
	public String getLemmaNumber() {
		return lemmaNumber;
	}

	public void setLemmaNumber(String lemmaNumber) {
		this.lemmaNumber = lemmaNumber;
	}

	/**
	 * The string representation of the lemmatised gender for adjectives (e.g.
	 * "m" for French masculine adjectives).
	 */
	public String getLemmaGender() {
		return lemmaGender;
	}

	public void setLemmaGender(String lemmaGender) {
		this.lemmaGender = lemmaGender;
	}

	/**
	 * A dependent which, as soon as it is reached, causes the expansion to
	 * stop, e.g. relative pronouns or conjugated verbs. It is assumed that no
	 * noun phrase containing these should be considered a term.
	 */
	public Set<String> getTermStopTags() {
		return termStopTags;
	}

	public void setTermStopTags(Set<String> termStopTags) {
		this.termStopTags = termStopTags;
	}

	/**
	 * Dependency labels which should never be included with a term governor -
	 * e.g. determinants or coordination. In other words, we accept the term
	 * "apple" but not "apple and orange" or "the apple".
	 */
	public Set<String> getNonTopLevelLabels() {
		return nonTopLevelLabels;
	}

	public void setNonTopLevelLabels(Set<String> nonTopLevelLabels) {
		this.nonTopLevelLabels = nonTopLevelLabels;
	}

}
