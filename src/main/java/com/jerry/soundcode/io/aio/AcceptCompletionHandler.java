package com.jerry.soundcode.io.aio;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, AsyncSocketServerHandler> {

	@Override
	public void completed(AsynchronousSocketChannel result, AsyncSocketServerHandler attachment) {
		attachment.asynchronousServerSocketChannel.accept(attachment, this);
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		result.read(buffer, buffer, new ReadCompletionHandler(result));
	}

	@Override
	public void failed(Throwable exc, AsyncSocketServerHandler attachment) {
		exc.printStackTrace();
		attachment.countDownLatch.countDown();
	}

}
