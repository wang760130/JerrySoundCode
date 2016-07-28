package com.jerry.soundcode.io.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;

import com.jerry.soundcode.thread.CountDownLatch;

public class AsyncSocketServerHandler implements Runnable {
	
	CountDownLatch countDownLatch;
	AsynchronousServerSocketChannel asynchronousServerSocketChannel;
	
	public AsyncSocketServerHandler(int port) {
		try {
			asynchronousServerSocketChannel = AsynchronousServerSocketChannel.open();
			asynchronousServerSocketChannel.bind(new InetSocketAddress(port));
			System.out.println("Server start : " + port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		countDownLatch = new CountDownLatch(1);
		doAccept();
		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void doAccept() {
		asynchronousServerSocketChannel.accept(this, new AcceptCompletionHandler());
	}

}
