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
//////////////////////////////////////////////////////////////////////////////package com.joliciel.talismane.parser;
package com.joliciel.talismane.parser.evaluate;

import java.io.IOException;
import java.util.List;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.output.ParseConfigurationProcessor;
import com.joliciel.talismane.posTagger.PosTagSequence;

/**
 * Simply a wrapper for the ParseConfigurationProcessor, processing the best guess
 * using the processor.
 * 
 * @author Assaf Urieli
 *
 */
public class ParseConfigurationProcessorWrapper implements ParseEvaluationObserver {
  private final ParseConfigurationProcessor processor;

  public ParseConfigurationProcessorWrapper(ParseConfigurationProcessor processor) {
    this.processor = processor;
  }

  @Override
  public void onParseEnd(ParseConfiguration realConfiguration, List<ParseConfiguration> guessedConfigurations) throws TalismaneException, IOException {
    processor.onNextParseConfiguration(guessedConfigurations.get(0));
  }

  @Override
  public void onEvaluationComplete() throws IOException {
    processor.onCompleteParse();
  }

  @Override
  public void onParseStart(ParseConfiguration realConfiguration, List<PosTagSequence> posTagSequences) {
  }
}
