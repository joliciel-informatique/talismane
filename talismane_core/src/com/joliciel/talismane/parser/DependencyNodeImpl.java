package com.joliciel.talismane.parser;

import java.util.Set;
import java.util.TreeSet;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.PosTaggerLexiconService;

class DependencyNodeImpl implements DependencyNode, Comparable<DependencyNode> {
	private PosTaggedToken token;
	private String label;
	private DependencyNode parent;
	private Set<DependencyNode> dependents = new TreeSet<DependencyNode>();
	private ParseConfiguration parseConfiguration;
	private ParserServiceInternal parserServiceInternal;
	private PosTaggerLexiconService lexiconService;
	
	DependencyNodeImpl(PosTaggedToken token, String label,
			DependencyNode parent, ParseConfiguration parseConfiguration) {
		super();
		this.token = token;
		this.label = label;
		this.parent = parent;
		this.parseConfiguration = parseConfiguration;
	}

	public PosTaggedToken getToken() {
		return token;
	}
	
	public String getLabel() {
		return label;
	}
	
	public DependencyNode getParent() {
		return parent;
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
		DependencyNode node = this.getParserServiceInternal().getDependencyNode(dependent, arc.getLabel(), this, this.parseConfiguration);
		this.getDependents().add(node);
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
		return this.getToken().compareTo(o.getToken());
	}

	public PosTaggerLexiconService getLexiconService() {
		return lexiconService;
	}

	public void setLexiconService(PosTaggerLexiconService lexiconService) {
		this.lexiconService = lexiconService;
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
		DependencyNode node = this.parserServiceInternal.getDependencyNode(this.token, this.label, this.parent, this.parseConfiguration);
		for (DependencyNode dependent : this.dependents) {
			DependencyNode clone = dependent.cloneNode();
			node.addDependent(clone);
		}
		return node;
	}

	@Override
	public String toString() {
		String string = this.getToken().getToken().getOriginalText();

		boolean firstDependent = true;
		for (DependencyNode dependent : this.dependents) {
			if (firstDependent) {
				string += " < ";
				firstDependent = false;
			} else {
				string += ", ";
			}
			string += dependent.toString();
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
}
