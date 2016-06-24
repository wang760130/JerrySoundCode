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
	
	public ThreadLocal.ThreadLocalMap threadLocals = null;
	
	public ThreadLocal.ThreadLocalMap inheritableThreadLocal = null;
	
	private long stackSize;
	
	private long nativeParkEventPointer;
	
	private long tid;
	
	private static long threadSeqNumber;
	
	private int threadStatus = 0;
	
	private static synchronized long nextThreadID() {
		return ++threadSeqNumber;
	}
	
	volatile Object parkBlocker;
	
	private volatile Interruptible blocker;
	
	private Object blockerLock = new Object();
	
	void blockedOn(Interruptible b) {
		synchronized (blockerLock) {
			blocker = b;
		}
	}
	
	public final static int MIN_PRIORITY = 1;
	
	public final static int MORM_PRIORITY = 5;
	
	public final static int MAX_PRIORITY = 10;
	
	private boolean stopBeforeStart;
	
	private Throwable throwableFromStop;
	
	private volatile Thread me;
	
//	public static native Thread currentThread();
	
	public static native void yield();
	
	public static native void sleep(long millis) throws InterruptedException;
	
	@Override
	public void run() {
		
	}


	public static Thread currentThread() {
		// TODO Auto-generated method stub
		return null;
	}

	public static boolean interrupted() {
		// TODO Auto-generated method stub
		return false;
	}

	public void interrupt() {
		// TODO Auto-generated method stub
		
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public long getId() {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean isInterrupted() {
		// TODO Auto-generated method stub
		return false;
	}

	

}
