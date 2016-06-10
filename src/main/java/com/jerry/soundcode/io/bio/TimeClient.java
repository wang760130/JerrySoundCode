package com.jerry.soundcode.io.bio;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class TimeClient {
	
	int port = 8888;
	String host  = "localhost";
	
	public void sendServer() {
		
		Socket socket = null;
		InputStream is = null;
		InputStreamReader reader = null;
		BufferedReader br = null;
		OutputStream os = null;
		PrintWriter pw = null;
		try {
			
			socket = new Socket(host, port);
			is = socket.getInputStream();
			reader = new InputStreamReader(is);
			br = new BufferedReader(reader);
		
			os = socket.getOutputStream();
			pw = new PrintWriter(os, true);
			pw.println("time");
			String line = br.readLine();
			System.out.println("Now is " + line);
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				pw.close();
				os.close();
				br.close();
				reader.close();
				is.close();
				socket.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		 
	}
	
	public static void main(String[] args) {
		TimeClient client = new TimeClient();
		client.sendServer();
	}
	
}
