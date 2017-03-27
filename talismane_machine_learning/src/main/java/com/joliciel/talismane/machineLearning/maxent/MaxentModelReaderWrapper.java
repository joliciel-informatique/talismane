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
package com.joliciel.talismane.machineLearning.maxent;

import java.io.IOException;
import java.io.InputStream;

import opennlp.maxent.io.GISModelReader;
import opennlp.model.AbstractModel;
import opennlp.model.AbstractModelReader;
import opennlp.model.BinaryFileDataReader;

class MaxentModelReaderWrapper extends AbstractModelReader {
  AbstractModelReader reader = null;
  
  public MaxentModelReaderWrapper(InputStream inputStream) {
    super(new BinaryFileDataReader(inputStream));
    reader = new GISModelReader(super.dataReader);
  }

  @Override
  public void checkModelType() throws IOException {
    reader.checkModelType();
  }

  @Override
  public AbstractModel constructModel() throws IOException {
    return reader.constructModel();
  }

}
