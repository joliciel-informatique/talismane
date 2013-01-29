///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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
package com.joliciel.talismane.posTagger.filters;

public interface PosTagFilterService {
	public static final String POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY = "postag_preprocessing_filter";	
	public static final String POSTAG_POSTPROCESSING_FILTER_DESCRIPTOR_KEY = "postag_postprocessing_filter";
	
	/**
	 * Gets a PosTagSequenceFilter corresponding to a given descriptor.
	 * The descriptor should contain the class name, followed by any arguments, separated by tabs.
	 * @param descriptor
	 * @return
	 */
	public PosTagSequenceFilter getPosTagSequenceFilter(String descriptor);
}
