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

import java.io.IOException;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Talismane server, for loading all resources up front and processing
 * sentences received on the fly.
 * 
 * @author Assaf Urieli
 *
 */
public class TalismaneServer {
  private static final Logger LOG = LoggerFactory.getLogger(TalismaneServer.class);
  private final int port;
  private boolean listening = true;
  private final TalismaneSession session;

  public TalismaneServer(TalismaneSession session) throws IOException, ReflectiveOperationException {
    this.session = session;
    this.port = session.getPort();
  }

  public void analyse() throws IOException, ReflectiveOperationException, TalismaneException {
    long startTime = new Date().getTime();
    ServerSocket serverSocket = null;

    try {
      LOG.info("Starting server...");

      // load Talismane to load any required resources
      StringWriter out = new StringWriter();
      @SuppressWarnings("unused")
      Talismane talismane = new Talismane(out, null, session);

      serverSocket = new ServerSocket(port);
      LOG.info("Server started. Waiting for clients...");
      while (listening) {
        TalismaneServerThread thread = new TalismaneServerThread(session, serverSocket.accept());
        thread.start();
      }
    } finally {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
      LOG.info("Server shut down.");
      long endTime = new Date().getTime();
      long totalTime = endTime - startTime;
      LOG.info("Total server run time (ms): " + totalTime);
    }
  }

  /**
   * The port to listen on.
   */
  public int getPort() {
    return port;
  }

  /**
   * Whether or not any new connections will be accepted.
   */
  public boolean isListening() {
    return listening;
  }

  public void setListening(boolean listening) {
    this.listening = listening;
  }
}
