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
package com.joliciel.talismane.extensions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.joliciel.talismane.TalismaneMain;
import com.joliciel.talismane.extensions.standoff.ConllFileSplitter;

/**
 * Extensions use a command-line identical to {@link TalismaneMain}.<br>
 * <br>
 * To use extensions, various configuration settings need to be updated to refer
 * to the classes in this package, and in some cases fill in their specific
 * configuration settings. See the reference.conf file for details.<br>
 * <br>
 * The current exception is the --splitConllFile command, which has it's own
 * command line. See {@link ConllFileSplitter} for details.
 * 
 * 
 * @author Assaf Urieli
 *
 */
public class Extensions {

  public enum ExtendedCommand {
    toStandoff,
    fromStandoff,
    splitConllFile,
    corpusStatistics,
    posTaggerStatistics,
    nonProjectiveStatistics,
    modifyCorpus,
    projectify
  }

  public static void main(String[] args) throws Exception {
    if (args.length > 0) {
      Set<String> argSet = new HashSet<>(Arrays.asList(args));
      if (argSet.contains("--" + ExtendedCommand.splitConllFile.name())) {
        ConllFileSplitter.main(args);
        return;
      }
    }

    TalismaneMain.main(args);
  }
}
