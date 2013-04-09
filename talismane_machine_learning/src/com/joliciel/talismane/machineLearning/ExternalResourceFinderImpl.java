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
package com.joliciel.talismane.machineLearning;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class ExternalResourceFinderImpl implements ExternalResourceFinder {
	private Map<String,ExternalResource> resourceMap = new HashMap<String, ExternalResource>();
	
	@Override
	public ExternalResource getExternalResource(String name) {
		return this.resourceMap.get(name);
	}
	
	public void addExternalResource(ExternalResource externalResource) {
		this.resourceMap.put(externalResource.getName(), externalResource);
	}

	@Override
	public Collection<ExternalResource> getExternalResources() {
		return resourceMap.values();
	}
}
