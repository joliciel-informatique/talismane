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
package com.joliciel.talismane.tokeniser.filters;

import java.io.IOException;
import java.lang.reflect.Constructor;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * A filter for applying a given transformation to a sentence that has been
 * tokenised. This may call token.setText(String) to any token, and add empty
 * tokens.
 * 
 * @author Assaf Urieli
 *
 */
public interface TokenFilter {
  public void apply(TokenSequence tokenSequence);

  /**
   * Load a filter by it's class name. The filter must implement a
   * single-argument constructor taking a {@link TalismaneSession}, and must
   * implement the {@link TokenFilter} interface.
   * 
   * @param className
   * @param session
   * @return the filter loaded
   * @throws IOException
   * @throws TalismaneException
   *           if the class to build does not implement the correct interface or
   *           does not have the required constructor
   * @throws ReflectiveOperationException
   */
  public static TokenFilter loadFilter(String className, TalismaneSession session) throws IOException, TalismaneException, ReflectiveOperationException {
    TokenFilter filter = null;

    @SuppressWarnings("rawtypes")
    Class untypedClass = Class.forName(className);
    if (!TokenFilter.class.isAssignableFrom(untypedClass))
      throw new TalismaneException("Class " + className + " does not implement interface " + TokenFilter.class.getSimpleName());

    @SuppressWarnings("unchecked")
    Class<? extends TokenFilter> clazz = untypedClass;

    Constructor<? extends TokenFilter> cons = null;

    if (cons == null) {
      try {
        cons = clazz.getConstructor(TalismaneSession.class);
      } catch (NoSuchMethodException e) {
        // do nothing
      }
      if (cons != null) {
        filter = cons.newInstance(session);
      } else {
        throw new TalismaneException("No constructor found with correct signature for: " + className);
      }
    }

    return filter;
  }
}
