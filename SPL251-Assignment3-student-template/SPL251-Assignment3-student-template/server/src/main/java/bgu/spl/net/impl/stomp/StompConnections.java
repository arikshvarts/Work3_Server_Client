package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.util.Set;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

public class StompConnections<T> implements Connections<T> {
    // Maps connection IDs to connection handlers
    private final ConcurrentMap<Integer, ConnectionHandler<T>> clients = new ConcurrentHashMap<>();
    // Maps destinations (topics/queues) to lists of subscribed connection IDs
    private final ConcurrentMap<String, CopyOnWriteArrayList<Integer>> subscriptions = new ConcurrentHashMap<>();
        private static final AtomicInteger messageIDCounter = new AtomicInteger(0);
        //subscriptionsId key - connectionId and value - hashmap that maps each topic to its subscriptionid of this client
        private final ConcurrentMap<Integer, ConcurrentMap<String, Integer>> subscriptionsId = new ConcurrentHashMap<>();
        //we chhose the value be map of topic -> subid and not subid -> topic for efficiency in check double subscribe but can be upside down
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
        ConcurrentMap<String,Integer> clientSubscriptions = subscriptionsId.computeIfAbsent(connectionId, key -> new ConcurrentHashMap<>());
        if (clientSubscriptions.containsKey(destination)) {
        System.out.println("Client " + connectionId + " is already subscribed to destination " + destination + " with subscriptionId " + subscriptionId);
        return false; // Already subscribed
        }
        // Subscribes a client to a destination
        subscriptions.computeIfAbsent(destination, key -> new CopyOnWriteArrayList<>()).add(connectionId);
        subscriptionsId.computeIfAbsent(connectionId, key -> new ConcurrentHashMap<>()).put(destination, subscriptionId);
        return true;
    }


    public boolean unsubscribe(String destination, int connectionId) {
        // Unsubscribes a client from a destination
        CopyOnWriteArrayList<Integer> subscribers = subscriptions.get(destination);
        if (subscribers != null) {
            subscribers.remove(Integer.valueOf(connectionId));
            if (subscribers.isEmpty()) {
                subscriptions.remove(destination);
            }
            return true;
        }
        return false;
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

    public ConcurrentMap<Integer, ConcurrentMap<String, Integer>> get_subscriptionsId(){
        return subscriptionsId;
    }
}