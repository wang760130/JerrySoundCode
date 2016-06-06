package com.jerry.soundcode.io.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SocketClient {
	
	String host = "127.0.0.1";
	int port = 8888;
	
	public void sendServer() {
		try {
			DatagramSocket socket = new DatagramSocket();
			
			String str = "Hello! I am client";
			byte[] data = str.getBytes();
			
			InetAddress addr = InetAddress.getByName(host);
			DatagramPacket packetSend = new DatagramPacket(data, 0, data.length, addr, port);
			socket.send(packetSend);
			
			// 接收服务器端发送的数据
			byte[] buf = new byte[100];
			DatagramPacket packetReceive = new DatagramPacket(buf, 0, buf.length);
			socket.receive(packetSend);
			String msg = new String(buf, 0, packetReceive.getLength());
			System.out.println("receive :" + msg);
			
			socket.close();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		SocketClient client = new SocketClient();
		client.sendServer();
	}
	
}
