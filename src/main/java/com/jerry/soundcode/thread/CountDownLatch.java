package com.jerry.soundcode.thread;

import java.util.concurrent.TimeUnit;

import com.jerry.soundcode.concurrent.locks.AbstractQueuedSynchronizer;

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
		sync.acquireShoredInterruptibly(1);
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
