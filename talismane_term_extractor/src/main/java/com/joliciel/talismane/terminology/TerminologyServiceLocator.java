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

import com.joliciel.talismane.TalismaneServiceLocator;

public class TerminologyServiceLocator {
	TerminologyServiceImpl terminologyService = null;
	private static TerminologyServiceLocator instance = null;
	private TalismaneServiceLocator talismaneServiceLocator;
	
	private TerminologyServiceLocator(TalismaneServiceLocator talismaneServiceLocator) {
		this.talismaneServiceLocator = talismaneServiceLocator;
	}
	
	public static TerminologyServiceLocator getInstance(TalismaneServiceLocator talismaneServiceLocator) {
		if (instance==null) {
			instance = new TerminologyServiceLocator(talismaneServiceLocator);
		}
		return instance;
	}
	
	public TerminologyService getTerminologyService() {
		if (terminologyService==null) {
			terminologyService = new TerminologyServiceImpl();
			terminologyService.setTalismaneService(this.talismaneServiceLocator.getTalismaneService());
		}
		return terminologyService;
	}
}
