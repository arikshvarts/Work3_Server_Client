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
        Message frame = (Message)msg;
        switch (frame.getCommand()) {
            case "CONNECT":
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

    }

    @Override
    public boolean shouldTerminate(){
        return shouldTerminate;
    }


//handlers method to each specific frame type from client
    public void handleConnect(Message msg){
        //server responds by sending a CONNECTED Message to registered Clients
        String clientVersion = msg.getHeaders().get("accept-version");
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
        connections.disconnect(connectionId);
            shouldTerminate = true;
            System.out.println("Client disconnected: ID=" + connectionId);
    }

    public void handleSend(Message msg){
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

            connections.send(connectionId, (T) errorFrame);
            connections.disconnect(connectionId); // Close the connection
            shouldTerminate = true;
        }
        else{
        String message = msg.getBody();
        HashMap<String, String> messageHeaders = new HashMap<>();
        messageHeaders.put("subscription : ",((StompConnections<T>)connections).get_subscriptionsId().get(topic).toString());
        messageHeaders.put("message-id: ", ((StompConnections<T>)connections).generateMessageId());
        Message messageFrame = new Message("Message", messageHeaders, msg.getBody());
        //sending the message the client had to the channel
        ((StompConnections<T>) connections).send(topic, (T)messageFrame);
        }
    }

    public void handleSubscribe(Message msg){
        
    }

    public void handleUnsubscribe(Message msg){
        
    }
}
