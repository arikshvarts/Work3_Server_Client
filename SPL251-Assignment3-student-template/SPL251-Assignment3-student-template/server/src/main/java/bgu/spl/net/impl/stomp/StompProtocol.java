package bgu.spl.net.impl.stomp;
import java.util.HashMap;
import java.util.Map;

import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.srv.Connections;

public class StompProtocol {
    private boolean shouldTerminate = false;
    private Connections<Message> connections;
    private int connectionId;
    
    public void start(int connectionId, Connections<Message> connections){
        this.connectionId = connectionId;
        this.connections = connections;
    }
    
    
    public Message process(Message msg){
        switch (msg.getCommand()) {
            case "CONNECT":
                handleConnect(msg);
                break;

            case "SEND":
                handleSend(msg);
                break;

            case "SUBSCRIBE":
                handleSubscribe(msg);
                break;

            case "UNSUBSCRIBE":
                handleUnsubscribe(msg);
                break;

            case "DISCONNECT":
                handleDisconnect(msg);
                break;

            default:
                // sendError("Unknown command: " + msg.getCommand());
        }
        return null;

    }

    
    public boolean shouldTerminate(){
        return shouldTerminate;
    }


//handlers method to each specific frame type from client
    public void handleConnect(Message msg){
        //server responds by sendin a CONNECTED Message to registered Clients
        HashMap<String, String> headers = new HashMap();
        headers.put("header1", "Version : 1.2"); //should version be generic?
        Message Connected_Frame = new Message("CONNECTED", null, "~@");
        
    }

    public void handleDisconnect(Message msg){
        
    }

    public void handleSend(Message msg){
        
    }

    public void handleSubscribe(Message msg){
        
    }

    public void handleUnsubscribe(Message msg){
        
    }
}
