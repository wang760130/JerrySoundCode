package com.jerry.soundcode.io.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SocketClient {
	
	String host = "localhost";
	int port = 8888;
	
	DatagramSocket socket = null;
	
	public void sendServer() {
		try {
			socket = new DatagramSocket();
			
			String str = "This is client message";
			byte[] data = str.getBytes();
			
			InetAddress addr = InetAddress.getByName(host);
			DatagramPacket packetSend = new DatagramPacket(data, 0, data.length, addr, port);
			socket.send(packetSend);
			
			// 接收服务器端发送的数据
			byte[] buf = new byte[100];
			DatagramPacket packetReceive = new DatagramPacket(buf, 0, buf.length);
			socket.receive(packetReceive);
			String msg = new String(buf, 0, packetReceive.getLength());
			System.out.println("Server return message : " + msg);
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			socket.close();
		}
	}
	
	public static void main(String[] args) {
		SocketClient client = new SocketClient();
		client.sendServer();
	}
	
}
