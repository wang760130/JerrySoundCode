package com.jerry.soundcode.io.nio;

public class SocketClient {

	String host = "localhost";
	int port = 8888;
	
	public void sendServer() {
		for(;;) {
			new Thread(new SocketClientHandle(host, port)).start();
		}
	}
	
	public static void main(String[] args) {
		SocketClient client = new SocketClient();
		client.sendServer();
	}
}
