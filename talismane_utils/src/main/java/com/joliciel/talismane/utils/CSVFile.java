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
package com.joliciel.talismane.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * A class allowing us to extract information from a CSV file.
 */
public class CSVFile {
  private CSVFormatter formatter = new CSVFormatter();
  List<List<String>> cellMatrix = new ArrayList<List<String>>();

  public CSVFile(File file, String encoding) {
    try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding)))) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        List<String> cells = formatter.getCSVCells(line);
        cellMatrix.add(cells);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return the string value of a cell, or null if the cell is not in the CSV
   * file limits.
   * 
   * @param label
   *          in standard Excel format, e.g. A1 for top-left.
   */
  public String getValue(String label) {
    int row = formatter.getRowIndex(label);
    int column = formatter.getColumnIndex(label);
    return this.getValue(row, column);
  }

  /**
   * Return the string value of a cell, or null if the cell is not in the CSV
   * file limits.
   * 
   * @param row
   *          zero-indexed row index, e.g. 0 for A1
   * @param column
   *          zero-indexed column index, e.g. 0 for A1
   */
  public String getValue(int row, int column) {
    String result = null;
    if (cellMatrix.size() > row) {
      List<String> rowCells = cellMatrix.get(row);
      if (rowCells.size() > column) {
        result = rowCells.get(column);
      }
    }
    return result;
  }

  public int numRows() {
    return cellMatrix.size();
  }

  public int numColumns(int row) {
    if (cellMatrix.size() > row) {
      return cellMatrix.get(row).size();
    } else {
      return 0;
    }
  }
}
