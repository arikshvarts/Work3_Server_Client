package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.ConnectionHandler;

import java.io.IOException;

public class StompConnectionHandlerImpl<T> implements ConnectionHandler<T> {

	@Override
	public void send(T msg) {
		// Implementation of send method
	}

	@Override
	public void close() throws IOException {
		// Implementation of close method
	}
}
