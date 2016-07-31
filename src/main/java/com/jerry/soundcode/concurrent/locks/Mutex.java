package com.jerry.soundcode.concurrent.locks;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import com.jerry.soundcode.thread.Thread;

/**
 * 排他锁的实现，一次只能一个线程获取到锁
 */
public class Mutex implements Lock, Serializable {
	
	private static final long serialVersionUID = 1L;

	// 内部类，自定义同步器
	private static class Sync extends AbstractQueuedSynchronizer {
		
		private static final long serialVersionUID = 1L;

		// 是否处于占用状态
		@Override
		protected boolean isHeldExclusively() {
			return getState() == 1;
		}
		
		// // 当状态为0的时候获取锁
		@Override
		protected boolean tryAcquire(int acquires) {
			assert acquires == 1;
			if(compareAndSetState(0, 1)) {
				setExclusiveOwnerThread(Thread.currentThread());
				return true;
			}
			return false;
		}

		@Override
		protected boolean tryRelease(int releases) {
			assert releases == 1;
			if(getState() == 0) {
				throw new IllegalMonitorStateException();
			}
			setExclusiveOwnerThread(null);
			setState(0);
			return true;
		}

		@Override
		protected int tryAcquireShared(int arg) {
			return super.tryAcquireShared(arg);
		}

		@Override
		protected boolean tryReleaseShared(int arg) {
			return super.tryReleaseShared(arg);
		}
		
		Condition newCondition() {
			return new ConditionObject();
		}
	}
	
	// 仅需要将操作代理到Sync上即可
	private final Sync sync = new Sync();
	
	@Override
	public void lock() {
		sync.acquire(1);
	}

	@Override
	public boolean tryLock() {
		return sync.tryAcquire(1);
	}
	
	@Override
	public void unlock() {
		sync.release(1);
	}
	
	@Override
	public Condition newCondition() {
		return sync.newCondition();
	} 
	
	public boolean isLocked() {
		return sync.isHeldExclusively();
	}
	
	public boolean hasQueuedThreads() {
		return sync.hasQueuedThreads();
	}
	
	@Override
	public void lockInterruptibly() throws InterruptedException {
		sync.acquireInterrutibly(1);
	}

	@Override
	public boolean tryLock(long timeout, TimeUnit unit)
			throws InterruptedException {
		return sync.tryAcquireNanos(1, unit.toNanos(timeout));
	}

}
