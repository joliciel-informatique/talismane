///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import opennlp.model.MaxentModel;
import opennlp.perceptron.BinaryPerceptronModelWriter;
import opennlp.perceptron.PerceptronModelWriter;

/**
 * A Perceptron model writer that can write directly to an OutputStream.
 * @author Assaf Urieli
 *
 */
class OpenNLPPerceptronModelWriterWrapper extends PerceptronModelWriter {
	private final BinaryPerceptronModelWriter writer;
	private OutputStream outputStream;
	
	public OpenNLPPerceptronModelWriterWrapper(MaxentModel model, OutputStream outputStream) {
		super(model);
		writer = new BinaryPerceptronModelWriter(model,
	            new DataOutputStream(outputStream));
		this.outputStream = outputStream;
	}

	@Override
	public void writeUTF(String s) throws IOException {
		writer.writeUTF(s);
	}

	@Override
	public void writeInt(int i) throws IOException {
		writer.writeInt(i);
	}

	@Override
	public void writeDouble(double d) throws IOException {
		writer.writeDouble(d);
	}

	@Override
	public void close() throws IOException {
		outputStream.flush();
	}
}
