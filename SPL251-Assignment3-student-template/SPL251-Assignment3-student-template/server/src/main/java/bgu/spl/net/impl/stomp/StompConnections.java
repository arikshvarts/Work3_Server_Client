package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.util.Set;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class StompConnections<T> implements Connections<T> {
    // Maps connection IDs to connection handlers
    private final ConcurrentMap<Integer, ConnectionHandler<T>> clients = new ConcurrentHashMap<>();
    // Maps destinations (topics/queues) to lists of subscribed connection IDs
    private final ConcurrentMap<String, CopyOnWriteArrayList<Integer>> subscriptions = new ConcurrentHashMap<>();

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

    public boolean subscribe(String destination, int connectionId) {
        // Subscribes a client to a destination
        subscriptions.computeIfAbsent(destination, key -> new CopyOnWriteArrayList<>()).add(connectionId);
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
}