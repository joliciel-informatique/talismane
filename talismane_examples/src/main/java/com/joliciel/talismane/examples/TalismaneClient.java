///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Joliciel Informatique
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
package com.joliciel.talismane.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 * A class showing how to communicate with Talismane running in client/server
 * mode.<br/>
 * Before running this class, start Talismane in server mode via:<br/>
 * 
 * <pre>
 * java -Xmx2G -Dconfig.file=[languagePackConfigFile] -jar talismane-core-X.X.Xb.jar mode=server encoding=UTF-8
 * </pre>
 * 
 * Next, assuming you have compiled the present project via javac, try the
 * following command<br/>
 * 
 * <pre>
 * java com.joliciel.talismane.examples.TalismaneClient localhost 7272
 * </pre>
 * 
 * Type as many sentences as you like (note that a Windows console might not
 * handle accents very well).<br/>
 * When you're finished, type a carriage return without any text. At this point,
 * the server will analyse the sentences.<br/>
 * 
 * You can try the same command several times, showing that the server is in
 * listening mode.<br/>
 * 
 * @author Assaf Urieli
 *
 */
public class TalismaneClient {
	public static void main(String[] args) throws IOException {

		if (args.length != 2) {
			System.err.println("Usage: java com.joliciel.talismane.examples.TalismaneClient <host name> <port number>");
			System.exit(1);
		}

		String hostName = args[0];
		int portNumber = Integer.parseInt(args[1]);

		// open socket to server
		Socket socket = new Socket(hostName, portNumber);
		OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), Charset.forName("UTF-8"));
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.forName("UTF-8")));

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		String fromServer;
		String fromUser;

		// Get next input from the user, ending with a blank line
		String input = "\f";
		while ((fromUser = stdIn.readLine()) != null) {
			System.out.println("Client: " + fromUser);
			if (fromUser.length() == 0)
				break;
			input += fromUser + "\n";
		}

		// end input with three end-of-block characters to indicate the input is
		// finished
		input += "\f\f\f";

		// Send user input to the server
		out.write(input);
		out.flush();

		// Display output from server
		while ((fromServer = in.readLine()) != null) {
			System.out.println("Server: " + fromServer);
		}

		socket.close();
	}
}
