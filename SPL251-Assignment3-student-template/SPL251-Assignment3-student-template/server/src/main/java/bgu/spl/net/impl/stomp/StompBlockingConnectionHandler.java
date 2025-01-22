package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.ConnectionHandler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.impl.stomp.StompConnections;
public class StompBlockingConnectionHandler<T> implements ConnectionHandler<T> ,Runnable{

	private final StompProtocol<T> protocol;
    private final StompEncoderDecoder encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;

    public StompBlockingConnectionHandler(Socket sock, StompEncoderDecoder reader, StompProtocol<T> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
    }

	@Override
	public void run() {
		try (Socket sock = this.sock) { // Auto-close the socket
			in = new BufferedInputStream(sock.getInputStream());
			out = new BufferedOutputStream(sock.getOutputStream());
	
			while (!protocol.shouldTerminate() && connected) {
				try {
					int read = in.read(); // Blocking call to read bytes
					if (read < 0) break;  // End of stream reached
	
					Message nextMessage = encdec.decodeNextByte((byte) read);
					if (nextMessage != null) {
						protocol.process((T) nextMessage); // Delegate processing and response to protocol
					}
				} catch (IOException e) {
					System.err.println("Error during client communication: " + e.getMessage());
					break;
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			close();
		}
	}
	public void close() {
		try {
			connected = false;
			sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	// @Override
	// public void send(T msg) {
	// 	try {
	// 		byte[] encodedMsg = encdec.encode((String) msg); // Serialize the message
	// 		out.write(encodedMsg);                 // Write to the output stream
	// 		out.flush();                           // Ensure the message is sent
	// 	} catch (IOException e) {
	// 		e.printStackTrace();
	// 		connected = false; // Mark the connection as closed
	// 	}
	// }
	
	public StompProtocol getProtocol(){
		return protocol;
	}
	@Override
public void send(T msg) {
    try {
        Message message = (Message) msg;
        // Serialize the Message object into bytes
        byte[] encodedMsg = encdec.encode(message);
        // Write the encoded bytes to the output stream
        out.write(encodedMsg);
        out.flush(); // Ensure the message is sent
    } catch (IOException e) {
        e.printStackTrace();
        connected = false; // Mark the connection as closed
    } catch (ClassCastException e) {
        System.err.println("Error: The provided message is not of type Message: " + e.getMessage());
    }
}


}
