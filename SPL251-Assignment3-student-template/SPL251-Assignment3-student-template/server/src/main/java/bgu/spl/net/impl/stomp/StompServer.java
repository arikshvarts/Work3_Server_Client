package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;
import bgu.spl.net.impl.stomp.StompProtocol; 

public class StompServer {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java StompServer <port> <tpc|reactor>");
            return;
        }

        try {
            int port = Integer.parseInt(args[0]);
            String mode = args[1];

            if (mode.equalsIgnoreCase("tpc")) {
                // Start the server in Thread-Per-Client mode
                Server.threadPerClient(
                        port, // Port number
                        StompProtocol::new, // Protocol factory
                        StompEncoderDecoder::new // Encoder-decoder factory
                ).serve();
            } else if (mode.equalsIgnoreCase("reactor")) {
                // Start the server in Reactor mode
                Server.reactor(
                        Runtime.getRuntime().availableProcessors(), // Number of threads
                        port, // Port number
                        StompProtocol::new, // Protocol factory
                        StompEncoderDecoder::new // Encoder-decoder factory
                ).serve();
            } else {
                System.out.println("Invalid mode. Use 'tpc' or 'reactor'.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number. Please provide a valid integer.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
