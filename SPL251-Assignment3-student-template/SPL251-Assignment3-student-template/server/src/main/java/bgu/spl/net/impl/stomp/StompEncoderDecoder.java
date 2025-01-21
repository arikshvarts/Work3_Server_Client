package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class StompEncoderDecoder<T> implements MessageEncoderDecoder<T> {
    private byte[] bytes = new byte[1 << 10]; // Initial buffer size: 1KB
    private int len = 0;

    // Function to parse a STOMP frame into a generic type T
    private final Function<String, T> frameParser;

    // Function to serialize a generic type T into a STOMP frame
    private final Function<T, String> frameSerializer;

    public StompEncoderDecoder(Function<String, T> frameParser, Function<T, String> frameSerializer) {
        this.frameParser = frameParser;
        this.frameSerializer = frameSerializer;
    }

    @Override
    public T decodeNextByte(byte nextByte) {
        // STOMP frames are null-terminated
        if (nextByte == '\u0000') {
            String frame = popString(); // Convert bytes to string
            return frameParser.apply(frame);  // Parse the STOMP frame into a generic type T
        }
        pushByte(nextByte); // Accumulate bytes in the buffer
        return null;        // Frame not complete
    }

    @Override
    public byte[] encode(T message) {
        // Serialize the generic type T into a STOMP frame and null-terminate it
        return (frameSerializer.apply(message) + "\u0000").getBytes(StandardCharsets.UTF_8);
    }

    private void pushByte(byte nextByte) {
        // Expand buffer size dynamically if needed
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, bytes.length * 2);
        }
        bytes[len++] = nextByte; // Add the byte to the buffer
    }

    private String popString() {
        // Convert the accumulated bytes into a UTF-8 string
        String result = new String(bytes, 0, len, StandardCharsets.UTF_8);
        len = 0; // Reset the buffer
        return result;
    }
}