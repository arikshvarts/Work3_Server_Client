package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class StompEncoderDecoder implements MessageEncoderDecoder<Message> {

    private byte[] bytes = new byte[1 << 10]; // Initial buffer size: 1KB
    private int len = 0;


        @Override

    public Message decodeNextByte(byte nextByte) {
    // STOMP frames are null-terminated
    if (nextByte == '\u0000') {
        String rawFrame = popString(); // Extract the full frame as a String
        return parseFrame(rawFrame);  // Parse the raw frame into a Message object
    }
    pushByte(nextByte); // Accumulate bytes in the buffer
    return null;        // Frame not complete
}

    private Message parseFrame(String frame) {
        String[] parts = frame.split("\n", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid STOMP frame");
        }

        String command = parts[0].trim();
        String[] headersAndBody = parts[1].split("\n\n", 2);

        // Parse headers
        Map<String, String> headers = new HashMap<>();
        for (String headerLine : headersAndBody[0].split("\n")) {
            String[] headerParts = headerLine.split(":", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0].trim(), headerParts[1].trim());
            }
        }

        // Parse body
        String body = headersAndBody.length > 1 ? headersAndBody[1] : null;

        return new Message(command, headers, body);
    }

    private String popString() {
        // Convert the accumulated bytes into a UTF-8 string
        String result = new String(bytes, 0, len, StandardCharsets.UTF_8);
        len = 0; // Reset the buffer
        return result;
    }

 
    @Override
    public byte[] encode(Message message) {
        // Serialize the Message object into a STOMP frame and null-terminate it
        return (serializeFrame(message) + "\u0000").getBytes(StandardCharsets.UTF_8);
    }

    private String serializeFrame(Message message) {
        StringBuilder builder = new StringBuilder();
        builder.append(message.getCommand()).append("\n");

        // Add headers
        for (Map.Entry<String, String> header : message.getHeaders().entrySet()) {
            builder.append(header.getKey()).append(":").append(header.getValue()).append("\n");
        }

        builder.append("\n"); // Separate headers from the body

        // Add body (if present)
        if (message.getBody() != null) {
            builder.append(message.getBody());
        }

        return builder.toString();
    }

    private void pushByte(byte nextByte) {
        // Expand buffer size dynamically if needed
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, bytes.length * 2);
        }
        bytes[len++] = nextByte; // Add the byte to the buffer
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
