package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class StompConnections<T> implements Connections<T> {

	@Override
	public void disconnect(int connectionId) {
		// Implementation here
	}

	@Override
	public void send(String channel, T msg) {
		// Implementation here
	}

	@Override
	public boolean send(int connectionId, T msg) {
return false;
        // Implementation here
    }
}