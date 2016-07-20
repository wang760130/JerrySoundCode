package com.jerry.soundcode.io.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class TimeClientHandle implements Runnable {

	private String host;
	private int port;
	
	private Selector selector;
	private SocketChannel socketChannel;
	private volatile boolean stop;
	
	public TimeClientHandle(String host, int port) {
		this.host = host;
		this.port = port;
		
		try {
			selector = Selector.open();
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		
		try {
			doConnect();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		while(!stop) {
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
					} catch(Exception e) {
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
				System.exit(1);
			}
		}
		
		if(selector != null) {
			// 多路复用器关闭后，所有注册在上面的长乐路和pipe等资源都会被自动去	注册并关闭，所以不需要重复释放资源
			try {
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
	private void handleInput(SelectionKey key) throws ClosedChannelException, IOException {
		if(key.isValid()) {
			// 判断是否连接成功
			SocketChannel socketChannel = (SocketChannel) key.channel();
			if(key.isConnectable()) {
				// 连接状态，服务端已经返回ACK应答消息
				if(socketChannel.finishConnect()) {
					// 客户端连接成功
					
					// 将SocketChannel注册到多路复用器上
					socketChannel.register(selector, SelectionKey.OP_READ);
					doWrite(socketChannel);
				} else {
					// 连接失败，进程退出
					System.exit(1);
				}
			}
			
			if(key.isReadable()) {
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes = socketChannel.read(readBuffer);
				if(readBytes > 0) {
					readBuffer.flip();
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String body = new String(bytes, "UTF-8");
					System.out.println("Now is :" + body);
					this.stop = true;
				} else if(readBytes < 0) {
					// 对端链路关闭
					key.cancel();
					socketChannel.close();
				} else {
					// 读到0字节，不管他
				}
			}
		}
	}
	
	private void doConnect() throws IOException {
		// 异步连接服务器
		boolean connected = socketChannel.connect(new InetSocketAddress(host, port));
		if(connected) {
			// 注册读状态位到多路复用器中
			socketChannel.register(selector, SelectionKey.OP_READ);
			this.doWrite(socketChannel);
		} else {
			// 服务器没有返回TCP握手应答消息，向Reactor线程的多路复用器注册OP_CONNECT状态位，监听服务端的TCP ACK应答
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
		}
	}
	
	private void doWrite(SocketChannel socketChannel) throws IOException {
		String order = "query time order";
		byte[] bytes = order.getBytes();
		ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
		buffer.put(bytes);
		buffer.flip();
		socketChannel.write(buffer);
		
		// 对发送结果进行判断
		if(buffer.hasRemaining()) {
			// 缓冲区中的消息全部发送完成
			System.out.println("Send order 2 server succeed");
		}
	}

}
