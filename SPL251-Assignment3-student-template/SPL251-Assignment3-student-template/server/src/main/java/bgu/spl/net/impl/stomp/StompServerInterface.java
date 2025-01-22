package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import java.io.Closeable;
import java.util.function.Supplier;

public interface StompServerInterface<T> extends Closeable {

    /**
     * The main loop of the server, Starts listening and handling new clients.
     */
    void serve();

    /**
     *This function returns a new instance of a thread per client pattern server
     * @param port The port for the server socket
     * @param protocolFactory A factory that creats new MessagingProtocols
     * @param encoderDecoderFactory A factory that creats new MessageEncoderDecoder
     * @param <T> The Message Object for the protocol
     * @return A new Thread per client server
     */
    public static <T> StompServerInterface<T>  StompTPCServer(
            int port,
            Supplier<StompProtocol<T> > protocolFactory,
            Supplier<StompEncoderDecoder> encoderDecoderFactory) {

        return new StompTPCServer<T>(port, protocolFactory, encoderDecoderFactory) {
            @Override
            protected void execute(StompBlockingConnectionHandler<T>  handler) {
                new Thread(handler).start();
            }
        };

    }

    /**
     * This function returns a new instance of a reactor pattern server
     * @param nthreads Number of threads available for protocol processing
     * @param port The port for the server socket
     * @param protocolFactory A factory that creats new MessagingProtocols
     * @param encoderDecoderFactory A factory that creats new MessageEncoderDecoder
     * @param <T> The Message Object for the protocol
     * @return A new reactor server
     */
    public static <T> StompServerInterface<T> StompReactor(
            int nthreads,
            int port,
            Supplier<StompProtocol<T>> protocolFactory,
            Supplier<StompEncoderDecoder> encoderDecoderFactory) {
        return new StompReactor<T>(nthreads, port, protocolFactory, encoderDecoderFactory);
    }

}
