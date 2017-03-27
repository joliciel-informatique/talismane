import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;

public class TalismaneClient {
  public static void main(String[] args) throws IOException {

    if (args.length != 2) {
      System.err.println(
          "Usage: java TalismaneClient <host name> <port number>");
      System.exit(1);
    }

    String hostName = args[0];
    int portNumber = Integer.parseInt(args[1]);

    // open socket to server
    Socket socket = new Socket(hostName, portNumber);
    OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), Charset.forName("UTF-8"));
    BufferedReader in = new BufferedReader(
        new InputStreamReader(socket.getInputStream(), Charset.forName("UTF-8")));

    BufferedReader stdIn =
      new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
    String fromServer;
    String fromUser;

    // Get next input from the user, ending with a blank line
    String input = "\f";
    while ((fromUser = stdIn.readLine()) != null) {
      System.out.println("Client: " + fromUser);
      if (fromUser.length()==0)
        break;
      input += fromUser + "\n";
    }
    
    // end input with three end-of-block characters to indicate the input is finished
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
