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
		
		// 尝试获取资源，立即返回。成功则返回true，否则false。
		@Override
		protected boolean tryAcquire(int acquires) {
			// 这里限定只能为1个量
			assert acquires == 1;
			// state为0才设置为1，不可重入！
			if(compareAndSetState(0, 1)) {
				// 设置为当前线程独占资源
				setExclusiveOwnerThread(Thread.currentThread());
				return true;
			}
			return false;
		}

		// 尝试释放资源，立即返回。成功则为true，否则false。
		@Override
		protected boolean tryRelease(int releases) {
			// 限定为1个量
			assert releases == 1;
			
			if(getState() == 0) {
				// 既然来释放，那肯定就是已占有状态了。只是为了保险，多层判断！
				throw new IllegalMonitorStateException();
			}
			setExclusiveOwnerThread(null);
			// 释放资源，放弃占有状态
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
	// 真正同步类的实现都依赖继承于AQS的自定义同步器！
	private final Sync sync = new Sync();
	
	// lock<-->acquire。两者语义一样：获取资源，即便等待，直到成功才返回。
	@Override
	public void lock() {
		sync.acquire(1);
	}

	// tryLock<-->tryAcquire。两者语义一样：尝试获取资源，要求立即返回。成功则为true，失败则为fa
	@Override
	public boolean tryLock() {
		return sync.tryAcquire(1);
	}
	
	// unlock<-->release。两者语文一样：释放资源。
	@Override
	public void unlock() {
		sync.release(1);
	}
	
	@Override
	public Condition newCondition() {
		return sync.newCondition();
	} 
	
	// 锁是否占有状态
	public boolean isLocked() {
		return sync.isHeldExclusively();
	}
	
	public boolean hasQueuedThreads() {
		return sync.hasQueuedThreads();
	}
	
	@Override
	public void lockInterruptibly() throws InterruptedException {
		sync.acquireInterruptibly(1);
	}

	@Override
	public boolean tryLock(long timeout, TimeUnit unit)
			throws InterruptedException {
		return sync.tryAcquireNanos(1, unit.toNanos(timeout));
	}

}
