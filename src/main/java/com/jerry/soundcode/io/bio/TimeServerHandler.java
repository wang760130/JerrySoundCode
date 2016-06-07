package com.jerry.soundcode.io.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

public class TimeServerHandler implements Runnable {

	private Socket socket;
	
	public TimeServerHandler(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		InputStream is = null;
		InputStreamReader reader = null;
		BufferedReader in = null;
		OutputStream os = null;
		PrintWriter pw = null;
		
		try {
			is = this.socket.getInputStream();
			reader = new InputStreamReader(is);
			in = new BufferedReader(reader);
			os = this.socket.getOutputStream();
			pw = new PrintWriter(os, true);
			
			String body = null;
			String currentTime = null;
			while(true) {
				body = in.readLine();
				if(body == null) {
					break;
				}
				System.out.println("The time server receive order : " + body);
				currentTime = "time".equals(body) ? new Date(System.currentTimeMillis()).toString() : "bad order";
				pw.println(currentTime);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				pw.close();
				os.close();
				in.close();
				reader.close();
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

}
