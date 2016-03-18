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
package com.joliciel.talismane.extensions.corpus;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * Transforms a non-projective corpus to a projective corpus by attaching non-projective arcs to the nearest projective head.
 * @author Assaf Urieli
 *
 */
public class CorpusProjectifier implements ParseConfigurationProcessor {
	ParseConfigurationProcessor wrappedProcessor = null;
	
	public CorpusProjectifier(ParseConfigurationProcessor wrappedProcessor) {
		super();
		this.wrappedProcessor = wrappedProcessor;
	}

	@Override
	public void onNextParseConfiguration(ParseConfiguration parseConfiguration, Writer writer) {
		
		List<DependencyArc> arcs = new ArrayList<DependencyArc>(parseConfiguration.getDependencies());
		
		NonProjectivePair pair = this.getNextPair(arcs);
		if (pair!=null) {
			// have non projective dependencies - set up initial non-projective set so that it stays untouched
			for (DependencyArc arc : arcs) {
				parseConfiguration.addManualNonProjectiveDependency(arc.getHead(), arc.getDependent(), arc.getLabel());
			}
		}
		while (pair!=null) {
			PosTaggedToken newHead1 = null;
			PosTaggedToken parent1 = parseConfiguration.getHead(pair.arc1.getHead());
			int depIndex1 = pair.arc1.getDependent().getToken().getIndex();
			while (parent1!=null) {
				int headIndex = parent1.getToken().getIndex();
				int startIndex = headIndex < depIndex1 ? headIndex : depIndex1;
				int endIndex = headIndex >= depIndex1 ? headIndex : depIndex1;
				if (isProjective(startIndex, endIndex, pair.arc2)) {
					newHead1 = parent1;
					break;
				}
				parent1 = parseConfiguration.getHead(parent1);
			}
			PosTaggedToken newHead2 = null;
			PosTaggedToken parent2 = parseConfiguration.getHead(pair.arc2.getHead());
			int depIndex2 = pair.arc2.getDependent().getToken().getIndex();
			while (parent2!=null) {
				int headIndex = parent2.getToken().getIndex();
				int startIndex = headIndex < depIndex2 ? headIndex : depIndex2;
				int endIndex = headIndex >= depIndex2 ? headIndex : depIndex2;
				if (isProjective(startIndex, endIndex, pair.arc2)) {
					newHead2 = parent2;
					break;
				}
				parent2 = parseConfiguration.getHead(parent2);
			}
			if (newHead1!=null && newHead2!=null) {
				int distance1 = 0;
				PosTaggedToken parent = parseConfiguration.getHead(newHead1);
				while (parent!=null) {
					distance1++;
					parent = parseConfiguration.getHead(parent);
				}
				int distance2 = 0;
				parent = parseConfiguration.getHead(newHead2);
				while (parent!=null) {
					distance2++;
					parent = parseConfiguration.getHead(parent);
				}
				// we want the new arc to be as far as possible from root
				if (distance1<distance2) {
					newHead1 = null;
				} else {
					newHead2 = null;
				}
			}
			if (newHead1!=null && newHead2==null) {
				parseConfiguration.getDependencies().remove(pair.arc1);
				parseConfiguration.addDependency(newHead1, pair.arc1.getDependent(), pair.arc1.getLabel(), null);
			} else if (newHead1==null && newHead2!=null) {
				parseConfiguration.getDependencies().remove(pair.arc2);
				parseConfiguration.addDependency(newHead2, pair.arc2.getDependent(), pair.arc2.getLabel(), null);
			} else {
				throw new RuntimeException("Cannot deprojectify " + pair);
			}
			parseConfiguration.clearMemory();
			
			arcs = new ArrayList<DependencyArc>(parseConfiguration.getDependencies());
			pair = this.getNextPair(arcs);
		}
		this.wrappedProcessor.onNextParseConfiguration(parseConfiguration, writer);
	}
	
	private NonProjectivePair getNextPair(List<DependencyArc> arcs) {
		NonProjectivePair pair = null;
		DependencyArc arc = null;
		DependencyArc otherArc = null;
		for (int i=0; i<arcs.size(); i++) {
			arc = arcs.get(i);
			if (arc.getHead().getTag().equals(PosTag.ROOT_POS_TAG) && (arc.getLabel()==null||arc.getLabel().length()==0))
				continue;
			int headIndex = arc.getHead().getToken().getIndex();
			int depIndex = arc.getDependent().getToken().getIndex();
			int startIndex = headIndex < depIndex ? headIndex : depIndex;
			int endIndex = headIndex >= depIndex ? headIndex : depIndex;
			
			for (int j=i+1; j<arcs.size();j++) {
				otherArc = arcs.get(j);
				if (otherArc.getHead().getTag().equals(PosTag.ROOT_POS_TAG) && (otherArc.getLabel()==null||otherArc.getLabel().length()==0))
					continue;
				if (!isProjective(startIndex, endIndex, otherArc)) {
					pair = new NonProjectivePair(arc, otherArc);
					break;
				}

			}
			if (pair!=null)
				break;
		}
		return pair;
	}
	
	boolean isProjective(int startIndex, int endIndex, DependencyArc otherArc) {
		boolean projective = true;
		
		int headIndex2 = otherArc.getHead().getToken().getIndex();
		int depIndex2 = otherArc.getDependent().getToken().getIndex();
		int startIndex2 = headIndex2 < depIndex2 ? headIndex2 : depIndex2;
		int endIndex2 = headIndex2 >= depIndex2 ? headIndex2 : depIndex2;
		if (startIndex2<startIndex && endIndex2>startIndex && endIndex2<endIndex) {
			projective = false;
		} else if (startIndex2>startIndex && startIndex2<endIndex && endIndex2>endIndex) {
			projective = false;
		}
		return projective;
	}
	
	@Override
	public void onCompleteParse() {
		this.wrappedProcessor.onCompleteParse();
	}
	
	private static final class NonProjectivePair {
		DependencyArc arc1;
		DependencyArc arc2;
		public NonProjectivePair(DependencyArc arc1, DependencyArc arc2) {
			super();
			this.arc1 = arc1;
			this.arc2 = arc2;
		}
		@Override
		public String toString() {
			return "NonProjectivePair [arc1=" + arc1 + ", arc2=" + arc2 + "]";
		}
		
		
	}

}
