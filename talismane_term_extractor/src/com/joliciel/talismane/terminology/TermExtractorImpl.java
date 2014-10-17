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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.parser.DependencyNode;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * Extracts all noun phrases from a given parse configuration.
 * @author Assaf Urieli
 *
 */
class TermExtractorImpl implements TermExtractor {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(TermExtractorImpl.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(TermExtractorImpl.class);

	private static final int DEFAULT_MAX_DEPTH = 8;
	private int maxDepth = DEFAULT_MAX_DEPTH;
	private static final int TOKEN_BUFFER_FOR_CONTEXT = 6;
	private String outFilePath = null;
	
	private TerminologyService terminologyService;
	private TalismaneService talismaneService;
	private PosTaggerLexicon lexicon;
	
	private TerminologyBase terminologyBase;
	private Set<String> zeroDepthLabels;
	private List<TermObserver> termObservers = new ArrayList<TermObserver>();
	
	Set<PosTag> includeChildren = new HashSet<PosTag>();
	Set<PosTag> includeWithParent = new HashSet<PosTag>();
	
	public TermExtractorImpl(TerminologyBase terminologyBase) {
		this.terminologyBase = terminologyBase;
	}

	@Override
	public void onNextParseConfiguration(ParseConfiguration parseConfiguration, Writer writer) {
		MONITOR.startTask("onNextParseConfiguration");
		try {
			for (TermObserver termObserver : termObservers) {
				termObserver.onNewContext(parseConfiguration.getPosTagSequence().getTokenSequence().getText());
			}
			
			// find all nouns
			List<PosTaggedToken> nouns = new ArrayList<PosTaggedToken>();
			for (PosTaggedToken posTaggedToken : parseConfiguration.getPosTagSequence()) {
				if (posTaggedToken.getTag().getCode().equals("NC")||posTaggedToken.getTag().getCode().equals("NPP")) {
					nouns.add(posTaggedToken);
				}
			}
			
			Map<PosTaggedToken, List<Expansion>> expansionsPerNoun = new HashMap<PosTaggedToken, List<Expansion>>();
			for (PosTaggedToken noun : nouns) {
				this.getExpansionStrings(noun, parseConfiguration, 0, expansionsPerNoun);
			} // next noun head
		} finally {
			MONITOR.endTask();
		}
	}
	
	Set<String> getExpansionStrings(PosTaggedToken noun, ParseConfiguration parseConfiguration, int depth, Map<PosTaggedToken, List<Expansion>> expansionsPerNoun) {
		Set<String> nounPhrases = null;
		List<Expansion> expansions = null;
		MONITOR.startTask("find expansions");
		try {
			expansions = this.getExpansions(noun, parseConfiguration, 0, expansionsPerNoun);
		} finally {
			MONITOR.endTask();
		}
		MONITOR.startTask("find phrases");
		try {
			nounPhrases = new TreeSet<String>();
			
			for (Expansion expansion : expansions) {
				DependencyNode node = expansion.getNode();
				Term term = terminologyBase.getTerm(expansion.display());
				
				Context context = terminologyBase.getContext(node.getFirstToken().getToken().getFileName(),
						node.getFirstToken().getToken().getLineNumber(),
						node.getFirstToken().getToken().getColumnNumber());
				int startIndex = node.getFirstToken().getToken().getIndex();
				startIndex -= TOKEN_BUFFER_FOR_CONTEXT;
				if (startIndex < 0)
					startIndex = 0;
				
				int endIndex = node.getLastToken().getToken().getIndex();
				endIndex += TOKEN_BUFFER_FOR_CONTEXT;
				if (endIndex >= parseConfiguration.getPosTagSequence().getTokenSequence().size())
					endIndex = parseConfiguration.getPosTagSequence().getTokenSequence().size()-1;
				
				Token startToken = parseConfiguration.getPosTagSequence().getTokenSequence().get(startIndex);
				Token endToken = parseConfiguration.getPosTagSequence().getTokenSequence().get(endIndex);
				String textSegment = parseConfiguration.getPosTagSequence().getTokenSequence().getSentence().getText().substring(startToken.getStartIndex(), endToken.getEndIndex());
				context.setTextSegment(textSegment);
				term.addContext(context);
				for (TermObserver termObserver : termObservers) {
					termObserver.onNewTerm(term);
				}
				terminologyBase.storeTerm(term);
				terminologyBase.storeContext(context);
				
				for (Expansion parent : expansion.getParents()) {
					Term parentTerm = terminologyBase.getTerm(parent.display());
					parentTerm.addExpansion(term);
					terminologyBase.storeTerm(parentTerm);
				}
				
				for (Expansion child : expansion.getChildren()) {
					Term childTerm = terminologyBase.getTerm(child.display());
					childTerm.addHead(term);
					terminologyBase.storeTerm(childTerm);
				}

				// once more to add the heads
				terminologyBase.storeTerm(term);
				
				terminologyBase.commit();
				
				String nounPhrase = expansion.display();
				nounPhrases.add(nounPhrase);
			}
			return nounPhrases;
		} finally {
			MONITOR.endTask();
		}
	}
	
	/**
	 * Get all expansions for this node.
	 * Note: we assume in here that, for a coordinated structure, the coordinator depends on the first node in the structure, and all other
	 * coordinants depend on the coordinator. This is the case for the ftbDep corpus by Crabbé and Candito.
	 * For a training corpus where the coordinator is the governor for the full structure, this will have to be generalised somehow.
	 * @param posTaggedToken
	 * @param parseConfiguration
	 * @param depth
	 * @param expansionsPerNoun
	 * @return
	 */
	List<Expansion> getExpansions(PosTaggedToken posTaggedToken, ParseConfiguration parseConfiguration, int depth, Map<PosTaggedToken, List<Expansion>> expansionsPerNoun) {
		List<Expansion> expansions = new ArrayList<Expansion>();
		
		List<Expansion> myExpansions = expansionsPerNoun.get(posTaggedToken);
		if (myExpansions==null) {
			myExpansions = new ArrayList<Expansion>();
			
			List<PosTaggedToken> dependents = parseConfiguration.getDependents(posTaggedToken);

			DependencyNode kernel = parseConfiguration.getDetachedDependencyNode(posTaggedToken);
			
			// only add the kernel on its own if it meets certain criteria
			String posTagCode = posTaggedToken.getTag().getCode();
			int numDependents = dependents.size();
			if (!(posTagCode.equals("P") || posTagCode.equals("CC") || posTagCode.equals("CS") || posTagCode.equals("PONCT")|| posTagCode.equals("P+D"))
					&& !(numDependents>0 && (posTagCode.equals("VPR")))) {
				myExpansions.add(new Expansion(lexicon, kernel));
			}
			
			// add the various dependents one at a time, until we hit a dependent that shouldn't be included
			List<PosTaggedToken> leftHandDependents = new ArrayList<PosTaggedToken>();
			List<PosTaggedToken> rightHandDependents = new ArrayList<PosTaggedToken>();
			List<List<Expansion>> leftHandExpansionList = new ArrayList<List<Expansion>>();
			List<List<Expansion>> rightHandExpansionList = new ArrayList<List<Expansion>>();
			
			for (PosTaggedToken dependent : dependents) {
				// stop when we hit conjugated verbs or pronouns
				// current assumption is these will always be "to the right" of the term candidate
				if (posTagCode.equals("V")||posTagCode.equals("VS")
						||posTagCode.equals("VIMP")||posTagCode.equals("PRO")||posTagCode.equals("P+PRO")
						||posTagCode.equals("PROREL")||posTagCode.equals("PROWH")
						||posTagCode.equals("PONCT")) {
					break;
				}
				
				List<Expansion> dependentExpansions = this.getExpansions(dependent, parseConfiguration, depth+1, expansionsPerNoun);
				
				if (dependentExpansions.size()>0) {
					if (dependent.getIndex()<posTaggedToken.getIndex()) {
						leftHandDependents.add(0, dependent);
						leftHandExpansionList.add(0, dependentExpansions);
					} else {
						rightHandDependents.add(dependent);
						rightHandExpansionList.add(dependentExpansions);
					}
				}
			}
			
			// add expansions from left and right side individually
			for (int i=0; i<2; i++) {
				List<List<Expansion>> oneSideExpansionList = leftHandExpansionList;
				if (i==1)
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
							
							if (perceivedDepth<=this.getMaxDepth()) {
								Expansion expansion = new Expansion(lexicon, newNode);
								myExpansions.add(expansion);
							}
							
							biggestNode = newNode;
						}
					}
					if (biggestNode==null)
						break;
					
					currentNode = biggestNode;
				}
			}
			
