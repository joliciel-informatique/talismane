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
package com.joliciel.talismane.tokeniser;

/**
 * A set of decisions regarding each separator.
 * 
 * @author Assaf Urieli
 *
 */
public enum SeparatorDecision {
  /**
   * The current symbol has a token separation before and after it.
   */
  IS_SEPARATOR,

  /**
   * The current symbol is separated from the symbol preceding it, but attached
   * to the following one.
   */
  IS_SEPARATOR_BEFORE,

  /**
   * The current symbol is separated from the symbol following it, but attached
   * to the preceding one.
   */
  IS_SEPARATOR_AFTER,

  /**
   * The current symbol is not a separator.
   */
  IS_NOT_SEPARATOR,

  /**
   * The current symbol doesn't require a tokeniser decision (it's not a
   * separator).
   */
  NOT_APPLICABLE
}
