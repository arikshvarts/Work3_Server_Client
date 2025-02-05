package bgu.spl.net.impl.stomp;
import java.util.HashMap;
import java.util.Map;

import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.impl.stomp.Message;;


public class StompProtocol<T> implements StompMessagingProtocol<T> {
    private boolean shouldTerminate = false;
    private Connections<T> connections;
    private int connectionId;
     
    
    public void start(int connectionId, Connections<T> connections){
        this.connectionId = connectionId;
        this.connections = connections; //object that implement Connctions interface
    }
    
    public void process(T msg){
        // try {
        System.out.println("Halooooooooo");
        // Message frame = parseMessage((String) msg); // Convert the String to a Message
        Message frame = (Message)msg;


        switch (frame.getCommand()) {
            case "CONNECT":
                System.out.println("CASE CONNECT");
                handleConnect(frame);
                break;

            case "SEND":
                handleSend(frame);
                break;

            case "SUBSCRIBE":
                handleSubscribe(frame);
                break;

            case "UNSUBSCRIBE":
                handleUnsubscribe(frame);
                break;

            case "DISCONNECT":
                handleDisconnect(frame);
                break;

            default:
                // sendError("Unknown command: " + msg.getCommand());
        }
        //if the frame from the client contains reciept, response with RECEIPT frame
        String receiptId = frame.getHeaders().get("receipt");
        if (receiptId != null) {
        HashMap<String, String> receiptHeaders = new HashMap<>();
        receiptHeaders.put("receipt-id", receiptId);
        Message receiptFrame = new Message("RECEIPT", receiptHeaders, null);
        ((StompConnections<T>) connections).send(connectionId, (T) receiptFrame.toString());
        }
    //     // }
    //  catch (IllegalArgumentException e) {
    //         // Handle invalid frames and send an ERROR frame if necessary
    //         System.err.println("Invalid frame received: " + msg);
    //     }

    }

    @Override
    public boolean shouldTerminate(){
        return shouldTerminate;
    }


//handlers method to each specific frame type from client
    public void handleConnect(Message msg){
        System.out.println("proccessing CONNECT ");
        //server responds by sending a CONNECTED Message to registered Clients
        String clientVersion = msg.getHeaders().get("accept-version");
        String login = msg.getHeaders().get("login");
        String passcode = msg.getHeaders().get("passcode");
        if(((StompConnections<T>) connections).handle_credentials(login, passcode) == false){
        //false:send error frame, true:correct credentials/update credentials if the user is new
        HashMap<String, String> errorHeaders = new HashMap<>();
            errorHeaders.put("message:", "unmatch credentials"); 
            if(msg.getHeaders().get("receipt") != null){
            //if the frame from the client include receipt, include it in the ERROR headers
                errorHeaders.put("receipt", msg.getHeaders().get("receipt")); 
            }
            String errorBody = "The message:\n-----\n" + msg.toString() + "\n-----\n" +
            "The passcode doesn't match the login or credentials are not in format";
            Message errorFrame = new Message("ERROR", errorHeaders , errorBody);

            ((StompConnections<T>) connections).send(connectionId, (T) errorFrame);
            connections.disconnect(connectionId); // Close the connection
            shouldTerminate = true;
        }
        if(!clientVersion.equals("1.2")){
            //we need to send an ERROR frame
            HashMap<String, String> errorHeaders = new HashMap<>();
            errorHeaders.put("message:", "STOMP version different then 1.2"); 
            if(msg.getHeaders().get("receipt") != null){
            //if the frame from the client include receipt, include it in the ERROR headers
                errorHeaders.put("receipt", msg.getHeaders().get("receipt")); 
            }
            String errorBody = "The message:\n-----\n" + msg.toString() + "\n-----\n" +
            "Did not specify a supported version. Only version 1.2 is supported.";
            Message errorFrame = new Message("ERROR", errorHeaders , errorBody);

            ((StompConnections<T>) connections).send(connectionId, (T) errorFrame);
            connections.disconnect(connectionId); // Close the connection
            shouldTerminate = true;

        }
        else{
        HashMap<String, String> connectedHeaders = new HashMap<>();
        connectedHeaders.put("version", clientVersion); // Negotiated version
        Message connectedFrame = new Message("CONNECTED", connectedHeaders, null);
        //send the CONNECTED frame to the specific cliend tried to connect
        ((StompConnections<T>) connections).send(connectionId, (T) connectedFrame); 
        }

    }

