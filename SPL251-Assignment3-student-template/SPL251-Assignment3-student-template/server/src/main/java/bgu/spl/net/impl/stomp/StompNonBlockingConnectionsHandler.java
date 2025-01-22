package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Reactor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class StompNonBlockingConnectionsHandler<T> implements ConnectionHandler<T>  {
    private static final int BUFFER_ALLOCATION_SIZE = 1 << 13; //8k
    private static final ConcurrentLinkedQueue<ByteBuffer> BUFFER_POOL = new ConcurrentLinkedQueue<>();

    private final StompProtocol<T> protocol;
    private final StompEncoderDecoder encdec;
    private final Queue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<>();
    private final SocketChannel chan;
    private final StompReactor reactor;


    public StompNonBlockingConnectionsHandler(
            StompEncoderDecoder reader,
            StompProtocol<T> protocol,
            SocketChannel chan,
            StompReactor reactor) {
        this.chan = chan;
        this.encdec = reader;
        this.protocol = protocol;
        this.reactor = reactor;
    }

   public Runnable continueRead() {
        ByteBuffer buf = leaseBuffer();

        boolean success = false;
        try {
            success = chan.read(buf) != -1;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (success) {
            buf.flip();
            return () -> {
                try {
                    while (buf.hasRemaining()) {
                        Message nextMessage = encdec.decodeNextByte(buf.get());
                        if (nextMessage != null) {
                            T message = (T) nextMessage;
                            protocol.process(message);
                        }
                    }
                } finally {
                    releaseBuffer(buf);
                }
            };
        } else {
            releaseBuffer(buf);
            close();
            return null;
        }   

    }

    public void close() {
        try {
            chan.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isClosed() {
        return !chan.isOpen();
    }

    public void continueWrite() {
        while (!writeQueue.isEmpty()) {
            try {
                ByteBuffer top = writeQueue.peek();
                chan.write(top);
                if (top.hasRemaining()) {
                    return;
                } else {
                    writeQueue.remove();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                close();
            }
        }

        if (writeQueue.isEmpty()) {
            if (protocol.shouldTerminate()) close();
            else reactor.updateInterestedOps(chan, SelectionKey.OP_READ);
        }
    }

    private static ByteBuffer leaseBuffer() {
        ByteBuffer buff = BUFFER_POOL.poll();
        if (buff == null) {
            return ByteBuffer.allocateDirect(BUFFER_ALLOCATION_SIZE);
        }

        buff.clear();
        return buff;
    }

    private static void releaseBuffer(ByteBuffer buff) {
        BUFFER_POOL.add(buff);
    }

// private execute(StompNonBlockingConnectionsHandler<T> handler, Runnable task) {
//         if (task != null) {
//             task.run();
//         }
//     }

// @Override
// public void send(T msg) {
//     try {
//         byte[] encodedMsg = encdec.encode((String) msg); // Serialize the message
//         writeQueue.add(ByteBuffer.wrap(encodedMsg)); // Enqueue the message
//         reactor.updateInterestedOps(chan, SelectionKey.OP_READ | SelectionKey.OP_WRITE); // Enable write readiness
//     } catch (Exception e) {
//         e.printStackTrace();
//         close(); // Close the connection on error
//     }
// }

    @Override
    public void send(T msg) {
        try {
            Message message = (Message) msg;
            // Serialize the Message object into bytes
            byte[] encodedMsg = encdec.encode(message);
            writeQueue.add(ByteBuffer.wrap(encodedMsg));

            // Update the reactor to enable write readiness
            reactor.updateInterestedOps(chan, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        } catch (ClassCastException e) {
            System.err.println("Error: The provided message is not of type Message: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            close(); // Close the connection on error
        }
    }


public StompProtocol getProtocol(){
    return protocol;
}
}

