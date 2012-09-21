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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.utils.PerformanceMonitor;

import opennlp.maxent.GISTrainer;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.model.DataIndexer;
import opennlp.model.EventStream;
import opennlp.model.MaxentModel;
import opennlp.model.TwoPassRealValueDataIndexer;

/**
 * A class for training and persisting Maxent Models.
 * @author Assaf Urieli
 *
 */
public class MaxentModelTrainer {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(MaxentModelTrainer.class);
	
	private int iterations = 100;
	private int cutoff = 5;
	private double sigma = 0;
	private double smoothing = 0;
	private MaxentModel model = null;
	private CorpusEventStream corpusEventStream;
	private EventStream eventStream;
	
	public MaxentModelTrainer(CorpusEventStream corpusEventStream) {
		super();
		this.corpusEventStream = corpusEventStream;
		this.eventStream = new MaxentEventStream(corpusEventStream);
	}

	public MaxentModelTrainer(EventStream eventStream) {
		super();
		this.eventStream = eventStream;
	}

	public MaxentModel trainModel() {
		try {
	    	DataIndexer dataIndexer = new TwoPassRealValueDataIndexer(eventStream, cutoff);
			GISTrainer trainer = new GISTrainer(true);

			PerformanceMonitor.startTask("MaxentModelTrainer.trainModel");
			try {
				model =  trainer.trainModel(iterations, dataIndexer, cutoff);
			} finally {
				PerformanceMonitor.endTask("MaxentModelTrainer.trainModel");
			}
			
			return model;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }
	
	/**
	 * Persist the model to a file.
	 * @param outputFile
	 */
	public void persistModel(File outputFile) {
		try {
			new SuffixSensitiveGISModelWriter(model, outputFile).persist();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Persist the model to an ouput stream.
	 * @param outputStream
	 */
	public void persistModel(OutputStream outputStream) {
		try {
			new MaxentModelWriter(model, outputStream).persist();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * The number of iterations for Maxent training.
	 * @return
	 */
	public int getIterations() {
		return iterations;
	}


	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	/**
	 * Cutoff for maxent training - features must appear at least this many times to be included in the model.
	 * Note that for numeric features, any value > 0 counts as 1 time for cutoff purposes.
	 * @return
	 */
	public int getCutoff() {
		return cutoff;
	}

	public void setCutoff(int cutoff) {
		this.cutoff = cutoff;
	}

	/**
	 * Sigma for Gaussian smoothing on maxent training.
	 * @return
	 */
	public double getSigma() {
		return sigma;
	}

	public void setSigma(double sigma) {
		this.sigma = sigma;
	}

	/**
	 * Additive smoothing parameter during maxent training.
	 * @return
	 */
	public double getSmoothing() {
		return smoothing;
	}

	public void setSmoothing(double smoothing) {
		this.smoothing = smoothing;
	}

	/**
	 * The corpus event stream which will be used to build the model.
	 */
	public CorpusEventStream getCorpusEventStream() {
		return corpusEventStream;
	}
	
}
