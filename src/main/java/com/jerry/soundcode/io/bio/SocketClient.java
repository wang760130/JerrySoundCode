package com.jerry.soundcode.io.bio;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient {
	
	int port = 8888;
	String host  = "localhost";
	
	public void sendServer() {
		
		Socket socket = null;
		InputStream is = null;
		InputStreamReader reader = null;
		BufferedReader buffer = null;
		OutputStream os = null;
		PrintWriter pw = null;
		try {
			
			// 创建Socket对象（并连接服务器）
			socket = new Socket(host, port);
			
			// 调用getOutputStream方法，进行I/O
			os = socket.getOutputStream();
			pw = new PrintWriter(os, true);
			
			pw.println("This is client message");
			pw.flush();
			
			// 接收服务器返回的数据
			is = socket.getInputStream();
			reader = new InputStreamReader(is);
			buffer = new BufferedReader(reader);
		
			String line = buffer.readLine();
			System.out.println("Server return message : " + line);

		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				pw.close();
				os.close();
				buffer.close();
				reader.close();
				is.close();
				socket.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		 
	}
	
	public static void main(String[] args) {
		SocketClient client = new SocketClient();
		client.sendServer();
	}
	
}
