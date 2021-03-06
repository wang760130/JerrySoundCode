package com.jerry.soundcode.io.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer {
	
	int port = 8888;
	
	ServerSocket serverSocket = null;
	Socket socket = null;
	InputStream in = null;
	InputStreamReader reader = null;
	BufferedReader buffer = null;
	OutputStream os = null;
	PrintWriter pw = null;
	
	public void startServer() {
		try {
			// 创建ServerSocket对象（并绑定端口）
			serverSocket = new ServerSocket(port);
			
			// 调用accept方法，接收客户端的数据
			// 对于accept方法的调用将造成阻塞，直到ServerSocket接受到一个连接请求为止。
			// 一旦连接请求被接受，服务器可以读客户socket中的请求。
			while(true) {
				socket = serverSocket.accept();
				
				in = socket.getInputStream();
				reader = new InputStreamReader(in);
				buffer = new BufferedReader(reader);
				
				// 输出客户端传过来的数据
				String line = buffer.readLine();
				System.out.println("Accept client data : " + line);
				
				// 给客户端传输数据
				os = socket.getOutputStream();
				pw = new PrintWriter(socket.getOutputStream());
				pw.println(line + " from server");
				pw.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				pw.close();
				os.close();
				buffer.close();
				reader.close();
				in.close();
				socket.close();
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		SocketServer server = new SocketServer();
		server.startServer();
	}
}