    public void handleDisconnect(Message msg){
        System.out.println("proccessing DISCONNECT ");
        connections.disconnect(connectionId);
            shouldTerminate = true;
            System.out.println("Client disconnected: ID=" + connectionId);
    }

    public void handleSend(Message msg){
        System.out.println("proccessing SEND ");
        String topic = msg.getHeaders().get("destination");
        if(((StompConnections)connections).isSubscribed(connectionId, topic) == false){
            //client who is not subscribed to a channel cant send message to this channel
            //we need to send an ERROR frame
            HashMap<String, String> errorHeaders = new HashMap<>();
            errorHeaders.put("message:", "sender is not subscribed"); 
            if(msg.getHeaders().get("receipt") != null){
            //if the frame from the client include receipt, include it in the ERROR headers
                errorHeaders.put("receipt", msg.getHeaders().get("receipt")); 
            }
            String errorBody = "The message:\n-----\n" + msg.toString() + "\n-----\n" +
            "client who is not subscribed to a channel cant send message to this channel";
            Message errorFrame = new Message("ERROR", errorHeaders , errorBody);

            ((StompConnections<T>) connections).send(connectionId, (T) errorFrame);
            ((StompConnections<T>) connections).disconnect(connectionId); // Close the connection
            shouldTerminate = true;
        }
        else{
        String message = msg.getBody();
        HashMap<String, String> messageHeaders = new HashMap<>();
        messageHeaders.put("subscription:",((StompConnections<T>)connections).get_clientsTo_subscriptionsId().get(connectionId).get(topic).toString());
        messageHeaders.put("message-id: ", ((StompConnections<T>)connections).generateMessageId());
        Message messageFrame = new Message("Message", messageHeaders, msg.getBody());
        //sending the message the client had to the channel
        ((StompConnections<T>) connections).send(topic, (T)messageFrame);
        }
    }

    public void handleSubscribe(Message msg){
        System.out.println("proccessing SUBSCRIBE ");
        String destination = msg.getHeaders().get("destination");
        String subscriptionIdStr = msg.getHeaders().get("id");

        if (destination == null || subscriptionIdStr == null) {
        // Send ERROR frame for missing headers
        HashMap<String, String> errorHeaders = new HashMap<>();
        errorHeaders.put("message", "Missing 'destination' or 'id' header in SUBSCRIBE frame");
        if(msg.getHeaders().get("receipt") != null){
            //if the frame from the client include receipt, include it in the ERROR headers
            errorHeaders.put("receipt", msg.getHeaders().get("receipt")); 
        }
        Message errorFrame = new Message("ERROR", errorHeaders, null);
        connections.send(connectionId, (T) errorFrame.toString());
        connections.disconnect(connectionId);
        shouldTerminate = true;
        return;
        }

            int subscriptionId;
        try {
            subscriptionId = Integer.parseInt(subscriptionIdStr);
        } catch (NumberFormatException e) {
            // Send ERROR frame for invalid subscription ID
            HashMap<String, String> errorHeaders = new HashMap<>();
            errorHeaders.put("message", "Invalid 'id' in SUBSCRIBE frame: " + subscriptionIdStr);        
            if(msg.getHeaders().get("receipt") != null){
                //if the frame from the client include receipt, include it in the ERROR headers
                errorHeaders.put("receipt", msg.getHeaders().get("receipt")); 
            }
            Message errorFrame = new Message("ERROR", errorHeaders, null);
            connections.send(connectionId, (T) errorFrame.toString());
            connections.disconnect(connectionId);
            shouldTerminate = true;
            return;
        }

        // Check if already subscribed
        if (!((StompConnections<T>) connections).subscribe(destination, connectionId, subscriptionId)) {
            //if it enter the if, the subscribe returned false - already subscribe
            // Send ERROR frame for duplicate subscription
            HashMap<String, String> errorHeaders = new HashMap<>();
            errorHeaders.put("message", "Client is already subscribed to " + destination + " with subscription ID " + subscriptionId);
            if(msg.getHeaders().get("receipt") != null){
                //if the frame from the client include receipt, include it in the ERROR headers
                errorHeaders.put("receipt", msg.getHeaders().get("receipt")); 
            }       
            Message errorFrame = new Message("ERROR", errorHeaders, null);
            connections.send(connectionId, (T) errorFrame.toString());
            return; // No need to disconnect, just inform the client
        }
//if didnt enter the last if, subscribe returned true - calid SUBSCRIBE frame, connections.subscribed handled the subscription



    }

