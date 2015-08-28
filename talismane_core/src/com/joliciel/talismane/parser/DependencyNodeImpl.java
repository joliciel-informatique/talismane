package com.joliciel.talismane.parser;

import java.util.Set;
import java.util.TreeSet;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.PosTaggedTokenLeftToRightComparator;

class DependencyNodeImpl implements DependencyNode, Comparable<DependencyNode> {
	private PosTaggedToken token;
	private String label;
	private DependencyNode parent;
	private Set<DependencyNode> dependents = new TreeSet<DependencyNode>();
	private ParseConfiguration parseConfiguration;
	private ParserServiceInternal parserServiceInternal;
	private String string = null;
	private Boolean contiguous = null;
	
	DependencyNodeImpl(PosTaggedToken token, String label,
			ParseConfiguration parseConfiguration) {
		super();
		this.token = token;
		this.label = label;
		this.parseConfiguration = parseConfiguration;
	}

	public PosTaggedToken getPosTaggedToken() {
		return token;
	}
	
	public String getLabel() {
		return label;
	}
	
	public DependencyNode getParent() {
		return parent;
	}

	public void setParent(DependencyNode parent) {
		this.parent = parent;
	}

	public Set<DependencyNode> getDependents() {
		return dependents;
	}

	@Override
	public DependencyNode addDependent(PosTaggedToken dependent) {
		DependencyArc arc = parseConfiguration.getGoverningDependency(dependent);
		if (arc==null) {
			throw new TalismaneException("Can only add a dependent to a dependency node if it is a true dependent in the parse configuration.");
		}
		DependencyNode node = this.getParserServiceInternal().getDependencyNode(dependent, arc.getLabel(), this.parseConfiguration);
		this.getDependents().add(node);
		node.setParent(this);
		this.setDirty();
		return node;
	}

	@Override
	public void addDependent(DependencyNode dependent) {
		this.getDependents().add(dependent);
		this.setDirty();
	}

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

	public ParseConfiguration getParseConfiguration() {
		return parseConfiguration;
	}

	public ParserServiceInternal getParserServiceInternal() {
		return parserServiceInternal;
	}

	public void setParserServiceInternal(ParserServiceInternal parserServiceInternal) {
		this.parserServiceInternal = parserServiceInternal;
	}

	@Override
	public DependencyNode cloneNode() {
		DependencyNode node = this.parserServiceInternal.getDependencyNode(this.token, this.label, this.parseConfiguration);
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
		if (string==null) {
			string = this.getPosTaggedToken().getToken().getOriginalText();
	
			boolean firstDependent = true;
			if (this.dependents.size()>0) {
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

	@Override
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
	
	@Override
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

	@Override
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

	@Override
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

	@Override
	public boolean isContiguous() {
		if (contiguous==null) {
			Set<PosTaggedToken> tokens = new TreeSet<PosTaggedToken>(new PosTaggedTokenLeftToRightComparator());
			this.getAllNodes(tokens);
			int currentIndex = -1;
			contiguous = true;
			for (PosTaggedToken token : tokens) {
				if (currentIndex<0) {
					currentIndex = token.getIndex();
				} else if (token.getIndex()==currentIndex+1) {
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
			((DependencyNodeImpl) dependent).getAllNodes(posTaggedTokens);
		}
	}

	@Override
	public void setDirty() {
		this.string = null;
		this.contiguous = null;
		if (this.parent!=null) {
			this.parent.setDirty();
		}
	}
	
	
}
