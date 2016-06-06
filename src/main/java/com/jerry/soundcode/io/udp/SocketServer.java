package com.jerry.soundcode.io.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class SocketServer {
	
	int port = 8888;
	
	public void openServer() {
		
		try {
			DatagramSocket socket = new DatagramSocket(port);
			
			byte[] buf = new byte[100];
			DatagramPacket packetReceive = new DatagramPacket(buf, 0, buf.length);
			socket.receive(packetReceive);
			
			String str = new String(buf, 0, packetReceive.getLength());
			System.out.println(str);
			
			// 获取端口号
			int port = packetReceive.getPort();
			// 获取IP地址
			InetAddress addr = packetReceive.getAddress();
			// 向客户端传输数据
			byte[] date = "this is server".getBytes();
			
			DatagramPacket packetSend = new DatagramPacket(date, 0, date.length, addr, port);
			socket.send(packetSend);
			
			socket.close();
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		SocketServer server = new SocketServer();
		server.openServer();
	}
}
