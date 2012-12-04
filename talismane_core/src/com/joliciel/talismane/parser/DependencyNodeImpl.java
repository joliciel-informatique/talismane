package com.joliciel.talismane.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTaggedToken;

class DependencyNodeImpl implements DependencyNode, Comparable<DependencyNode> {
	private PosTaggedToken token;
	private String label;
	private DependencyNode parent;
	private Set<DependencyNode> dependents = new TreeSet<DependencyNode>();
	private ParseConfiguration parseConfiguration;
	private ParserServiceInternal parserServiceInternal;
	
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
		return node;
	}

	@Override
	public void addDependent(DependencyNode dependent) {
		this.getDependents().add(dependent);
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
	public String toString() {
		String string = this.getPosTaggedToken().getToken().getOriginalText();

		boolean firstDependent = true;
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

	public int getPerceivedDepth(Set<PosTag> zeroDepthPosTags) {
		int depth = 1;
		if (zeroDepthPosTags.contains(this.getPosTaggedToken().getTag()))
			depth = 0;
		int maxDepth = 0;
		for (DependencyNode dependent : this.getDependents()) {
			int dependentDepth = dependent.getPerceivedDepth(zeroDepthPosTags);
			if (dependentDepth > maxDepth) {
				maxDepth = dependentDepth;
			}
		}
		depth += maxDepth;
		return depth;
	}
	
	public List<DependencyNode> getHeads(Set<PosTag> includeChildren, Set<PosTag> includeWithParent) {
		List<DependencyNode> heads = new ArrayList<DependencyNode>();
		if (this.dependents.size()==0)
			return heads;
		List<DependencyNode> detachableLeaves = this.getDetachableLeaves(includeChildren, includeWithParent);
		if (detachableLeaves.size()==0)
			return heads;
		for (DependencyNode detachableDependent : detachableLeaves) {
			DependencyNode head = this.cloneNode();
			head.removeNode(detachableDependent);
			heads.add(head);
		}
		
		return heads;
	}
	
	public List<DependencyNode> getDetachableLeaves(Set<PosTag> includeChildren, Set<PosTag> includeWithParent) {
		List<DependencyNode> detachableLeaves = new ArrayList<DependencyNode>();
		
		boolean includesChildren = includeChildren.contains(this.getPosTaggedToken().getTag());
		for (DependencyNode dependent : this.getDependents()) {
			List<DependencyNode> myDetachableLeaves = dependent.getDetachableLeaves(includeChildren, includeWithParent);
			if (myDetachableLeaves.size()==0) {
				boolean detachable = !includesChildren && !(includeWithParent.contains(dependent.getPosTaggedToken().getTag()));
				if (detachable)
					detachableLeaves.add(dependent);
			} else {
				detachableLeaves.addAll(myDetachableLeaves);
			}
		}
		return detachableLeaves;
	}

	@Override
	public boolean removeNode(DependencyNode node) {
		if (this.getDependents().contains(node)) {
			this.getDependents().remove(node);
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
}
