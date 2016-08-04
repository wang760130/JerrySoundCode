package com.jerry.soundcode.io.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 多路复用类，独立的线程，负责轮询多路复用器Selctor，可以处理多个客户达un的并发接入
 */
public class MultiplexerSocketServer implements Runnable {
	
	private Selector selector;
	private ServerSocketChannel serverChannel;
	
	volatile boolean stop;
	
	/**
	 * 初始化多路复用器、绑定监听端口
	 * @param port
	 */
	public MultiplexerSocketServer(int port) {
		try {
			// 创建Reactor线程
			selector = Selector.open();
						
			// 用户监听客户端的连接，是所有客户端连接的父管道
			serverChannel = ServerSocketChannel.open();
			
			// 绑定监听端口
			serverChannel.socket().bind(new InetSocketAddress(port), 1024);
			
			// 设置为非阻塞模式
			serverChannel.configureBlocking(false);
			
			// 将ServerSocketChannel注册到Reactor线程的多路复用器Seector上，监听ACCEPT事件
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			System.out.println("Server start : " + port);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	@Override
	public void run() {
		while(!stop) {
			System.out.println("run....");
			try {
				// 多路复用器在线程run方法的无限循环体内轮询准备就绪的Key
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
	
	private void handleInput(SelectionKey key) throws IOException {
		if(key.isValid()) {
			// 处理新接入的请求消息
			if(key.isAcceptable()) {
				// Accept the new connection
				ServerSocketChannel serverChannnel = (ServerSocketChannel) key.channel();
				// 多路复用器监听到有新的客户端接入，处理新的接入请求，完成TCP三次握手，建立物理链路
				SocketChannel socketChannel = serverChannnel.accept();
				// 设置客户端链路为非阻塞模式
				socketChannel.configureBlocking(false);
				// 将新的接入的客户端连接注册到Reactor线程的多路复用器上，监听读操作，用来读取客户端发送的网络信息
				socketChannel.register(selector, SelectionKey.OP_READ);
			}
			
			if(key.isReadable()) {
				// Read the data
				SocketChannel socketChannel = (SocketChannel) key.channel();
				ByteBuffer buffer = ByteBuffer.allocate(1024);
				// 异步读取客户端请求信息到缓冲区
				int readBytes = socketChannel.read(buffer);
				if(readBytes > 0) {
					// 返回值大于0，读到了字节，对字节进行编解码
					buffer.flip();
					byte[] bytes = new byte[buffer.remaining()];
					buffer.get(bytes);
					String body = new String(bytes, "UTF-8");
					System.out.println("Accept client data : " + body);
					this.doWrite(socketChannel, body + " from server");
				} else if(readBytes < 0) {
					// 链路已经关闭，需要关闭SocketChannel，释放资源
					key.cancel();
					socketChannel.close();
				}  else if(readBytes == 0) {
					// 没有读取到字节，属于正常场景
				}
			}
		}
	}
	
	private void doWrite(SocketChannel channel, String response) throws IOException {
		if(response != null && response.trim().length() > 0) {
			byte[] bytes = response.getBytes();
			ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
			buffer.put(bytes);
			// 将缓存区当前的limit设置为position，position设置为0，用于后续对缓存区的读取操作。	
			buffer.flip();
			// 将消息异步发送给客户端
			channel.write(buffer);
		}
	}
	
	public void stop() {
		this.stop = true;
	}
}