    public void handleUnsubscribe(Message msg) {
        System.out.println("proccessing UNSUBSCRIBE ");
    // Extract headers
        String subscriptionIdStr = msg.getHeaders().get("id");

        // Validate the frame headers
        if (subscriptionIdStr == null) {
            // Send ERROR frame for missing 'id' header
            HashMap<String, String> errorHeaders = new HashMap<>();
            errorHeaders.put("message", "Missing 'id' header in UNSUBSCRIBE frame");
            if(msg.getHeaders().get("receipt") != null){
                //if the frame from the client include receipt, include it in the ERROR headers
                errorHeaders.put("receipt", msg.getHeaders().get("receipt")); 
            }
            Message errorFrame = new Message("ERROR", errorHeaders, null);
            connections.send(connectionId, (T) errorFrame.toString());
            connections.disconnect(connectionId);
            shouldTerminate = true;
            return;
        }

        // Parse subscriptionId
        int subscriptionId;
        try {
            subscriptionId = Integer.parseInt(subscriptionIdStr);
        } catch (NumberFormatException e) {
            // Send ERROR frame for invalid subscription ID
            HashMap<String, String> errorHeaders = new HashMap<>();
            errorHeaders.put("message", "Invalid 'id' in UNSUBSCRIBE frame: " + subscriptionIdStr);
            if(msg.getHeaders().get("receipt") != null){
                //if the frame from the client include receipt, include it in the ERROR headers
                errorHeaders.put("receipt", msg.getHeaders().get("receipt")); 
            }
            Message errorFrame = new Message("ERROR", errorHeaders, null);
            connections.send(connectionId, (T) errorFrame.toString());
            connections.disconnect(connectionId);
            shouldTerminate = true;
            return;
        }

    //calling unsubscribe method in StompConnections, if unsubscribed is true - unsubscribed successfully, if false - wasnt able to unsubscribe
        boolean unsubscribed = ((StompConnections<T>) connections).unsubscribe(subscriptionId, connectionId);
        if (!unsubscribed) {
            // Send ERROR frame if the subscription does not exist
            HashMap<String, String> errorHeaders = new HashMap<>();
            errorHeaders.put("message", "No subscription found for ID " + subscriptionId);
            if(msg.getHeaders().get("receipt") != null){
                //if the frame from the client include receipt, include it in the ERROR headers
                errorHeaders.put("receipt", msg.getHeaders().get("receipt")); 
            }
            Message errorFrame = new Message("ERROR", errorHeaders, null);
            connections.send(connectionId, (T) errorFrame.toString());
            return; // No need to disconnect, just inform the client
        }

        // printing for debugging
        System.out.println("Client " + connectionId + " unsubscribed from subscription ID " + subscriptionId);
    }

    private Message parseMessage(String frame) {
        String[] parts = frame.split("\n", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid STOMP frame");
        }

        String command = parts[0].trim();
        String[] headersAndBody = parts[1].split("\n\n", 2);

        Map<String, String> headers = new HashMap<>();
        for (String headerLine : headersAndBody[0].split("\n")) {
            String[] headerParts = headerLine.split(":", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0].trim(), headerParts[1].trim());
            }
        }

        String body = headersAndBody.length > 1 ? headersAndBody[1] : null;

        return new Message(command, headers, body);
    }
}
