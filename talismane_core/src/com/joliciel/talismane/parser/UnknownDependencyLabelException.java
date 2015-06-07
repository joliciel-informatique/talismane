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
package com.joliciel.talismane.parser;

import com.joliciel.talismane.TalismaneException;

/**
 * Thrown when the dependency label requested does not exist in the current set of dependency labels.
 * @author Assaf Urieli
 *
 */
public class UnknownDependencyLabelException extends TalismaneException {
	private static final long serialVersionUID = 1L;
	private String dependencyLabel = "";
	private int index;
	
	public UnknownDependencyLabelException(int index, String dependencyLabel) {
		super("Unknown dependency label: " + dependencyLabel + " on index " + index);
		this.dependencyLabel = dependencyLabel;
		this.index = index;
	}
	
	public UnknownDependencyLabelException(String dependencyLabel) {
		super("Unknown dependency label: " + dependencyLabel);
		this.dependencyLabel = dependencyLabel;
	}

	public String getDependencyLabel() {
		return dependencyLabel;
	}
	
	public int getIndex() {
		return index;
	}
}