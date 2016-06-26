package com.jerry.soundcode.thread;

import java.util.concurrent.TimeUnit;

import com.jerry.soundcode.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * CountDownLatch这个类能够使一个线程等待其他线程完成各自的工作后再执行。例如，应用程序的主线程希望在负责启动框架服务的线程已经启动所有的框架服务之后再执行。
 * CountDownLatch是通过一个计数器来实现的，计数器的初始值为线程的数量。每当一个线程完成了自己的任务后，计数器的值就会减1。
 * 当计数器值到达0时，它表示所有的线程已经完成了任务，然后在闭锁上等待的线程就可以恢复执行任务。
 */
public class CountDownLatch {

	private static final class Sync extends AbstractQueuedSynchronizer {
		
		private static final long serialVersionUID = 1L;

		Sync(int count) {
			setState(count);
		}
		
		int getCount() {
			return getState();
		}
		
		public int tryAcquireShared(int acquires) {
			return getState() == 0 ? 1 : -1;
		}
		
		public boolean tryReleaseShared(int releases) {
			for(;;) {
				int c = getState();
				if(c == 0) {
					return false;
				}
				int nextc = c - 1;
				if(compareAndSetState(c, nextc)) {
					return nextc == 0;
				}
			}
		}
	}

	private final Sync sync;
	
	public CountDownLatch(int count) {
		if(count < 0) {
			throw new IllegalArgumentException("count < 0");
		}
		this.sync = new Sync(count);
	}
	
	public void await() throws InterruptedException {
		sync.acquireSharedInterruptibly(1);
	}
	
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException{
		return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
	}
	
	public void countDown() {
		sync.releaseShared(1);
	}
	
	public long getCount() {
		return sync.getCount();
	}
	
	public String toString() {
        return super.toString() + "[Count = " + sync.getCount() + "]";
    }
 }
