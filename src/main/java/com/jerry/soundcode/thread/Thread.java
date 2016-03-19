package com.jerry.soundcode.thread;

import java.security.AccessControlContext;

public class Thread implements Runnable {
	
	private static native void registerNatives();
	
	static {
		registerNatives();
	}
	
	private char name[];
	private int priority;
	private Thread threadQ;
	private long eetop;
	
	private boolean single_step;
	
	private boolean daemon = false;
	
	private boolean stillborn = false;
	
	private Runnable target;
	
	private ThreadGroup group;
	
	private ClassLoader contextClassLoader;
	
	private AccessControlContext inheritedAccessControlContext;
	
	private static int threadInitNumber;
	
	private static synchronized int nextThreadNum() {
		return threadInitNumber++;
	}
	
	@Override
	public void run() {
		
	}

	

}
