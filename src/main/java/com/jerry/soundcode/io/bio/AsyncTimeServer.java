package com.jerry.soundcode.io.bio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 伪异步IO
 * 采用线程池和任务队列实现
 */
public class AsyncTimeServer {
	
	int port = 8888;
	
	TimeServerHandlerExceutePool executor = new TimeServerHandlerExceutePool(50, 10000);
	
	ServerSocket server = null;
	Socket socket = null;
	
	public void startServer() {
		
		try {
			server = new ServerSocket(port);
			while(true) {
				socket = server.accept();
				executor.execute(new TimeServerHandler(socket));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
				server.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		AsyncTimeServer server = new AsyncTimeServer();
		server.startServer();
	}
	
}
