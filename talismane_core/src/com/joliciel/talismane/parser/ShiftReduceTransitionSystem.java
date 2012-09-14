///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.talismane.parser;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * The standard Shift-Reduce transition system, as described in Nivre 2009 Dependency Parsing, Chapter 3.
 * @author Assaf Urieli
 *
 */
class ShiftReduceTransitionSystem implements TransitionSystem {
    private static final Log LOG = LogFactory.getLog(ShiftReduceTransitionSystem.class);
	private ParserServiceInternal parserServiceInternal;

	@Override
	public void predictTransitions(ParseConfiguration configuration,
			Set<DependencyArc> targetDependencies) {
		LOG.debug("predictTransitions");
		LOG.debug(configuration.toString());
		LOG.debug(targetDependencies);
		while (!configuration.getBuffer().isEmpty()) {
			PosTaggedToken stackHead = configuration.getStack().peek();
			PosTaggedToken bufferHead = configuration.getBuffer().peekFirst();
			
			Transition transition = null;
			DependencyArc currentDep = null;
			for (DependencyArc arc : targetDependencies) {
				if (arc.getHead().equals(bufferHead)&&arc.getDependent().equals(stackHead)) {
					transition = this.getTransitionForName("LeftArc[" + arc.getLabel() + "]", 1.0);
					currentDep = arc;
					break;
				}
				
				if (arc.getHead().equals(stackHead)&&arc.getDependent().equals(bufferHead)) {
					boolean dependentHasDependents = false;
					for (DependencyArc otherArc : targetDependencies) {
						if (otherArc.getHead().equals(bufferHead)) {
							dependentHasDependents = true;
							break;
						}
					}
					if (!dependentHasDependents) {
						transition = this.getTransitionForName("RightArc[" + arc.getLabel() + "]", 1.0);
						currentDep = arc;
						break;
					}
				}

			}
			if (transition==null) {
				transition =  this.getTransitionForName("Shift", 1.0);
			}
			if (currentDep!=null)
				targetDependencies.remove(currentDep);
			
			transition.apply(configuration);
			if (LOG.isTraceEnabled())
				LOG.trace(this.toString());
			
		}
		if (targetDependencies.size()>0) {
			throw new RuntimeException("Wasn't able to predict: " + targetDependencies);
		}
		LOG.debug("Full prediction complete");
	}

	@Override
	public Transition getTransitionForName(String name, double probability) {
		AbstractTransition transition = null;
		String label = null;
		if (name.indexOf('[')>=0) {
			label = name.substring(name.indexOf('[')+1, (name.indexOf(']')));
		}
		if (name.startsWith("LeftArc")) {
			transition = new LeftArcTransition(label);
		} else if (name.startsWith("RightArc")) {
			transition = new RightArcTransition(label);
		} else if (name.startsWith("Shift")) {
			transition = new ShiftTransition();
		} else {
			throw new TalismaneException("Unknown transition name: " + name);
		}
		
		transition.setParserServiceInternal(this.parserServiceInternal);
		transition.setProbability(probability);
		
		return transition;
	}

	public ParserServiceInternal getParserServiceInternal() {
		return parserServiceInternal;
	}

	public void setParserServiceInternal(ParserServiceInternal parserServiceInternal) {
		this.parserServiceInternal = parserServiceInternal;
	}

}
