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
package com.joliciel.talismane.machineLearning;

import java.io.IOException;
import java.util.Map;

import com.joliciel.talismane.TalismaneException;

/**
 * An interface for reading corpus classification events from a training or test
 * corpus.
 * 
 * @author Assaf Urieli
 *
 */
public interface ClassificationEventStream {
  /**
   * Does this event reader have any more events to read?
   * 
   * @throws IOException
   */
  public boolean hasNext() throws TalismaneException, IOException;

  /**
   * The next event to read.
   * 
   * @throws TalismaneException
   * @throws IOException
   */
  public ClassificationEvent next() throws TalismaneException, IOException;

  /**
   * Get the attributes defining this event stream.
   */
  public Map<String, String> getAttributes();
}
