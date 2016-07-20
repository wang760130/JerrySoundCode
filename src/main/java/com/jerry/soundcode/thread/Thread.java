package com.jerry.soundcode.thread;

import java.lang.Thread.UncaughtExceptionHandler;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.jerry.soundcode.map.HashMap;
import com.jerry.soundcode.map.Map;

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
	
	public Thread() {
		init(null, null, "Thread-" + nextThreadNum(),0);
	}
	
	public Thread(Runnable target) {
		init(null, target, "Thread-" + nextThreadNum(),0);
	}
	
	public Thread(ThreadGroup group, Runnable target) {
		init(group, target, "Thread-" + nextThreadNum(), 0);
	}
	
	public Thread(String name) {
		init(null, null, name, 0);
	}
	
	public Thread(ThreadGroup group, String name) {
		init(group, null, name, 0);
	}
	
	public Thread(Runnable target, String name) {
		init(null, target, name, 0);
	}
	
	public Thread(ThreadGroup group, Runnable target, String name) {
		init(group, target, name, 0);
	}
	
	public Thread(ThreadGroup group, Runnable target, String name, long stackSize) {
		init(group, target, name, stackSize);
	}
	
	public synchronized void start() {
		if(threadStatus != 0 || this != me) {
			throw new IllegalThreadStateException();
		}
//		group.add(this);
		start0();
		if(stopBeforeStart) {
			start0();
		}
	}
	
	private native void start0();
	
	@Override
	public void run() {
		if(target != null) {
			target.run();
		}
	}
	
	private void exit() {
		if(group != null) {
//			group.remove(this);
			group = null;
		}
		
		target = null;
		threadLocals = null;
		inheritableThreadLocals = null;
		inheritedAccessControlContext = null;
		blocker = null;
		uncaughtExceptionHandler = null;
	}
	
	public final void stop() {
		if((threadStatus != 0) && !isAlive()) {
			return ;
		}
		stop1(new ThreadDeath());
	}
	
	public final synchronized void stop(Throwable obj) {
		stop1(obj);
	}
	
	private void stop1(Throwable th) {
		SecurityManager security = System.getSecurityManager();
		if(security != null) {
			checkAccess();
			if((this != Thread.currentThread()) || (!(th instanceof ThreadDeath))) {
//				security.checkPermission(SecurityConstants.STOP_THREAD_PERMISSION);
			}
		}
		
		if(threadStatus != 0) {
			resume();
			stop0(th);
		} else {
			if(th == null) {
				throw new NullPointerException();
			}
			
			stopBeforeStart = true;
			throwableFromStop = th;
		}
	}

	public void interrupt() {
		if(this != Thread.currentThread()) {
			checkAccess();
		}
		
		synchronized(blockerLock) {
			Interruptible b = blocker;
			if(b != null) {
				interrupt0();
				b.interrupt();
				return ;
			}
		}
		interrupt0();
	}
	
	public static boolean interrupted() {
		return currentThread().isInterrupted(true);
	}
	
	public boolean isInterrupted() {
		return isInterrupted(false);
	}
	
	private native boolean isInterrupted(boolean ClearInterrupted) ;

	
	public void destroy() {
		throw new NoSuchMethodError();
	}
	
	public final native boolean isAlive();
	
	public final void suspend() {
		checkAccess();
		suspend0();
	}
	
	public final void resume() {
		checkAccess();
		resume0();
	}
	
	public final void setPriority(int newPriority) {
		ThreadGroup g;
		checkAccess();
		if(newPriority > MAX_PRIORITY || newPriority < MIN_PRIORITY) {
			throw new IllegalArgumentException();
		}
		
		if((g = getThreadGroup()) != null) {
			if(newPriority > g.getMaxPriority()) {
				newPriority = g.getMaxPriority();
			}
			setPriority0(priority = newPriority);
		}
	}
	
	public final int getPriority() {
		return priority;
	}
	
	public final void setName(String name) {
		checkAccess();
		this.name = name.toCharArray();
	}
	
	public final String getName() {
		return String.valueOf(name);
	}
	
	public final ThreadGroup getThreadGroup() {
		return group;
	}
	
	public static int activeCount() {
		return currentThread().getThreadGroup().activeCount();
	}
	
	public static int enumerate(java.lang.Thread[] tarray) {
		return currentThread().getThreadGroup().enumerate(tarray);
	}
	
	public native int countStackFrames();
	
	public final synchronized void join(long millis) throws InterruptedException {
		long base = System.currentTimeMillis();
		long now = 0;
		
		if(millis < 0) {
			throw new IllegalArgumentException("timeout value is negative");
		}
		
		if(millis == 0) {
			while(isAlive()) {
				wait(0);
			}
		} else {
			while(isAlive()) {
				long delay = millis - now;
				if(delay <= 0) {
					break;
				}
				wait(delay);
				now = System.currentTimeMillis() - base;
			}
		}
	}
	
	public final synchronized void join(long millis, int nanos) throws InterruptedException {
		if(millis < 0) {
			throw new IllegalArgumentException("timeout value is negative");
		}
		
		if(nanos < 0 || nanos > 999999) {
			throw new IllegalArgumentException("nanosecond timeout value out of range");
		}
		
		if(nanos >= 500000 || (nanos != 0 && millis == 0)) {
			millis ++;
		}
		
		join(millis);
	}
	
	public final void join() throws InterruptedException {
		join(0);
	}
	
	public static void dumpStack() {
		new Exception("Stack trace").printStackTrace();
	}
	
	public final void setDaemon(boolean on) {
		checkAccess();
		if(isAlive()) {
			throw new IllegalArgumentException();
		}
		daemon = on;
	}
	
	public final boolean isDaemon() {
		return daemon;
	}
	
	public final void checkAccess() {
		SecurityManager security = System.getSecurityManager();
		if(security != null) {
//			security.checkAccess(this);
		}
	}
	
	public String toString() {
        ThreadGroup group = getThreadGroup();
        if (group != null) {
        	return "Thread[" + getName() + "," + getPriority() + "," + group.getName() + "]";
        } else {
        	return "Thread[" + getName() + "," + getPriority() + "," + "" + "]";
        }
    }
	
	public ClassLoader getContextClassLoader() {
		if(contextClassLoader == null) {
			return null;
		}
		
		SecurityManager sm = System.getSecurityManager();
		if(sm != null) {
//			ClassLoader ccl = ClassLoader.getCallerClassLoader();
//			if(ccl != null && ccl != contextClassLoader && !contextClassLoader.isAncestor(ccl)) {
//				sm.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);
//			}
		}
		return contextClassLoader;
	}
	
	public void setContextClassLoader(ClassLoader cl) {
		SecurityManager sm = System.getSecurityManager();
		if(sm != null) {
			sm.checkPermission(new RuntimePermission("setContextClassLoader"));
		}
		contextClassLoader = cl;
	}
	
	public static native boolean holdsLock(Object obj) ;
	
	private static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];	

	public StackTraceElement[] getStackTrace() {
		if(this != Thread.currentThread()) {
			
			SecurityManager security = System.getSecurityManager();
			if(security != null) {
//				security.checkPermission(SecurityConstants.GET_STACK_TRACE_PERMISSION);
			} 
			
			if(!isAlive()) {
				return EMPTY_STACK_TRACE;
			}
			
			StackTraceElement[][] stackTraceArray = dumpThreads(new Thread[] {this});
			StackTraceElement[] stackTrace = stackTraceArray[0];
			if(stackTrace == null) {
				stackTrace = EMPTY_STACK_TRACE;
			}
			return stackTrace;
		} else {
			return (new Exception()).getStackTrace();
		}
	}
	
	public static Map<Thread, StackTraceElement[]> getAllStackTraces() {
		SecurityManager security = System.getSecurityManager();
		if(security != null) {
//			security.checkPermission(SecurityConstants.GET_STACK_TRACE_PERMISSION);
//			security.checkPermission(SecurityConstants.MODIFY_THREADGROUP_PERMISSION);
		}
		
		Thread[] threads = getThreads();
		StackTraceElement[][] traces = dumpThreads(threads);
		Map<Thread, StackTraceElement[]> m = new HashMap<Thread, StackTraceElement[]> (threads.length);
		for(int i = 0; i < threads.length; i++) {
			StackTraceElement[] stackTrace = traces[i];
			if(stackTrace != null) {
				m.put(threads[i], stackTrace);
			}
		}
		return m;
	}
	
	private static final RuntimePermission SUBCLASS_IMPLEMENTATION_PERMISSION =  new RuntimePermission("enableContextClassLoaderOverride");
	 
	@SuppressWarnings("restriction")
	private static final sun.misc.SoftCache subclassAudits = new sun.misc.SoftCache(10);
	
	@SuppressWarnings({ "rawtypes", "restriction" })
	private static boolean isCCLOverridden(Class cl) {
		if(cl == Thread.class) {
			return false;
		}
		
		Boolean result = null;
		synchronized(subclassAudits) {
			result = (Boolean) subclassAudits.get(cl);
			if(result == null) {
				result = new Boolean(auditSubclass(cl));
				subclassAudits.put(cl, result);
			}
		}
		return result.booleanValue();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static boolean auditSubclass(final Class subcl) {
		Boolean result = (Boolean) AccessController.doPrivileged(
			new PrivilegedAction() {

				@Override
				public Object run() {
					for(Class cl = subcl; cl != Thread.class; cl = cl.getSuperclass()) {
						try {
							cl.getDeclaredMethod("getContextClassLoader", new Class[0]);
							return Boolean.TRUE;
						} catch (NoSuchMethodException e) {
						}
						
						Class[] params = {ClassLoader.class};
						try {
							cl.getDeclaredMethod("setContextClassLoader", params);
							return Boolean.TRUE;
						} catch (NoSuchMethodException e) {
						}
					}
					return Boolean.FALSE;
				}
				
			}
		);
		return result.booleanValue();
	}

	private native static StackTraceElement[][] dumpThreads(Thread[] threads) ;
	private native static Thread[] getThreads();
	
	public long getId() {
		return tid;
	}
	
	public enum State {
		NEW,
		
		RUNNABLE,
		
		BLOCKED,
		
		WAITING,
		
		TIMED_WAITING,
		
		TERMINATED;
	}
	
	@SuppressWarnings("restriction")
	public java.lang.Thread.State getState() {
		return sun.misc.VM.toThreadState(threadStatus);
	}
	
	public interface UncaughtExceptionHander {
		void uncaughtException(Thread t, Throwable e);
	}
	
	private volatile UncaughtExceptionHandler uncaughtExceptionHandler;
	
	private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;
	
	public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
		SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
        	sm.checkPermission(new RuntimePermission("setDefaultUncaughtExceptionHandler"));
        }
        defaultUncaughtExceptionHandler = eh;
     }
	
	public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler(){
        return defaultUncaughtExceptionHandler;
    }
	
	public UncaughtExceptionHandler getUncaughtExceptionHandler() { 
        return (UncaughtExceptionHandler) (uncaughtExceptionHandler != null ?
            uncaughtExceptionHandler : group);
    }
	
	public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) { 
        checkAccess();
        uncaughtExceptionHandler = eh;
    }
	
	private void dispatchUncaughtException(Throwable e) {
//        getUncaughtExceptionHandler().uncaughtException(this, e);
    }
	
	private native void setPriority0(int newPriority);
    private native void stop0(Object o);
    private native void suspend0();
    private native void resume0();
    private native void interrupt0();
	
}
