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
package com.joliciel.talismane.rawText;

import com.joliciel.talismane.TalismaneException;

/**
 * Replaces any whitespace characters (including tabs) by a standard whitespace.
 * 
 * @author Assaf Urieli
 *
 */
public class OtherWhiteSpaceFilter extends RawTextRegexAnnotator {
  public OtherWhiteSpaceFilter(int blockSize) throws TalismaneException {
    super(RawTextMarkType.REPLACE, "[\\t\u000B\u00a0\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a\u200b\u202f\u205f\u3000\ufeff]", 0,
        blockSize);
    this.setReplacement(" ");
  }
}
