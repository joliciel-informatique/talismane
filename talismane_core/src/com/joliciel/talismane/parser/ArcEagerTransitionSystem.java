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
 * An arc-eager Shift-Reduce transition system, as described in Nivre 2009 Dependency Parsing, Chapter 3,
 * in which right-depedencies are attached as soon as possible, rather than waiting until all left-dependencies
 * have been attached.
 * @author Assaf Urieli
 *
 */
class ArcEagerTransitionSystem extends AbstractTransitionSystem implements TransitionSystem {
	private static final long serialVersionUID = -7344308319170439498L;
	private static final Log LOG = LogFactory.getLog(ArcEagerTransitionSystem.class);

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
					transition = this.getTransitionForCode("LeftArc[" + arc.getLabel() + "]");
					currentDep = arc;
					break;
				}
				
				if (arc.getHead().equals(stackHead)&&arc.getDependent().equals(bufferHead)) {
					transition = this.getTransitionForCode("RightArc[" + arc.getLabel() + "]");
					currentDep = arc;
					break;
				}
			}
			
			if (transition==null) {
				boolean stackHeadHasGovernor = configuration.getHead(stackHead)!=null;
				boolean stackHeadHasDependents = false;
				if (stackHeadHasGovernor) {
					for (DependencyArc arc : targetDependencies) {
						if (arc.getHead().equals(stackHead)) {
							stackHeadHasDependents = true;
							break;
						}
					}
				}
	
				if (stackHeadHasGovernor && !stackHeadHasDependents) {
					transition = this.getTransitionForCode("Reduce");
				}
			}
			
			if (transition==null) {
				transition =  this.getTransitionForCode("Shift");
			}
			if (currentDep!=null)
				targetDependencies.remove(currentDep);
			
			transition.apply(configuration);
		}
		if (targetDependencies.size()>0) {
			throw new RuntimeException("Wasn't able to predict: " + targetDependencies);
		}
		LOG.debug("Full prediction complete");
	}

	@Override
	public Transition getTransitionForCode(String code) {
		AbstractTransition transition = null;
		String label = null;
		if (code.indexOf('[')>=0) {
			label = code.substring(code.indexOf('[')+1, (code.indexOf(']')));
		}
		if (code.startsWith("LeftArc")) {
			transition = new LeftArcEagerTransition(label);
		} else if (code.startsWith("RightArc")) {
			transition = new RightArcEagerTransition(label);
		} else if (code.startsWith("Shift")) {
			transition = new ShiftTransition();
		} else if (code.startsWith("Reduce")) {
			transition = new ReduceTransition();
		} else {
			throw new TalismaneException("Unknown transition name: " + code);
		}
		
		return transition;
	}


}