			// add dependents from both sides in combination
			if (leftHandExpansionList.size()>0 && rightHandExpansionList.size()>0) {
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
									if (perceivedDepth<=this.getMaxDepth()) {
										Expansion expansion = new Expansion(lexicon, newNode);										
										myExpansions.add(expansion);
									}
									biggestRightNode = rightExpansion.getNode();
								}
							}
							
							if (biggestRightNode==null)
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

		
		if (depth==0) {
			// if it's top-level, we don't want the coordinating structure, nor the determinant
			expansions = new ArrayList<Expansion>();
			for (Expansion expansion : myExpansions) {
				boolean include = true;
				boolean firstChild = true;
				for (DependencyNode child : expansion.getNode().getDependents()) {
					if (firstChild && child.getLabel().equals("det")) {
						include = false;
						break;
					}
					if (child.getLabel().equals("coord")) {
						include = false;
						break;
					}
					firstChild = false;
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
		String string = null;
		DependencyNode node = null;
		PosTaggerLexicon lexicon;
		List<Expansion> children = null;
		List<Expansion> parents = null;
		
		public Expansion(PosTaggerLexicon lexicon, DependencyNode node) {
			super();
			this.node = node;
			this.lexicon = lexicon;
		}
		
		public List<Expansion> getChildren() {
			if (this.children==null) {
				children = new ArrayList<TermExtractorImpl.Expansion>();
				for (DependencyNode child : node.getDependents()) {
					String posTagCode = child.getPosTaggedToken().getTag().getCode();
					if (posTagCode.equals("P") || posTagCode.equals("P+D")) {
						if (child.getDependents().size()>0) {
							DependencyNode realChild = child.getDependents().iterator().next().cloneNode();
							Expansion realChildExpansion = new Expansion(lexicon, realChild);
							if (realChildExpansion.display()!=null && realChildExpansion.display().length()>0)
								children.add(new Expansion(lexicon, realChild));
						}
					} else if (posTagCode.equals("NC") || posTagCode.equals("NPP")) {
						Expansion childExpansion = new Expansion(lexicon, child);
						if (childExpansion.display()!=null && childExpansion.display().length()>0)
							children.add(new Expansion(lexicon, child));
					}
				}
			}
			return children;
		}

		public List<Expansion> getParents() {
			if (this.parents==null) {
				parents = new ArrayList<TermExtractorImpl.Expansion>();
				List<DependencyNode> leftDependents = new ArrayList<DependencyNode>();
				List<DependencyNode> rightDependents = new ArrayList<DependencyNode>();
				for (DependencyNode child : node.getDependents()) {
					if (child.getPosTaggedToken().getIndex()<node.getPosTaggedToken().getIndex()) {
						leftDependents.add(child);
					} else {
						rightDependents.add(child);
					}
				}
				if (leftDependents.size()>0) {
					DependencyNode leftParent = node.cloneNode();
					leftParent.removeNode(leftDependents.get(0));
					parents.add(new Expansion(lexicon, leftParent));
				}
				if (rightDependents.size()>0) {
					DependencyNode rightParent = node.cloneNode();
					rightParent.removeNode(rightDependents.get(rightDependents.size()-1));
					parents.add(new Expansion(lexicon, rightParent));
				}
			}
			return parents;
		}

		public DependencyNode getNode() {
			return node;
		}
		
		public String display() {
			if (string==null) {
				string = this.displayInternal();
			}
			return string;
		}
		
		public String toString() {
			return this.node.toString();
		}
		
		String displayInternal() {
			DependencyNode startNode = node;
			String posTagCode = node.getPosTaggedToken().getTag().getCode();
			if (!(posTagCode.equals("NC")||posTagCode.equals("NPP"))) {
				return null;
			}

			Set<PosTaggedToken> tokensToDisplay = new TreeSet<PosTaggedToken>();
			
			this.collectNodesForDisplay(startNode, tokensToDisplay, 1);
			
			//if top level, return lemma for noun, and bring adjectives to singular form
			LexicalEntry headNounEntry = startNode.getPosTaggedToken().getLexicalEntry();
			boolean nounIsPlural = false;
			String headNounGender = "m";
			
			if (headNounEntry!=null) {
				if (headNounEntry.getNumber().size()==1 && headNounEntry.getNumber().get(0).equals("p")) {
					nounIsPlural = true;
					if (headNounEntry.getGender().size()==1)
						headNounGender = headNounEntry.getGender().get(0);
				}
			}
			
			Token lastToken = null;
			String sentence = startNode.getParseConfiguration().getPosTagSequence().getTokenSequence().getText();
			StringBuilder stringBuilder = new StringBuilder();
			for (PosTaggedToken posTaggedToken : tokensToDisplay) {
				Token currentToken = posTaggedToken.getToken();
				String tokenText = currentToken.getText();
				if (tokenText.equals("31")||tokenText.equals("999")||tokenText.equals("9,99")||tokenText.equals("1999"))
					tokenText = currentToken.getOriginalText();
				else if (currentToken.getOriginalText().length()==0)
					tokenText = currentToken.getOriginalText();
				else if (tokenText.equals("lequel")||tokenText.equals("lesquels")||tokenText.equals("lesquelles"))
					tokenText = currentToken.getOriginalText();

				if (nounIsPlural && posTaggedToken.equals(startNode.getPosTaggedToken())) {
					List<? extends LexicalEntry> singularEntries = lexicon.getEntriesMatchingCriteria(headNounEntry, node.getPosTaggedToken().getTag(), null, "s");
					LexicalEntry singularEntry = null;
					if (singularEntries.size()>0)
						singularEntry = singularEntries.get(0);
					
					if (singularEntry!=null)
						tokenText = singularEntry.getWord();
				} else if (nounIsPlural && node.getPosTaggedToken().equals(startNode.getParseConfiguration().getHead(posTaggedToken)) && 
						(posTaggedToken.getTag().getCode().equals("ADJ")||posTaggedToken.getTag().getCode().equals("VPP"))) {
					LexicalEntry pluralEntry = posTaggedToken.getLexicalEntry();
					if (pluralEntry!=null && !pluralEntry.getNumber().contains("s")) {
						List<? extends LexicalEntry> singularEntries = lexicon.getEntriesMatchingCriteria(pluralEntry, posTaggedToken.getTag(), headNounGender, "s");
						LexicalEntry singularEntry = null;
						if (singularEntries.size()>0)
							singularEntry = singularEntries.get(0);
						
						if (singularEntry!=null)
							tokenText = singularEntry.getWord();
					}
					
					if (pluralEntry==null) {
						if (tokenText.endsWith("aux")) {
							tokenText = tokenText.substring(0, tokenText.length()-3) + "al";
						} else if (tokenText.endsWith("s")) {
							tokenText = tokenText.substring(0, tokenText.length()-1);
						}
					}
				} // is this some sort of plural entry that needs to be singularised?
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
			String result = stringBuilder.toString().trim();
			return result;
		}
		
		public void collectNodesForDisplay(DependencyNode node, Set<PosTaggedToken> tokensToDisplay, int depth) {
			if (this.shouldDisplay(node, depth))
				tokensToDisplay.add(node.getPosTaggedToken());
			for (DependencyNode child : node.getDependents()) {
				String posTagCode = child.getPosTaggedToken().getTag().getCode();
				int newDepth = depth + 1;
				if (posTagCode.equals("DET"))
					newDepth = depth;
				this.collectNodesForDisplay(child, tokensToDisplay, newDepth);
			}	
		}
		
		public boolean shouldDisplay(DependencyNode node, int depth) {
			String posTagCode = node.getPosTaggedToken().getTag().getCode();
			if (depth==1 && (posTagCode.equals("DET"))) {
				return false;
			}
			
			int numRealDependents = node.getParseConfiguration().getDependents(node.getPosTaggedToken()).size();
			int numAttachedDependents = node.getDependents().size();
			if (numAttachedDependents == 0
					&& ((posTagCode.equals("P") || posTagCode.equals("CC") || posTagCode.equals("CS") || posTagCode.equals("PONCT")|| posTagCode.equals("P+D"))
					|| (numRealDependents>0 && (posTagCode.equals("VPR"))))) {
				return false;
			}
			return true;
		}
	}

	@Override
	public TerminologyBase getTerminologyBase() {
		return terminologyBase;
	}

	@Override
	public void onCompleteParse() {
		// nothing to do for now
	}

	@Override
	public int getMaxDepth() {
		return maxDepth;
	}

	@Override
	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	@Override
	public Set<String> getZeroDepthLabels() {
		if (zeroDepthLabels==null) {
			zeroDepthLabels = new HashSet<String>();
			zeroDepthLabels.add("prep");
			zeroDepthLabels.add("det");
			zeroDepthLabels.add("coord");
			zeroDepthLabels.add("dep_coord");
		}
		return zeroDepthLabels;
	}

	@Override
	public void setZeroDepthLabels(Set<String> zeroDepthLabels) {
		this.zeroDepthLabels = zeroDepthLabels;
	}

	@Override
	public String getOutFilePath() {
		return outFilePath;
	}

	@Override
	public void setOutFilePath(String outFilePath) {
		this.outFilePath = outFilePath;
	}

	@Override
	public Set<PosTag> getIncludeChildren() {
		return includeChildren;
	}

	@Override
	public Set<PosTag> getIncludeWithParent() {
		return includeWithParent;
	}

	@Override
	public void addTermObserver(TermObserver termObserver) {
		this.termObservers.add(termObserver);
	}

	public TerminologyService getTerminologyService() {
		return terminologyService;
	}

	public void setTerminologyService(TerminologyService terminologyService) {
		this.terminologyService = terminologyService;
	}

	public TalismaneService getTalismaneService() {
		return talismaneService;
	}

	public void setTalismaneService(TalismaneService talismaneService) {
		this.talismaneService = talismaneService;
		this.lexicon = talismaneService.getTalismaneSession().getMergedLexicon();
	}
	
}
