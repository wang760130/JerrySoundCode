package com.jerry.soundcode.thread;

import java.security.AccessControlContext;
import java.security.AccessController;

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
	
	public ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;
	
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
	
	public static native Thread currentThread();
	
	public static native void yield();
	
	public static native void sleep(long millis) throws InterruptedException;
	
	public static void sleep(long millis, int nanos) throws InterruptedException {
		if(millis < 0) {
			throw new IllegalArgumentException("timeout value is negative");
		}
		
		if(nanos < 0 || nanos > 999999) {
			throw new IllegalArgumentException("nanosecond timeout value out of range");
		}
		
		if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
		    millis++;
		}

		sleep(millis);
	}
	
	private void init(ThreadGroup g, Runnable target, String name, long stackSize) {
		Thread parent = currentThread();
		SecurityManager security = System.getSecurityManager();
		
		if(g == null) {
			if(security != null) {
				g = security.getThreadGroup();
			}
			
			if(g == null) {
				g = parent.getThreadGroup();
			}
		}
		
		g.checkAccess();
		
		if(security != null) {
			if(isCCLOverridden(getClass())) {
				security.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
			}
		}
		
//		g.addUnstarted();
		
		this.group = g;
		this.daemon = parent.isDaemon();
		this.priority = parent.getPriority();
		this.name = name.toCharArray();
		if(security == null || isCCLOverridden(parent.getClass())) {
			this.contextClassLoader = parent.getContextClassLoader();
		} else {
			this.contextClassLoader = parent.contextClassLoader;
		}
		this.inheritedAccessControlContext = AccessController.getContext();
		this.target = target;
		setPriority(priority);
		
		if(parent.inheritableThreadLocals != null) {
			this.inheritableThreadLocals = ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
			this.stackSize = stackSize;
			tid = nextThreadID();
			this.me = this;
		}
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		Thread t;
		synchronized (this) {
			t = (Thread) super.clone();
			
			t.tid = nextThreadID();
			t.parkBlocker = null;
			t.blocker = null;
			t.blockerLock = new Object();
			t.threadLocals = null;
			
			group.checkAccess();
			if(threadStatus == 0) {
//				group.addUnstarted();
			}
			t.setPriority(priority);
			
			final Thread current = Thread.currentThread();
			if(current.inheritableThreadLocals != null) {
				t.inheritableThreadLocals = ThreadLocal.createInheritedMap(current.inheritableThreadLocals);
			}
		}
		
		t.me = t;
		return t;
	} 
	
	private void setPriority(int priority) {
		// TODO Auto-generated method stub
		
	}

	private int getPriority() {
		// TODO Auto-generated method stub
		return 0;
	}

	private boolean isDaemon() {
		// TODO Auto-generated method stub
		return false;
	}

	private static final RuntimePermission SUBCLASS_IMPLEMENTATION_PERMISSION =
             new RuntimePermission("enableContextClassLoaderOverride");
	 
	private boolean isCCLOverridden(Class<? extends Thread> class1) {
		// TODO Auto-generated method stub
		return false;
	}

	private ThreadGroup getThreadGroup() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void run() {
		
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

	public final native boolean isAlive();
	
	public synchronized void start() {
		
	}

	public ClassLoader getContextClassLoader() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setContextClassLoader(ClassLoader ccl) {
		// TODO Auto-generated method stub
		
	}

}
