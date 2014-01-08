///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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
package com.joliciel.talismane;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.ServerSocket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;
import com.joliciel.talismane.utils.LogUtils;

class TalismaneServerImpl implements TalismaneServer {
	private static final Log LOG = LogFactory.getLog(TalismaneServerImpl.class);
	private int port = 7272;
	boolean listening = true;
	TalismaneConfig config = null;
	TalismaneServiceInternal talismaneService;
	SentenceProcessor sentenceProcessor;
	TokenSequenceProcessor tokenSequenceProcessor;
	PosTagSequenceProcessor posTagSequenceProcessor;
	ParseConfigurationProcessor parseConfigurationProcessor;
	boolean stopOnError = false;
	Reader reader;
	Writer writer;
	
	public TalismaneServerImpl(TalismaneConfig config) {
		this.config = config;
	}

	@Override
	public void process() {
        try {
        	// ping the lexicon to load it
        	TalismaneSession.getLexicon();
        	LOG.debug("Starting server");
        	
            ServerSocket serverSocket = new ServerSocket(port);
            while (listening) {
            	TalismaneServerThread thread = new TalismaneServerThread(this, config, serverSocket.accept());
            	thread.setTalismaneService(this.getTalismaneService());
                thread.start();
            }
        } catch (IOException e) {
            LogUtils.logError(LOG, e);
            throw new RuntimeException(e);
        }
	}
	
	/**
	 * The port to listen on - default is 7272.
	 * @param port
	 */
	@Override
	public int getPort() {
		return port;
	}

	@Override
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Whether or not any new connections will be accepted.
	 * @return
	 */
	@Override
	public boolean isListening() {
		return listening;
	}

	@Override
	public void setListening(boolean listening) {
		this.listening = listening;
	}

	public TalismaneServiceInternal getTalismaneService() {
		return talismaneService;
	}

	public void setTalismaneService(TalismaneServiceInternal talismaneService) {
		this.talismaneService = talismaneService;
	}

	public SentenceProcessor getSentenceProcessor() {
		return sentenceProcessor;
	}

	public void setSentenceProcessor(SentenceProcessor sentenceProcessor) {
		this.sentenceProcessor = sentenceProcessor;
	}

	public TokenSequenceProcessor getTokenSequenceProcessor() {
		return tokenSequenceProcessor;
	}

	public void setTokenSequenceProcessor(
			TokenSequenceProcessor tokenSequenceProcessor) {
		this.tokenSequenceProcessor = tokenSequenceProcessor;
	}

	public PosTagSequenceProcessor getPosTagSequenceProcessor() {
		return posTagSequenceProcessor;
	}

	public void setPosTagSequenceProcessor(
			PosTagSequenceProcessor posTagSequenceProcessor) {
		this.posTagSequenceProcessor = posTagSequenceProcessor;
	}

	public ParseConfigurationProcessor getParseConfigurationProcessor() {
		return parseConfigurationProcessor;
	}

	public void setParseConfigurationProcessor(
			ParseConfigurationProcessor parseConfigurationProcessor) {
		this.parseConfigurationProcessor = parseConfigurationProcessor;
	}

	public boolean isStopOnError() {
		return stopOnError;
	}

	public void setStopOnError(boolean stopOnError) {
		this.stopOnError = stopOnError;
	}

	public Reader getReader() {
		return reader;
	}

	public void setReader(Reader reader) {
		this.reader = reader;
	}

	public Writer getWriter() {
		return writer;
	}

	public void setWriter(Writer writer) {
		this.writer = writer;
	}


	
}
