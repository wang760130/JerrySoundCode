package com.jerry.soundcode.concurrent.locks;

import com.jerry.soundcode.concurrent.atomic.Unsafe;

/**
 * LockSupport是JDK中比较底层的类，用来创建锁和其他同步工具类的基本线程阻塞原语。
 * java锁和同步器框架的核心 AQS: AbstractQueuedSynchronizer，
 * 就是通过调用 LockSupport .park()和 LockSupport .unpark()实现线程的阻塞和唤醒 的。
 *
 */
public class LockSupport {

	private LockSupport() {};
	
	private static final Unsafe unsafe = Unsafe.getUnsafe();
	
	private static final long parkBlockerOffset;
	
	static {
		try {
			parkBlockerOffset = unsafe.objectFieldOffset(Thread.class.getDeclaredField("parkBlocker"));
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	private static void setBlocker(Thread thread, Object arg) {
		unsafe.putObject(thread, parkBlockerOffset, arg);
	}
	
	public static void unpark(Thread thread) {
		if(thread != null) {
			unsafe.unpark(thread);
		}
	}
	
	public static void park(Object blocker) {
		Thread thread = Thread.currentThread();
		setBlocker(thread, blocker);
		unsafe.park(false, 0L);
		setBlocker(thread, null);
	}
	
	public static void parkNanos(Object blocker, long nanos) {
		if(nanos > 0) {
			Thread thread = Thread.currentThread();
			setBlocker(thread, blocker);
			unsafe.park(false, nanos);
			setBlocker(thread, null);
		}
	}
	
	public static void parkUntil(Object blocker, long deadline) {
		Thread thread = Thread.currentThread();
		setBlocker(thread, blocker);
		unsafe.park(true, deadline);
		setBlocker(thread, null);
	}
	
	public static Object getBlocker(Thread thread) {
		if(thread == null) {
			throw new NullPointerException();
		}
		return unsafe.getObjectVolatile(thread, parkBlockerOffset);
	}
	
	public static void park() {
		unsafe.park(false, 0L);
	}
	
	public static void parkNanos(long nanos) {
		if(nanos > 0) {
			unsafe.park(false, nanos);
		}
	}
	
	public static void parkUnitl(long deadline) {
		unsafe.park(true, deadline);
	}
}
