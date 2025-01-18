package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class StompEncoderDecoder<Message> implements MessageEncoderDecoder<Message> {
    private byte[] bytes = new byte[1 << 10]; // Initial buffer size: 1KB
    private int len = 0;

    @Override
    public Message decodeNextByte(byte nextByte) {
        // STOMP frames are null-terminated
        if (nextByte == '\u0000') {
            String frame = popString(); // Convert bytes to string
            return parseFrame(frame);  // Parse the STOMP frame into a Message object
        }
        pushByte(nextByte); // Accumulate bytes in the buffer
        return null;        // Frame not complete
    }

    @Override
    public byte[] encode(Message message) {
        // Serialize the Message object into a STOMP frame and null-terminate it
        return (message.toString() + "\u0000").getBytes(StandardCharsets.UTF_8);
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

    private Message parseFrame(String frame) {
        // Split the frame into headers and body based on double newline
        String[] parts = frame.split("\n\n", 2);
        String[] headerLines = parts[0].split("\n"); // Split headers by newline

        // Extract the command from the first line
        String command = headerLines[0];

        // Parse headers into a map
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < headerLines.length; i++) {
            String[] headerParts = headerLines[i].split(":", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0].trim(), headerParts[1].trim());
            }
        }

        // Extract body (if present)
        String body = parts.length > 1 ? parts[1] : "";

        // Return the parsed Message object
        return new Message(command, headers, body);
    }
}
