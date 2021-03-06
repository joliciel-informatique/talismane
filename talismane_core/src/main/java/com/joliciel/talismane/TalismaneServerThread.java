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
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A server that processes the text which is sent on a socket. The assumption is
 * that the text should be handled as a single independent block.
 * 
 * @author Assaf Urieli
 *
 */
class TalismaneServerThread extends Thread {
  private static final Logger LOG = LoggerFactory.getLogger(TalismaneServerThread.class);
  private final Socket socket;
  private final String sessionId;

  public TalismaneServerThread(Socket socket, String sessionId) {
    super("TalismaneServerThread");
    this.socket = socket;
    this.sessionId = sessionId;
  }

  @Override
  public void run() {
    try {
      OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), TalismaneSession.get(sessionId).getOutputCharset());
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), TalismaneSession.get(sessionId).getInputCharset()));

      Talismane talismane = new Talismane(out, null, sessionId);
      talismane.analyse(in);
 
      socket.close();
    } catch (RuntimeException e) {
      LOG.error(e.getMessage(), e);
      throw e;
    } catch (IOException | ReflectiveOperationException e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }
}
