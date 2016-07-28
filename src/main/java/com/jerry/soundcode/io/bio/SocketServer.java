package com.jerry.soundcode.io.bio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 同步阻塞IO
 * 每当有一个新的客户端请求接入时，服务端必须创建一个新的线程处理新接入的客户端链路。
 * 一个线程只能处理一个客户端连接。
 */
public class SocketServer {
	
	int port = 8888;
	ServerSocket server = null;	// 负责绑定ip端口，启动监听端口
	Socket socket = null;	// 负责发起连接操作
	
	public void startServer() {
		try {
			server = new ServerSocket(port);
			while(true) {
				socket = server.accept();	// 阻塞
				new Thread (new SocketServerHandler(socket)).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		SocketServer server = new SocketServer();
		server.startServer();
	}
	
}
