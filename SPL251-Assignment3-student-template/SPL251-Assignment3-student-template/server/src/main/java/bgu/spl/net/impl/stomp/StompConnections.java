package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.util.Set;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

public class StompConnections<T> implements Connections<T> {
    	// Maps connection IDs to connection handlers
    	private final ConcurrentHashMap<Integer, ConnectionHandler<T>> clients;
    	// Maps destinations (topics/queues) to lists of subscribed connection IDs
    	private final ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>> subscriptions;
        private static AtomicInteger messageIDCounter	=new AtomicInteger(0);
        //subscriptionsId key - connectionId and value - hashmap that maps each topic to its subscriptionid of this client
        private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> clientsTo_subscriptionsId;
        //we chhose the value be map of topic -> subid and not subid -> topic for efficiency in check double subscribe but can be upside down
		// hashmap of login to passcode of all users(logged in users and logged out users)
        private final ConcurrentHashMap<String, String> credentials;
        //LinkedQueue of the users currently logged in
        private ConcurrentLinkedQueue<String> activeUsers;

        StompConnections() {
			clients = new ConcurrentHashMap<>();
			subscriptions = new ConcurrentHashMap<>();
			clientsTo_subscriptionsId = new ConcurrentHashMap<>();
            credentials = new ConcurrentHashMap<>();
            activeUsers = new ConcurrentLinkedQueue<>();
	}

    @Override
    public boolean send(int connectionId, T message) {
        // Sends a message to a specific client
        ConnectionHandler<T> handler = clients.get(connectionId);
        if (handler != null) {
            handler.send(message);
			return true;
        } else {
            System.err.println("Connection ID " + connectionId + " not found.");
			return false;
        }
    }

    @Override
    public void send(String destination, T message) {
        // Sends a message to all clients subscribed to a specific destination
        CopyOnWriteArrayList<Integer> subscribers = subscriptions.get(destination);
        if (subscribers != null) {
            for (Integer connectionId : subscribers) {
                send(connectionId, message);
            }
        }
    }

    @Override
    public void disconnect(int connectionId) {
        // Disconnect a client and remove it from subscriptions
        ConnectionHandler<T> handler = clients.remove(connectionId);
        if (handler != null) {
            try {
                handler.close();
            } catch (IOException e) {
                System.err.println("Failed to close connection handler for connection ID " + connectionId + ": " + e.getMessage());
            }
        }
        for (CopyOnWriteArrayList<Integer> subscribers : subscriptions.values()) {
            subscribers.remove(Integer.valueOf(connectionId));
        }
    }

    public boolean subscribe(String destination, int connectionId, int subscriptionId) {
        ConcurrentMap<String,Integer> clientSubscriptions = clientsTo_subscriptionsId.computeIfAbsent(connectionId, key -> new ConcurrentHashMap<>());
        if (clientSubscriptions.containsKey(destination)) {
        System.out.println("Client " + connectionId + " is already subscribed to destination " + destination + " with subscriptionId " + subscriptionId);
        return false; // Already subscribed
        }
        // Subscribes a client to a destination
        subscriptions.computeIfAbsent(destination, key -> new CopyOnWriteArrayList<>()).add(connectionId);
        clientsTo_subscriptionsId.computeIfAbsent(connectionId, key -> new ConcurrentHashMap<>()).put(destination, subscriptionId);
        return true;
    }


    // public boolean unsubscribe(String destination, int connectionId) {
    //     // Unsubscribes a client from a destination
    //     CopyOnWriteArrayList<Integer> subscribers = subscriptions.get(destination);
    //     if (subscribers != null) {
    //         subscribers.remove(Integer.valueOf(connectionId));
    //         if (subscribers.isEmpty()) {
    //             subscriptions.remove(destination);
    //         }
    //         return true;
    //     }
    //     return false;
    // }
    public boolean unsubscribe(int subscriptionId, int connectionId) {
        ConcurrentMap<String, Integer> clientSubscriptions = clientsTo_subscriptionsId.get(connectionId);
        if (clientSubscriptions != null) {

            if (clientSubscriptions.values().contains(subscriptionId) == true) {
                //if this subscription id exists for that client
                String destination = null;
                for(String dest : clientSubscriptions.keySet()){
                    if(clientSubscriptions.get(dest) == subscriptionId)
                    {
                        destination = dest;
                    }
                }
                CopyOnWriteArrayList<Integer> subscribers = subscriptions.get(destination);
                if (subscribers != null) {
                    subscribers.remove(Integer.valueOf(connectionId));
                    if (subscribers.isEmpty()) {
                        subscriptions.remove(destination);
                    }
                }
                return true; // Successfully unsubscribed
            }
            return false; // Subscription id doesnt exist for that client
        }
        return false; // client is not subscribed to any topic
    }
    

    /**
     * Registers a new connection.
     * 
     * @param connectionId The unique ID of the connection.
     * @param handler      The handler associated with the connection.
     */
    public void registerConnection(int connectionId, ConnectionHandler<T> handler) {
        clients.put(connectionId, handler);
    }

    public boolean isSubscribed(int connectionId, String channel) {
        CopyOnWriteArrayList<Integer> subscribers = subscriptions.get(channel);
        if(subscribers == null){return false;}
        else{
            return subscribers.contains(connectionId);
        }
    }

    public String generateMessageId() {
        //increase by one our atomic counter
        return String.valueOf(messageIDCounter.incrementAndGet());
    }

    public ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> get_clientsTo_subscriptionsId(){
        return clientsTo_subscriptionsId;
    }
	public ConcurrentMap<Integer, ConnectionHandler<T>> getClients(){
		return clients;
	}
    public boolean handle_credentials(String login, String passcode){
        if(credentials.get(login) != null){
            if(credentials.get(login).equals(passcode)){return true;}
            else{return false;} //passcode doesn't match the login
        }
        // this is a new user
        credentials.put(login, passcode);
        return true;
    }
}