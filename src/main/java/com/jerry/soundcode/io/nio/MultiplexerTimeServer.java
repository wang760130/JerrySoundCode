package com.jerry.soundcode.io.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerTimeServer implements Runnable {
	
	Selector selector;
	ServerSocketChannel serverChannel;
	
	volatile boolean stop;
	
	/**
	 * 初始化多路复用器、绑定监听端口
	 * @param port
	 */
	public MultiplexerTimeServer(int port) {
		try {
			selector = Selector.open();
			serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			serverChannel.socket().bind(new InetSocketAddress(port), 1024);
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("The time server is start in port :" + port);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	@Override
	public void run() {
		while(!stop) {
			
			try {
				selector.select(1000);
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectedKeys.iterator();
				SelectionKey key = null;
				while(it.hasNext()) {
					key = it.next();
					it.remove();
					try {
						this.handleInput(key);
					} catch (Exception e) {
						if(key != null) {
							key.cancel();
							if(key.channel() != null) {
								key.channel().close();
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// 多路复用器关闭后，所有注册在上面的channel和pipe等资源都会被自动去注册并关闭，
			// 所以不需要重复释放资源。
			if(selector != null) {
				try {
					selector.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void handleInput(SelectionKey key) throws IOException {
		if(key.isValid()) {
			// 处理新接入的请求消息
			if(key.isAcceptable()) {
				// Accept the new connection
				ServerSocketChannel serverChannnel = (ServerSocketChannel) key.channel();
				SocketChannel socketChannel = serverChannnel.accept();
				socketChannel.configureBlocking(false);
				socketChannel.register(selector, SelectionKey.OP_READ);
			}
			
			if(key.isReadable()) {
				// Read the data
				SocketChannel socketChannel = (SocketChannel) key.channel();
				ByteBuffer buffer = ByteBuffer.allocate(1024);
				int readBytes = socketChannel.read(buffer);
				if(readBytes > 0) {
					buffer.flip();
					byte[] bytes = new byte[buffer.remaining()];
					buffer.get(bytes);
					String body = new String(bytes, "UTF-8");
					System.out.println("The time server receive order : " + body);
					this.doWrite(socketChannel, new Date(System.currentTimeMillis()).toString());
				} else if(readBytes < 0) {
					key.cancel();
					socketChannel.close();
				} 
			}
		}
	}
	
	private void doWrite(SocketChannel channel, String response) throws IOException {
		if(response != null && response.trim().length() > 0) {
			byte[] bytes = response.getBytes();
			ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
			buffer.put(bytes);
			buffer.flip();
			channel.write(buffer);
		}
	}
	
	public void stop() {
		this.stop = true;
	}
}
