package com.jerry.soundcode.thread;

public interface ThreadFactory {

	Thread newThread(Runnable r);
	
}
