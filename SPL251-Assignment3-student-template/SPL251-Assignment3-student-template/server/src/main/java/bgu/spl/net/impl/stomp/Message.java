package bgu.spl.net.impl.stomp;

import java.util.Map;

public class Message {
    private String command;
    private Map<String, String> headers;
    private String body;

    public Message(String command, Map<String, String> headers, String body) {
        this.command = command;
        this.headers = headers;
        this.body = body;
    }

    public String getCommand() {
        return command;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(command + "\n");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.append(header.getKey()).append(":").append(header.getValue()).append("\n");
        }
        builder.append("\n").append(body != null ? body : "").append("\u0000");
        return builder.toString();
    }
}
