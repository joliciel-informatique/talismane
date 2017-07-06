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
package com.joliciel.talismane.posTagger.evaluate;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.output.PosTagSequenceProcessor;

/**
 * Simply a wrapper for the PosTagSequenceProcessor, writing the best guess
 * using the processor.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTagSequenceProcessorWrapper implements PosTagEvaluationObserver {
  Writer writer;
  PosTagSequenceProcessor processor;

  public PosTagSequenceProcessorWrapper(PosTagSequenceProcessor processor) {
    this.processor = processor;
  }

  @Override
  public void onNextPosTagSequence(PosTagSequence realSequence, List<PosTagSequence> guessedSequences) throws TalismaneException, IOException {
    processor.onNextPosTagSequence(guessedSequences.get(0));
  }

  @Override
  public void onEvaluationComplete() {
  }

}
