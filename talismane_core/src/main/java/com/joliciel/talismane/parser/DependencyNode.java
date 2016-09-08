package com.joliciel.talismane.parser;

import java.util.Set;
import java.util.TreeSet;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.PosTaggedTokenLeftToRightComparator;

/**
 * A node inside a dependency tree.
 * 
 * @author Assaf Urieli
 *
 */
public class DependencyNode implements Comparable<DependencyNode> {
	private final PosTaggedToken token;
	private final String label;
	private DependencyNode parent;
	private final Set<DependencyNode> dependents = new TreeSet<>();
	private final ParseConfiguration parseConfiguration;

	private String string = null;
	private Boolean contiguous = null;

	DependencyNode(PosTaggedToken token, String label, ParseConfiguration parseConfiguration) {
		this.token = token;
		this.label = label;
		this.parseConfiguration = parseConfiguration;
	}

	/**
	 * The actual pos-tagged token in this node.
	 */
	public PosTaggedToken getPosTaggedToken() {
		return token;
	}

	/**
	 * The dependency label tying this node to its parent.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * This node's parent.
	 */
	public DependencyNode getParent() {
		return parent;
	}

	public void setParent(DependencyNode parent) {
		this.parent = parent;
	}

	/**
	 * This node's dependents.
	 */
	public Set<DependencyNode> getDependents() {
		return dependents;
	}

	/**
	 * Add an existing dependent of the current node's token to the current
	 * dependency node. The dependent must already exist in the parse
	 * configuration. This is useful for constructing sub-trees out of the
	 * existing parse tree.
	 */
	public DependencyNode addDependent(PosTaggedToken dependent) {
		DependencyArc arc = parseConfiguration.getGoverningDependency(dependent);
		if (arc == null) {
			throw new TalismaneException("Can only add a dependent to a dependency node if it is a true dependent in the parse configuration.");
		}
		DependencyNode node = new DependencyNode(dependent, arc.getLabel(), this.parseConfiguration);
		this.getDependents().add(node);
		node.setParent(this);
		this.setDirty();
		return node;
	}

	/**
	 * Add a dependent in the form of an existing dependency node.
	 */
	public void addDependent(DependencyNode dependent) {
		this.getDependents().add(dependent);
		this.setDirty();
	}

	/**
	 * Populate this node's dependents directly from the parse configuration.
	 */
	public void autoPopulate() {
		for (PosTaggedToken dependent : parseConfiguration.getDependents(this.token)) {
			DependencyNode childNode = this.addDependent(dependent);
			childNode.autoPopulate();
		}
	}

	@Override
	public int compareTo(DependencyNode o) {
		return this.getPosTaggedToken().compareTo(o.getPosTaggedToken());
	}

	/**
	 * The parse configuration from which this node was derived.
	 */
	public ParseConfiguration getParseConfiguration() {
		return parseConfiguration;
	}

	/**
	 * Clone the current dependency node.
	 */
	public DependencyNode cloneNode() {
		DependencyNode node = new DependencyNode(this.token, this.label, this.parseConfiguration);
		for (DependencyNode dependent : this.dependents) {
			DependencyNode clone = dependent.cloneNode();
			node.addDependent(clone);
		}
		return node;
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this.toString().equals(obj.toString());
	}

	@Override
	public String toString() {
		if (string == null) {
			string = this.getPosTaggedToken().getToken().getOriginalText();

			boolean firstDependent = true;
			if (this.dependents.size() > 0) {
				string += "(";
				for (DependencyNode dependent : this.dependents) {
					if (firstDependent) {
						firstDependent = false;
					} else {
						string += ", ";
					}
					string += dependent.toString();
				}
				string += ")";
			}
		}
		return string;
	}

	/**
	 * Return the depth of this tree starting at its head (1).
	 */
	public int getDepth() {
		int depth = 1;
		int maxDepth = 0;
		for (DependencyNode dependent : this.getDependents()) {
			int dependentDepth = dependent.getDepth();
			if (dependentDepth > maxDepth) {
				maxDepth = dependentDepth;
			}
		}
		depth += maxDepth;
		return depth;
	}

	/**
	 * Language-specific depth as a typical user would expect to see it.<br/>
	 * In French, for example, a preposition and its object would thus be a
	 * single layer of depth, equivalent to a single adjective when modifying a
	 * noun. A determinent would not add to its governor's depth.
	 * 
	 * @param zeroDepthLabels
	 *            labels which shouldn't be counted for depth calculation
	 */
	public int getPerceivedDepth(Set<String> zeroDepthLabels) {
		int depth = 1;

		if (zeroDepthLabels.contains(this.getLabel()))
			depth = 0;
		int maxDepth = 0;
		for (DependencyNode dependent : this.getDependents()) {
			int dependentDepth = dependent.getPerceivedDepth(zeroDepthLabels);
			if (dependentDepth > maxDepth) {
				maxDepth = dependentDepth;
			}
		}
		depth += maxDepth;
		return depth;
	}

	/**
	 * Removes a node (and all of its children) from this dependency node's
	 * tree, wherever it may be located.
	 * 
	 * @return true if the node was removed.
	 */
	public boolean removeNode(DependencyNode node) {
		if (this.getDependents().contains(node)) {
			this.getDependents().remove(node);
			this.setDirty();
			return true;
		}
		for (DependencyNode dependent : this.getDependents()) {
			boolean removed = dependent.removeNode(node);
			if (removed)
				return true;
		}
		return false;
	}

	/**
	 * The first token comprising this dependency node, if viewed in linear
	 * order within a sentence.
	 */
	public PosTaggedToken getFirstToken() {
		PosTaggedToken firstToken = this.getPosTaggedToken();
		for (DependencyNode dependent : this.getDependents()) {
			PosTaggedToken firstDepToken = dependent.getFirstToken();
			if (firstDepToken.getToken().getStartIndex() < firstToken.getToken().getStartIndex()) {
				firstToken = firstDepToken;
			}
		}
		return firstToken;
	}

	/**
	 * The last token comprising this dependency node, if viewed in linear order
	 * within a sentence.
	 */
	public PosTaggedToken getLastToken() {
		PosTaggedToken lastToken = this.getPosTaggedToken();
		for (DependencyNode dependent : this.getDependents()) {
			PosTaggedToken lastDepToken = dependent.getLastToken();
			if (lastDepToken.getToken().getEndIndex() > lastToken.getToken().getEndIndex()) {
				lastToken = lastDepToken;
			}
		}
		return lastToken;
	}

	/**
	 * Return true if this node is contiguous, false if it contains gaps.
	 */
	public boolean isContiguous() {
		if (contiguous == null) {
			Set<PosTaggedToken> tokens = new TreeSet<PosTaggedToken>(new PosTaggedTokenLeftToRightComparator());
			this.getAllNodes(tokens);
			int currentIndex = -1;
			contiguous = true;
			for (PosTaggedToken token : tokens) {
				if (currentIndex < 0) {
					currentIndex = token.getIndex();
				} else if (token.getIndex() == currentIndex + 1) {
					currentIndex++;
				} else {
					contiguous = false;
					break;
				}
			}
		}
		return contiguous.booleanValue();
	}

	public void getAllNodes(Set<PosTaggedToken> posTaggedTokens) {
		posTaggedTokens.add(this.token);
		for (DependencyNode dependent : this.getDependents()) {
			dependent.getAllNodes(posTaggedTokens);
		}
	}

	void setDirty() {
		this.string = null;
		this.contiguous = null;
		if (this.parent != null) {
			this.parent.setDirty();
		}
	}

}
