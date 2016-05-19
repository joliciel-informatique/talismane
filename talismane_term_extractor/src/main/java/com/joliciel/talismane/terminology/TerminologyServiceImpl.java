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
package com.joliciel.talismane.terminology;

import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.terminology.TermExtractor.TerminologyProperty;
import com.joliciel.talismane.terminology.postgres.PostGresTerminologyBase;

class TerminologyServiceImpl implements TerminologyServiceInternal {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(TerminologyServiceImpl.class);
	
	private TalismaneService talismaneService;
	
	public TerminologyBase getPostGresTerminologyBase(String projectCode, Properties connectionProperties) {
		TerminologyBase terminologyBase = new PostGresTerminologyBase(projectCode, connectionProperties);
		return terminologyBase;
		
	}

	@Override
	public TermExtractor getTermExtractor(TerminologyBase terminologyBase, Map<TerminologyProperty,String> terminologyProperties) {
		TermExtractorImpl termExtractor = new TermExtractorImpl(terminologyBase, terminologyProperties);
		termExtractor.setTerminologyService(this);
		termExtractor.setTalismaneService(this.getTalismaneService());
		return termExtractor;
	}

	public TalismaneService getTalismaneService() {
		return talismaneService;
	}

	public void setTalismaneService(TalismaneService talismaneService) {
		this.talismaneService = talismaneService;
	}



	
}
