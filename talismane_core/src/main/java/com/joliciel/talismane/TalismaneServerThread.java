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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;

/**
 * A server that processes the text which is sent on a socket. The assumption is
 * that the text should be handled as a single independent block. If the text
 * consists of the command "@SHUTDOWN", the thread will instruct the
 * TalismaneServer which created it to shut down as soon as all current threads
 * are finished processing.
 * 
 * @author Assaf Urieli
 *
 */
class TalismaneServerThread extends Thread {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(TalismaneServerThread.class);
	private final Socket socket;
	private final TalismaneServer server;
	private final TalismaneSession session;

	public TalismaneServerThread(TalismaneServer server, TalismaneSession session, Socket socket) {
		super("TalismaneServerThread");
		this.server = server;
		this.socket = socket;
		this.session = session;
	}

	@Override
	public void run() {
		try {
			OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), session.getOutputCharset());
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), session.getInputCharset()));

			Talismane talismane = new Talismane(session);

			// Set the various processors from the server, in case they were set
			// to override the default
			talismane.setParseConfigurationProcessor(this.server.getParseConfigurationProcessor());
			talismane.setPosTagSequenceProcessor(this.server.getPosTagSequenceProcessor());
			talismane.setTokenSequenceProcessor(this.server.getTokenSequenceProcessor());

			// For the sentence processor, wrap it in a processor that checks
			// for a shut down command
			SentenceProcessor sentenceProcessor = this.server.getSentenceProcessor();
			if (sentenceProcessor == null)
				sentenceProcessor = SentenceProcessor.getProcessor(session);
			ShutDownListener listener = new ShutDownListener(this.server, sentenceProcessor);
			talismane.setSentenceProcessor(listener);

			talismane.setReader(in);
			talismane.setWriter(out);

			talismane.analyse();

			socket.close();
		} catch (IOException | ReflectiveOperationException e) {
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
		public void onNextSentence(Sentence sentence, Writer writer) {
			if (sentence.getText().equals("@SHUTDOWN")) {
				server.setListening(false);
			} else {
				if (wrappedProcessor != null)
					wrappedProcessor.onNextSentence(sentence, writer);
			}
		}
	}
}
