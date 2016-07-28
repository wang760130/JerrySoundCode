package com.jerry.soundcode.io.nio;

public class SocketServer {
	
	int port = 8888;
	
	public void startServer() {
		MultiplexerSocketServer server = new MultiplexerSocketServer(port);
		new Thread(server).start();
	}
	
	public static void main(String[] args) {
		SocketServer server = new SocketServer();
		server.startServer();
	}
	
}
