package com.jerry.soundcode.io.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketClient {
	
	String host = "localhost";
	int port = 8888;
	
	Socket socket = null;
	OutputStream os = null;
	PrintWriter pw = null;
	InputStream is = null;
	InputStreamReader reader = null;
	BufferedReader buffer = null;
	
	public void sendServer() {
		
		try {
			// 创建Socket对象（并连接服务器）
			socket = new Socket(host, port);
			
			// 调用getOutputStream方法，进行I/O
			os = socket.getOutputStream();
			pw = new PrintWriter(os);
			
			// 向服务器发送消息
			pw.println("Hello World, this is client message");
			pw.flush();
			
			// 接收服务器返回的数据
			is = socket.getInputStream();
			reader = new InputStreamReader(is);
			buffer = new BufferedReader(reader);
			
			String line = buffer.readLine();
			System.out.println("Server return message : " + line);
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
				reader.close();
				buffer.close();
				os.close();
				pw.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public static void main(String[] args) {
		SocketClient client  = new SocketClient();
		client.sendServer();
	}
	
}	
