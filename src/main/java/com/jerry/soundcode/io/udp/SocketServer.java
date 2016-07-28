package com.jerry.soundcode.io.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class SocketServer {
	
	int port = 8888;
	
	DatagramSocket socket = null;
	
	public void openServer() {
		
		try {
			socket = new DatagramSocket(port);
			
			byte[] buf = new byte[100];
			DatagramPacket packetReceive = new DatagramPacket(buf, buf.length);
			socket.receive(packetReceive);
			
			String str = new String(packetReceive.getData(), 0, packetReceive.getLength());
			System.out.println("Accept client data :" + str);
			
			// 获取端口号
			int port = packetReceive.getPort();
			// 获取IP地址
			InetAddress addr = packetReceive.getAddress();
			// 向客户端传输数据
			byte[] date = (str + " from server").getBytes();
			
			DatagramPacket packetSend = new DatagramPacket(date, date.length, addr, port);
			socket.send(packetSend);
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			socket.close();
		}
	}
	
	public static void main(String[] args) {
		SocketServer server = new SocketServer();
		server.openServer();
	}
}
