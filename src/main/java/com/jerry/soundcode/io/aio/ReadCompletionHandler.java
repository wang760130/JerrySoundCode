package com.jerry.soundcode.io.aio;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Date;

public class ReadCompletionHandler implements CompletionHandler<Integer, ByteBuffer>{

	private AsynchronousSocketChannel channel;
	
	public ReadCompletionHandler(AsynchronousSocketChannel channel) {
		if(this.channel == null) {
			this.channel = channel;
		}
	}
	

	@Override
	public void completed(Integer result, ByteBuffer attachment) {
		attachment.flip();
		byte[] body = new byte[attachment.remaining()];
		attachment.get(body);
		
		try {
			String request = new String(body, "UTF-8");
			System.out.println("receiveve :" + request);
			String currentTime = new Date(System.currentTimeMillis()).toString();
			doWrite(currentTime);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
	}

	private void doWrite(String currentTime) {
		if(currentTime != null && currentTime.trim().length() > 0) {
			byte[] bytes = currentTime.getBytes();
			ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
			buffer.put(bytes);
			buffer.flip();
			
		}
		
	}

	@Override
	public void failed(Throwable exc, ByteBuffer attachment) {
		// TODO Auto-generated method stub
		
	}

}
