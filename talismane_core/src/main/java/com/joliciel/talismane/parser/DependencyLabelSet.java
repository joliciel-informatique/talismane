///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2017 Joliciel Informatique
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

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import com.joliciel.talismane.TalismaneException;

/**
 * The set of allowable dependency labels.
 * 
 * @author Assaf Urieli
 *
 */
public class DependencyLabelSet {
	private Set<String> dependencyLabels = new TreeSet<>();
	private Map<String, String> descriptions = new HashMap<>();
	private String punctuationLabel = null;

	/**
	 * Load a dependency label set from a scanner with the following format:
	 * <br/>
	 * Empty lines or lines starting with # are ignored.<br/>
	 * Other lines an have up to three tabs.<br/>
	 * Tab 1: the label<br/>
	 * Tab 2: the description (optional)<br/>
	 * Tab 3: the word "Punctuation", if the current label is the generic label
	 * for punctuation (optional, may only appear once).
	 * 
	 * @param scanner
	 * @throws TalismaneException
	 *             if two or more labels were marked as Punctuation
	 */
	public DependencyLabelSet(Scanner scanner) throws TalismaneException {
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (!line.startsWith("#") && line.length() > 0) {
				String[] parts = line.split("\t");
				if (parts.length > 0 && parts[0].length() > 0) {
					dependencyLabels.add(parts[0]);
					if (parts.length > 1 && parts[1].length() > 0) {
						descriptions.put(parts[0], parts[1]);
					}
					if (parts.length > 2 && "Punctuation".equals(parts[2])) {
						if (punctuationLabel != null) {
							throw new TalismaneException(
									"Only one dependency label may be marked as Punctuation. 2 were found: " + punctuationLabel + ", " + parts[0]);
						}
						punctuationLabel = parts[0];
					}
				}
			}
		}
	}

	/**
	 * The set of dependency labels as Strings.
	 */
	public Set<String> getDependencyLabels() {
		return this.dependencyLabels;
	}

	/**
	 * Get the description for a particular label, or an empty string if none.
	 */
	public String getDescription(String label) {
		String description = this.descriptions.get(label);
		if (description == null)
			description = "";
		return description;
	}

	/**
	 * Return a label used for generic governing of punctuation, if one exists,
	 * null otherwise.
	 */
	public String getPunctuationLabel() {
		return this.punctuationLabel;
	}

}
