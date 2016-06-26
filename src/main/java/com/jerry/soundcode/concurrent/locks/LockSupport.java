package com.jerry.soundcode.concurrent.locks;

import com.jerry.soundcode.concurrent.atomic.Unsafe;
import com.jerry.soundcode.thread.Thread;

/**
 * LockSupport是JDK中比较底层的类，用来创建锁和其他同步工具类的基本线程阻塞原语。
 * java锁和同步器框架的核心 AQS: AbstractQueuedSynchronizer，
 * 就是通过调用 LockSupport.park()和 LockSupport.unpark()实现线程的阻塞和唤醒 的。
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
	
	/**
	 * unpark相当于释放许可(或使许可变为可用)。调用unpark方法会使目标线程在之前阻塞(调用park)地方继续执行，如果目标线程之前没有调用过park，那么在接下来调用park时不会阻塞。
	 * @param thread
	 */
	public static void unpark(Thread thread) {
		if(thread != null) {
			unsafe.unpark(thread);
		}
	}
	
	/**
	 * park相当于获取可用的许可(初始的许可不可用)，调用park()方法会使得当前调用线程阻塞(之前不要调用unpark方法)
	 * @param blocker
	 */
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
