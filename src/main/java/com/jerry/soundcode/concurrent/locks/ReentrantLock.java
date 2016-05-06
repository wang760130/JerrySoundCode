package com.jerry.soundcode.concurrent.locks;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * 读写锁拆成读锁和写锁来理解。读锁可以共享，多个线程可以同时拥有读锁，但是写锁却只能只有一个线程拥有，而且获取写锁的时候其他线程都已经释放了读锁，
 * 而且该线程获取写锁之后，其他线程不能再获取读锁。简单的说就是写锁是排他锁，读锁是共享锁。
 */
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
