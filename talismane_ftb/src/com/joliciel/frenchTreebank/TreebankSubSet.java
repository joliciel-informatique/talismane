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
package com.joliciel.frenchTreebank;

/**
 * A subset of the treebank, used for training, dev, evaluation, etc.
 * @author Assaf Urieli
 *
 */
public enum TreebankSubSet {
	/** Standard training subset, consisting of the files 1 through 8 for each group of 10. */
	TRAINING(new int[] {}, new int[] {0,9}),
	/** Standard dev subset, consisting of file 9 for each group of 10 */
	DEV(new int[] {9}, new int[] {}),
	/** Standard test subset, consisting of file 0 for each group of 10 */
	TEST(new int[] {0}, new int[] {}),
	/** Convenience subset grouping together standard dev & test */
	DEV_AND_TEST(new int[] {0,9}, new int[] {}),
	/** Entire treebank */
	ALL(new int[]{},new int[] {});
	
	int[] fileNumbersToInclude;
	int[] fileNumbersToExclude;
	
	private TreebankSubSet(int[] fileNumbersToInclude, int[] fileNumbersToExclude) {
		this.fileNumbersToInclude = fileNumbersToInclude;
		this.fileNumbersToExclude = fileNumbersToExclude;
	}
	
	/**
	 * Out of every group of 10 files (numbered from 0 to 9), which ones to include.
	 * If empty, include all files except for those explicitly excluded.
	 * @return
	 */
	public int[] getFileNumbersToInclude() {
		return fileNumbersToInclude;
	}

	/**
	 * Out of every group of 10 files (numbered from 0 to 9), which ones to exclude.
	 * If empty, exclude all files except for those explicitly included.
	 * @return
	 */
	public int[] getFileNumbersToExclude() {
		return fileNumbersToExclude;
	}
	

}
