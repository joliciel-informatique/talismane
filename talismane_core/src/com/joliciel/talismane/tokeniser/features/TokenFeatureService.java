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
package com.joliciel.talismane.tokeniser.features;

import java.util.List;
import java.util.Set;

import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;

public interface TokenFeatureService {
	public Set<TokeniserContextFeature<?>> getTokeniserContextFeatureSet(List<String> featureDescriptors,
			List<TokenPattern> patternList);
	
	public Set<TokenPatternMatchFeature<?>> getTokenPatternMatchFeatureSet(List<String> featureDescriptors);

	public TokenFeatureParser getTokenFeatureParser();
	
	public ExternalResourceFinder getExternalResourceFinder();
	public void setExternalResourceFinder(ExternalResourceFinder externalResourceFinder);

}
