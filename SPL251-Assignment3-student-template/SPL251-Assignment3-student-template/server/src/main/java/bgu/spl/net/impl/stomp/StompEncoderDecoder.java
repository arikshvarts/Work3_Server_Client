package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class StompEncoderDecoder implements MessageEncoderDecoder<String> {

    private byte[] bytes = new byte[1 << 10]; // Initial buffer size: 1KB
    private int len = 0;

    @Override
    public String decodeNextByte(byte nextByte) {
        // STOMP frames are null-terminated
        if (nextByte == '\u0000') {
            String frame = popString(); // Convert bytes to string
            return parseFrame(frame);  // Parse the STOMP frame into a string
        }
        pushByte(nextByte); // Accumulate bytes in the buffer
        return null;        // Frame not complete
    }

    @Override
    public byte[] encode(String message) {
        // Serialize the string into a STOMP frame and null-terminate it
        return (serializeFrame(message) + "\u0000").getBytes(StandardCharsets.UTF_8);
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

    private String parseFrame(String frame) {
        // Parse the STOMP frame into its components (command, headers, and body)
        String[] parts = frame.split("\n", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid STOMP frame");
        }

        String command = parts[0].trim();
        String[] headersAndBody = parts[1].split("\n\n", 2);

        String headers = headersAndBody.length > 0 ? headersAndBody[0] : "";
        String body = headersAndBody.length > 1 ? headersAndBody[1] : "";

        return "COMMAND: " + command + "\nHEADERS: " + headers + "\nBODY: " + body;
    }

    private String serializeFrame(String message) {
        // Split the message into its components (command, headers, and body)
        String[] parts = message.split("\n", 3);

        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid message format for serialization");
        }

        String command = parts[0].trim();
        String headers = parts[1].trim();
        String body = parts[2].trim();

        // Construct the STOMP frame
        return command + "\n" + headers + "\n\n" + body;
    }
}
