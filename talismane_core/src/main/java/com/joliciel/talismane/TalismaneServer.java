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
import java.net.ServerSocket;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.utils.LogUtils;

/**
 * A Talismane server, for loading all resources up front and processing
 * sentences received on the fly.
 * 
 * @author Assaf Urieli
 *
 */
public class TalismaneServer extends Talismane {
	private static final Logger LOG = LoggerFactory.getLogger(TalismaneServer.class);
	private final int port;
	private boolean listening = true;
	private final TalismaneConfig config;
	private final TalismaneSession talismaneSession;

	public TalismaneServer(TalismaneConfig config, TalismaneSession talismaneSession) {
		super(config, talismaneSession);

		this.config = config;
		this.talismaneSession = talismaneSession;
		this.port = config.getPort();
	}

	@Override
	public void process() {
		long startTime = new Date().getTime();
		ServerSocket serverSocket = null;

		try {
			LOG.info("Starting server...");
			config.preloadResources();

			serverSocket = new ServerSocket(port);
			LOG.info("Server started. Waiting for clients...");
			while (listening) {
				TalismaneServerThread thread = new TalismaneServerThread(this, config, talismaneSession, serverSocket.accept());
				thread.start();
			}
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} finally {
			if (serverSocket != null && !serverSocket.isClosed()) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					LogUtils.logError(LOG, e);
					throw new RuntimeException(e);
				}
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
