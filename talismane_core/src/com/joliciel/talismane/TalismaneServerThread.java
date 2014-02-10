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
package com.joliciel.talismane;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;

/**
 * A server that processes the text which is sent on a socket.
 * The assumption is that the text should be handled as a single independent block.
 * @author Assaf Urieli
 *
 */
class TalismaneServerThread extends Thread {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(TalismaneServerThread.class);
    private Socket socket = null;
    private TalismaneServer server = null;
    private TalismaneConfig config = null;
    private TalismaneServiceInternal talismaneService;
	private Locale locale;
	private PosTagSet posTagSet;
	private PosTaggerLexicon lexicon;
	private TransitionSystem transitionSystem;
	private LanguageSpecificImplementation implementation;
    
    public TalismaneServerThread(TalismaneServer server, TalismaneConfig config, Socket socket) {
        super("TalismaneServerThread");
        this.server = server;
        this.socket = socket;
        this.config = config;
        this.locale = TalismaneSession.getLocale();
        this.posTagSet = TalismaneSession.getPosTagSet();
        this.lexicon = TalismaneSession.getLexicon();
        this.transitionSystem = TalismaneSession.getTransitionSystem();
        this.implementation = TalismaneSession.getImplementation();
    }
     
    public void run() {
    	try {
    		TalismaneSession.setLocale(locale);
    		TalismaneSession.setLexicon(lexicon);
    		TalismaneSession.setPosTagSet(posTagSet);
    		TalismaneSession.setTransitionSystem(transitionSystem);
    		TalismaneSession.setImplementation(implementation);
    		
	        OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), config.getOutputCharset());
	        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), config.getInputCharset()));
            
	        Talismane talismane = talismaneService.getTalismane(config);
	        talismane.setParseConfigurationProcessor(this.server.getParseConfigurationProcessor());
	        talismane.setPosTagSequenceProcessor(this.server.getPosTagSequenceProcessor());
	        talismane.setTokenSequenceProcessor(this.server.getTokenSequenceProcessor());
	        talismane.setSentenceProcessor(this.server.getSentenceProcessor());
	        talismane.setStopOnError(this.server.isStopOnError());
	        talismane.setReader(in);
	        talismane.setWriter(out);
	        
	        ShutDownListener listener = new ShutDownListener(this.server, config.getSentenceProcessor());
	        talismane.setSentenceProcessor(listener);
	        
	        talismane.process();
	        
	    	socket.close();
	    } catch (IOException e) {
	        throw new RuntimeException(e);
	    }
    }
    
    private static final class ShutDownListener implements SentenceProcessor {
    	TalismaneServer server;
    	SentenceProcessor wrappedProcessor;
    	
    	public ShutDownListener(TalismaneServer server, SentenceProcessor sentenceProcessor) {
    		this.server = server;
    		this.wrappedProcessor = sentenceProcessor;
    	}
    	
		@Override
		public void onNextSentence(String sentence, Writer writer) {
        	if (sentence.equals("@SHUTDOWN")) {
        		server.setListening(false);
        	} else {
        		if (wrappedProcessor!=null)
        			wrappedProcessor.onNextSentence(sentence, writer);
        	}
		}
    }

	public TalismaneServiceInternal getTalismaneService() {
		return talismaneService;
	}

	public void setTalismaneService(TalismaneServiceInternal talismaneService) {
		this.talismaneService = talismaneService;
	}
}
