package com.jerry.soundcode.io.aio;

public class SocketClient {
	
	String host = "localhost";
	int port = 8080;
	
	public void sendServer() {
	    new Thread(new AsyncSocketClientHandler(host, port), "AIOClient").start();
	}
	
	public static void main(String[] args) {
		SocketClient client = new SocketClient();
		client.sendServer();
	}

}
