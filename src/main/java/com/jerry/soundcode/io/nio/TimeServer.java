package com.jerry.soundcode.io.nio;

public class TimeServer {
	
	int port = 8888;
	
	public void startServer() {
		MultiplexerTimeServer server = new MultiplexerTimeServer(port);
		new Thread(server, "NIO-MultiplexerTimeServer-001").start();
	}
	
}
