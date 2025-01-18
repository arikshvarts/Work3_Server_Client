package bgu.spl.net.impl.stomp;
import java.util.HashMap;
import java.util.Map;

import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.impl.stomp.Message;;


public class StompProtocol<T> implements MessagingProtocol<T> {
    private boolean shouldTerminate = false;
    private Connections<T> connections;
    private int connectionId;
     
    
    public void start(int connectionId, Connections<T> connections){
        this.connectionId = connectionId;
        this.connections = connections;
    }
    
    public T process(T msg){
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
        return null;

    }

    @Override
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
