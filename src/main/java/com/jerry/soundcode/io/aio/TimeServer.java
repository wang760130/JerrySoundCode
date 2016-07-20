package com.jerry.soundcode.io.aio;

public class TimeServer {

	static int port = 8888;
	
	public static void main(String[] args) {
		AsyncTimeServerHandler asyncTimeServerHandler = new AsyncTimeServerHandler(port);
		new Thread(asyncTimeServerHandler, "AIO-AsyncTimeServerHandler-001").start();
	}

}
