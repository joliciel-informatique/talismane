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
package com.joliciel.talismane.parser.features;

import com.joliciel.talismane.parser.ParseConfiguration;

/**
 * A simple container for a ParseConfiguration + an address function.
 * @author Assaf Urieli
 *
 */
public class ParseConfigurationAddress {
	private ParseConfiguration parseConfiguration;
	private AddressFunction addressFunction;
	public ParseConfigurationAddress(ParseConfiguration parseConfiguration,
			AddressFunction addressFunction) {
		super();
		this.parseConfiguration = parseConfiguration;
		this.addressFunction = addressFunction;
	}
	public ParseConfiguration getParseConfiguration() {
		return parseConfiguration;
	}
	public AddressFunction getAddressFunction() {
		return addressFunction;
	}
	
	
}
