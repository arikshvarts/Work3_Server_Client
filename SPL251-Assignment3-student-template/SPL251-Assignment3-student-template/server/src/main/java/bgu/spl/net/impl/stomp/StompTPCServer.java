package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.srv.BlockingConnectionHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public abstract class StompTPCServer<T> implements StompServerInterface<T> {

  private final int port;
    private final Supplier<StompProtocol<T>> protocolFactory;
    private final Supplier<StompEncoderDecoder> encdecFactory;
    private ServerSocket sock;
    private StompConnections connections;
    private int idClient = 1;

    public StompTPCServer(
            int port,
            Supplier<StompProtocol<T>> protocolFactory,
            Supplier<StompEncoderDecoder> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.sock = null;
        this.connections = new StompConnections<>();
    }

    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");

            this.sock = serverSock; //just to be able to close
            while (!Thread.currentThread().isInterrupted()) {

                Socket clientSock = serverSock.accept();

                StompBlockingConnectionHandler<T> handler = new StompBlockingConnectionHandler<>(
                        clientSock,
                        encdecFactory.get(),
                        protocolFactory.get());
                connections.getClients().put(idClient, handler);//check what id i want here
                handler.getProtocol().start(idClient, connections);//check what id i want here
                idClient++;
                execute(handler);
            }
        } catch (IOException ex) {
        }

        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
    }

    protected  void execute(StompBlockingConnectionHandler<T>  handler)
    {
        try {
            new Thread(handler).start();
        } catch (Exception e) {
            System.err.println("Failed to start client handler: " + e.getMessage());
        }
    }
}
