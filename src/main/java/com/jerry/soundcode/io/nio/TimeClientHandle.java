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
				if(socketChannel.finishConnect()) {
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
		if(socketChannel.connect(new InetSocketAddress(host, port))) {
			socketChannel.register(selector, SelectionKey.OP_READ);
			this.doWrite(socketChannel);
		} else {
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
		if(buffer.hasRemaining()) {
			System.out.println("Send order 2 server succeed");
		}
	}

}
