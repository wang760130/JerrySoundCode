package com.jerry.soundcode.io.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketServerHandler implements Runnable {

	private Socket socket;
	
	public SocketServerHandler(Socket socket) {
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
			
			String line = null;
			while(true) {
				line = in.readLine();
				
				if(line == null) {
					break;
				}
				
				System.out.println("Accept client data : " + line);
				
				pw.println(line + " from server");
				pw.flush();
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
