package com.jerry.soundcode.io.aio;

public class SocketServer {

	static int port = 8888;
	
	public void startServer() {
		AsyncSocketServerHandler asyncTimeServerHandler = new AsyncSocketServerHandler(port);
		new Thread(asyncTimeServerHandler).start();
	}
	
	public static void main(String[] args) {
		SocketServer server = new SocketServer();
		server.startServer();
	}

}
