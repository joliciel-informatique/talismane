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
package com.joliciel.talismane.filters;

import static org.junit.Assert.*;

import org.junit.Test;

public class DuplicateWhiteSpaceFilterTest {
	
	@Test
	public void testApply() {
		DuplicateWhiteSpaceFilter filter = new DuplicateWhiteSpaceFilter();
		String text = "Hello  World.\n\n  How are you     today?\tFine,\f\f  thanks.  \n";
		String newText = filter.apply(text);
		assertEquals("Hello World.\n\n How are you today? Fine, thanks. \n", newText);
	}
}
