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
package com.joliciel.talismane.extensions.standoff;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.extensions.Extensions.ExtendedCommand;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * Class for splitting a CoNNL file into lots of smaller files.
 * 
 * @author Assaf Urieli
 *
 */
public class ConllFileSplitter {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(ConllFileSplitter.class);

  private static DecimalFormat df = new DecimalFormat("000");

  public void split(File inFile, File outDir, int startIndex, int sentencesPerFile, String encoding) throws IOException {
    if (outDir != null)
      outDir.mkdirs();

    String fileBase = inFile.getName();
    if (fileBase.indexOf('.') > 0)
      fileBase = fileBase.substring(0, fileBase.lastIndexOf('.'));

    Writer writer = null;
    try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(inFile), encoding)))) {

      boolean hasSentence = false;
      int currentFileIndex = startIndex;

      int sentenceCount = 0;
      int rowCount = 0;

      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (line.length() == 0 && !hasSentence) {
          continue;
        } else if (line.length() == 0) {
          writer.write("\n");
          writer.flush();
          hasSentence = false;
        } else {
          if (!hasSentence) {
            hasSentence = true;
            sentenceCount++;
            rowCount = 0;
          }
          if (sentenceCount % sentencesPerFile == 1 && rowCount == 0) {
            if (writer != null) {
              writer.flush();
              writer.close();
            }
            File outFile = new File(outDir, fileBase + "_" + df.format(currentFileIndex) + ".tal");
            outFile.delete();
            outFile.createNewFile();

            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile, false), encoding));
            currentFileIndex++;
          }

          writer.write(line + "\n");
          writer.flush();
          rowCount++;
        }
      }
    } finally {
      if (writer != null) {
        writer.flush();
        writer.close();
      }
    }
  }

  public static void main(String[] args) throws IOException {
    OptionParser parser = new OptionParser();
    parser.accepts(ExtendedCommand.splitConllFile.name(), "split conll file into chunks");

    OptionSpec<File> inFileOption = parser.accepts("inFile", "input file").requiredIf(ExtendedCommand.splitConllFile.name()).withRequiredArg()
        .ofType(File.class);
    OptionSpec<File> outDirOption = parser.accepts("outDir", "output directory").requiredIf(ExtendedCommand.splitConllFile.name()).withRequiredArg()
        .ofType(File.class);
    OptionSpec<String> encodingOption = parser.accepts("encoding", "encoding for input and output").withRequiredArg().ofType(String.class);
    OptionSpec<Integer> sentencesPerFileOption = parser.accepts("sentencesPerFile", "number of sentences per chunk")
        .requiredIf(ExtendedCommand.splitConllFile.name()).withRequiredArg().ofType(Integer.class);
    OptionSpec<Integer> startIndexOption = parser.accepts("startIndex", "the sentence to start on").withRequiredArg().ofType(Integer.class);

    if (args.length <= 1) {
      parser.printHelpOn(System.out);
      return;
    }

    OptionSet options = parser.parse(args);

    File inFile = options.valueOf(inFileOption);
    File outDir = options.valueOf(outDirOption);

    String encoding = "UTF-8";
    if (options.has(encodingOption))
      encoding = options.valueOf(encodingOption);

    int sentencesPerFile = options.valueOf(sentencesPerFileOption);
    int startIndex = 1;
    if (options.has(startIndexOption))
      startIndex = options.valueOf(startIndexOption);

    ConllFileSplitter splitter = new ConllFileSplitter();
    splitter.split(inFile, outDir, startIndex, sentencesPerFile, encoding);
  }

}
