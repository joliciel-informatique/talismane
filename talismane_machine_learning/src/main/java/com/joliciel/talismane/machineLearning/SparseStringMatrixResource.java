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

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.List;
import java.util.Map;

import com.joliciel.talismane.utils.JolicielException;

/**
 * A sparse matrix indexed by Strings for both rows and columns, and containing double values.
 * @author Assaf Urieli
 *
 */
public class SparseStringMatrixResource implements ExternalResource<Double> {
  private static final long serialVersionUID = 1L;
  private String name;
  
  Map<String,TObjectDoubleMap<String>> matrix = new THashMap<String, TObjectDoubleMap<String>>();
  
  public SparseStringMatrixResource(String name) {
    super();
    this.name = name;
  }

  public void add(String string1, String string2, double value) {
    TObjectDoubleMap<String> row = matrix.get(string1);
    if (row==null) {
      row = new TObjectDoubleHashMap<String>();
      matrix.put(string1, row);
    }
    row.put(string2, value);
  }

  @Override
  public Double getResult(List<String> keyElements) {
    if (keyElements.size()!=2)
      throw new JolicielException("SparseStringMatrixResource only possible with 2 key elements");
    return this.getResult(keyElements.get(0), keyElements.get(1));
  }
  
  public Double getResult(String string1, String string2) {
    Double value = null;
    TObjectDoubleMap<String> row = matrix.get(string1);
    if (row!=null && row.containsKey(string2)) {
      value = row.get(string2);
    }
    return value;
  }
  
  public double getValueOrZero(String string1, String string2) {
    double value = 0;
    TObjectDoubleMap<String> row = matrix.get(string1);
    if (row!=null) {
      value = row.get(string2);
    }
    return value;
  }

  public TObjectDoubleIterator<String> getInnerKeys(String key) {
    TObjectDoubleMap<String> row = matrix.get(key);
    if (row!=null) {
      return row.iterator();
    }
    return null;
  }
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
