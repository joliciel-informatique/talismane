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
package com.joliciel.talismane.machineLearning.features;

import java.util.HashMap;
import java.util.Map;

class RuntimeEnvironmentImpl implements RuntimeEnvironment {
	private Map<String, String> variableMap = new HashMap<String, String>();
	
	@Override
	public String getValue(String variableName) {
		return variableMap.get(variableName);
	}

	@Override
	public void setValue(String variableName, String value) {
		variableMap.put(variableName, value);
	}

	@Override
	public String getKey() {
		if (variableMap.size()==0)
			return "";
		StringBuilder sb = new StringBuilder();
		for (String variable : variableMap.keySet()) {
			sb.append("|" + variable + ":" + variableMap.get(variable));
		}
		return sb.toString();
	}

}
