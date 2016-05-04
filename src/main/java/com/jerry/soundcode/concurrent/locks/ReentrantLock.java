package com.jerry.soundcode.concurrent.locks;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class ReentrantLock implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Sync sync;
	
	static abstract class Sync extends AbstractQueuedSynchronizer {

		private static final long serialVersionUID = 1L;
		
		abstract void lock();
		
		final boolean nonfairTryAcquire(int acquires) {
			final Thread current = Thread.currentThread();
			int c = getState();
			if(c == 0) {
				if(compareAndSetState(0, acquires)) {
					setExclusiveOwnerThread(current);
					return true;
				}
			} else if(current == getExclusiveOwnerThread()) {
				int nextc = c + acquires;
				if(nextc < 0) {
					throw new Error("Maximum lock count exceeded");
				}
				setState(nextc);
				return true;
			}
			return false;
		}
		
		protected final boolean tryRelease(int releases) {
			int c = getState() - releases;
			if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
			boolean free = false;
			if(c == 0) {
				free = true;
				setExclusiveOwnerThread(null);
			}
			setState(c);
			return free;
		}
		
		protected final boolean isHeldExclusively() {
			return getExclusiveOwnerThread() == Thread.currentThread();
		}
		
		final ConditionObject newCondition() {
			return new ConditionObject();
		}
		
		final Thread getOwner() {
			return getState() == 0 ? null : getExclusiveOwnerThread();
		}
		
		final int getHoldCount() {
			return isHeldExclusively() ? getState() : 0;
		}
		
		final boolean isLocked() {
			return getState() != 0;
		}
		
		private void readObject(ObjectInputStream s) 
				throws java.io.IOException, ClassNotFoundException {
			s.defaultReadObject();
			setState(0);
		}
	}
	
	final static class NonfairSync extends Sync {

		private static final long serialVersionUID = 1L;

		@Override
		final void lock() {
			if(compareAndSetState(0, 1)) {
				setExclusiveOwnerThread(Thread.currentThread());
			} else {
				acquire(1);
			}
		}
		
		protected final boolean tryAcquire(int acquires) {
			return nonfairTryAcquire(acquires);
		}
	}
	
	final static class FairSync extends Sync {

		private static final long serialVersionUID = 1L;

		@Override
		final void lock() {
			acquire(1);
		}
		
		protected final boolean tryAcquire(int acquires) {
			final Thread current = Thread.currentThread();
			int c = getState();
			if(c == 0) {
				if(isFirst(current) && compareAndSetState(0, acquires)) {
					setExclusiveOwnerThread(current);
					return true;
				}
			} else if(current == getExclusiveOwnerThread()) {
				int nextc = c + acquires;
				if (nextc < 0)
					 throw new Error("Maximum lock count exceeded");
				setState(nextc);
				return true;
			}
			return false;
		}
	}
	
	public ReentrantLock() {
		sync = new NonfairSync();
	}
	
	public ReentrantLock(boolean fair) {
		sync = (fair) ? new FairSync() : new NonfairSync();
	}
	
	public void lock() {
		sync.lock();
	}
	
	public void lockInterruptibly() throws InterruptedException {
		sync.acquireInterrutibly(1);
	}
	
	public boolean tryLock() {
		return sync.nonfairTryAcquire(1);
	}
	
	public boolean tryLock(long timeout, TimeUnit unit) throws Exception { 
		return sync.tryAcquireNanos(1, unit.toNanos(timeout));
	}
	
	public void unlock() {
		sync.release(1);
	}
	
	public Condition newCondition() {
		return sync.newCondition();
	}
	
	public int getHoldCount() {
		return sync.getHoldCount();
	}
	
	public boolean isHeldByCurrentThread() {
		return sync.isHeldExclusively();
	}
	
	public boolean isLocked() {
		return sync.isLocked();
	}
	
	public final boolean isFair() {
		return sync instanceof FairSync;
	}
	
	// TODO 
	
}
