package com.jerry.soundcode.concurrent.locks;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import com.jerry.soundcode.thread.Thread;
import com.jerry.soundcode.list.Collection;

/**
 * 读写锁：分为读锁和写锁，多个读锁不互斥，读锁与写锁互斥，这是由jvm自己控制的，你只要上好相应的锁即可。
 * 如果你的代码只读数据，可以很多人同时读，但不能同时写，那就上读锁；如果你的代码修改数据，只能有一个人在写，且不能同时读取，那就上写锁。
 * 总之，读的时候上读锁，写的时候上写锁！
 * 
 * ReentrantReadWriteLock会使用两把锁来解决问题，一个读锁，一个写锁
 * 线程进入读锁的前提条件：
 * 没有其他线程的写锁，
 * 没有写请求或者有写请求，但调用线程和持有锁的线程是同一个
 * 线程进入写锁的前提条件：
 * 没有其他线程的读锁
 * 没有其他线程的写锁
 */
public class ReentrantReadWriteLock implements ReadWriteLock, Serializable{

	private static final long serialVersionUID = 1L;

	private final ReentrantReadWriteLock.ReadLock readerLock;
	
	private final ReentrantReadWriteLock.WriteLock writerLock;
	
	private final Sync sync;
	
	public ReentrantReadWriteLock() {
		this(false);
	}
	
	public ReentrantReadWriteLock(boolean fair) {
		sync = (fair) ? new FairSync() : new NonfairSync();
		readerLock = new ReadLock(this);
		writerLock = new WriteLock(this);
	}
	
	public ReentrantReadWriteLock.WriteLock writeLock() { 
		return writerLock; 
	}
	
    public ReentrantReadWriteLock.ReadLock  readLock()  {
    	return readerLock; 
    }

	static abstract class Sync extends AbstractQueuedSynchronizer {

		private static final long serialVersionUID = 1L;
		
		static final int SHARED_SHIFT 	= 16;
		static final int SHARED_UNIT  	= (1 << SHARED_SHIFT);
		static final int MAX_COUNT    	= (1 << SHARED_SHIFT) - 1;
		static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;
		
		static int sharedCount(int c) {
			return c >>> SHARED_SHIFT;
		}
		
		static int exclusiveCount(int c) {
			return c & EXCLUSIVE_MASK;
		}
		
		static final class HoldCounter {
			int count;
			final long tid = Thread.currentThread().getId();
			int tryDecrement() {
				int c = count;
				if(c > 0) {
					count = c - 1;
				}
				return c;
			}
		}
		
		static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter> {
			public HoldCounter initialValue() {
				return new HoldCounter();
			}
		}
		
		transient ThreadLocalHoldCounter readHolds;
		
		transient HoldCounter cachedHoldCounter;
		
		Sync() {
			readHolds = new ThreadLocalHoldCounter();
			setState(getState());
		}
		
		abstract boolean readerShouldBlock(Thread current);
		
		abstract boolean writerShouldBlock(Thread current);
		
		@Override
		protected final boolean tryRelease(int releases) {
			int nextc = getState() - releases;
			if(Thread.currentThread() != getExclusiveOwnerThread()) {
				throw new IllegalMonitorStateException();
			}
			if(exclusiveCount(nextc) == 0) {
				setExclusiveOwnerThread(null);
				setState(nextc);
				return true;
			} else {
				setState(nextc);
				return false;
			}
		}
		
		@Override
		protected final boolean tryAcquire(int acquires) {
			Thread current = Thread.currentThread();
			int c = getState();
			int w = exclusiveCount(c);
			
			if(c != 0) {
				if(w == 0 || current != getExclusiveOwnerThread()) {
					return false;
				}
				if(w + exclusiveCount(acquires) > MAX_COUNT) {
					throw new Error("Maximum lock count exceeded");
				}
			}
			
			if((w == 0 && writerShouldBlock(current)) || !compareAndSetState(c, c + acquires)) {
				return false;
			}
			setExclusiveOwnerThread(current);
			return true;
		}
		
		@Override
		protected final boolean tryReleaseShared(int unused) {
			HoldCounter rh = cachedHoldCounter;
			Thread current = Thread.currentThread();
			if(rh == null || rh.tid != current.getId()) {
				rh = readHolds.get();
			}
			
			if(rh.tryDecrement() <= 0) {
				throw new IllegalMonitorStateException();
			}
			
			for(;;) {
				int c = getState();
				int nextc = c - SHARED_UNIT;
				if(compareAndSetState(c, nextc)) {
					return nextc == 0;
				}
			}
		}
		
		@Override
		protected final int tryAcquireShared(int unused) {
			Thread current = Thread.currentThread();
			int c = getState();
			if(exclusiveCount(c) != 0 && getExclusiveOwnerThread() != current) {
				return -1;
			}
			
			if(sharedCount(c) == MAX_COUNT) {
				throw new Error("Maximum lock count exceeded");
			}
			
			if(!readerShouldBlock(current) && compareAndSetState(c, c + SHARED_SHIFT)) {
				HoldCounter rh = cachedHoldCounter;
				if(rh == null || rh.tid != current.getId()) {
					cachedHoldCounter = rh = readHolds.get();
				}
				rh.count ++;
				return 1;
			}
			return fullTryAcquireShared(current);
		}
		
		final int fullTryAcquireShared(Thread current) {
			HoldCounter rh = cachedHoldCounter;
			if(rh == null || rh.tid != current.getId()) {
				rh = readHolds.get();
			}
			for(;;) {
				int c = getState();
				int w = exclusiveCount(c);
				if((w != 0 && getExclusiveOwnerThread() != current) ||
						((rh.count | w) == 0 && readerShouldBlock(current))) {
					return -1;
				}
				if(sharedCount(c) == MAX_COUNT) {
					throw new Error("Maximum lock count exceeded");
				}
				if(compareAndSetState(c, c + SHARED_UNIT)) {
					cachedHoldCounter = rh;
					rh.count++;
					return 1;
				}
			}
		}
		
		final boolean tryWriteLock() {
			Thread current = Thread.currentThread();
			int c = getState();
			if(c != 0) {
				int w = exclusiveCount(c);
				if(w == 0 || current != getExclusiveOwnerThread()) {
					return false;
				}
				if(w == MAX_COUNT) {
					 throw new Error("Maximum lock count exceeded");
				}
			}
			
			if(!compareAndSetState(c, c + 1)) {
				return false;
			}
			setExclusiveOwnerThread(current);
			return false;
		}
		
		final boolean tryReadLock() {
			Thread current = Thread.currentThread();
			for(;;) {
				int c = getState();
				if(exclusiveCount(c) != 0 && getExclusiveOwnerThread() != current) {
					return false;
				}
				
				if(sharedCount(c) == MAX_COUNT) {
					 throw new Error("Maximum lock count exceeded");
				}
				
				if(compareAndSetState(c, c + SHARED_UNIT)) {
					HoldCounter rh = cachedHoldCounter;
					if(rh == null || rh.tid != current.getId()) {
						cachedHoldCounter = rh = readHolds.get();
					}
					rh.count ++;
					return true;
				}
			}
		}
		
		@Override
		protected final boolean isHeldExclusively() {
			return getExclusiveOwnerThread() == Thread.currentThread();
		}
		
		final ConditionObject newCondition() {
			return new ConditionObject();
		}
		
		final Thread getOwner() {
			return ((exclusiveCount(getState()) == 0) ? null : getExclusiveOwnerThread());
		}
		
		final int getReadLockCount() {
			return sharedCount(getState());
		}
		
		final boolean isWriteLocked() {
			return exclusiveCount(getState()) != 0;
		}
		
		final int getWriteHoldCount() {
			return isHeldExclusively() ? exclusiveCount(getState()) : 0;
		}
			
		final int getReadHoldCount() {
			return getReadHoldCount() == 0 ? 0 : readHolds.get().count;
		}
		
		private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException{
			s.defaultReadObject();
			readHolds = new ThreadLocalHoldCounter();
		}
		
		final int getCount() {
			return getState();
		}
	}
	
	final static class NonfairSync extends Sync {

		private static final long serialVersionUID = 1L;
		
		@Override
		final boolean writerShouldBlock(Thread current) {
			return false;
		}
		
		@Override
		final boolean readerShouldBlock(Thread current) {
			return apparentlyFirstQueuedIsExclusive();
		}
	}
	
	final static class FairSync extends Sync {

		private static final long serialVersionUID = 1L;

		@Override
		boolean readerShouldBlock(Thread current) {
			return !isFirst(current);
		}

		@Override
		boolean writerShouldBlock(Thread current) {
			return !isFirst(current);
		}
	}
	

	public static class ReadLock implements Lock, Serializable {
		
		private static final long serialVersionUID = 1L;
		
		private final Sync sync;
		
		protected ReadLock(ReentrantReadWriteLock lock) {
			sync = lock.sync;
		}
		
		@Override
		public void lock() {
			sync.acquireShared(1);
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			sync.acquireSharedInterruptibly(1);
		}

		@Override
		public boolean tryLock() {
			return sync.tryReadLock();
		}

		@Override
		public boolean tryLock(long timeout, TimeUnit unit)
				throws InterruptedException {
			return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
		}

		@Override
		public void unlock() {
			sync.releaseShared(1);
		}

		@Override
		public Condition newCondition() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public String toString() {
            int r = sync.getReadLockCount();
            return super.toString() +
                "[Read locks = " + r + "]";
        }
	}
	
	public static class WriteLock implements Lock, Serializable {

		private static final long serialVersionUID = 1L;
		
		private final Sync sync;
		
		protected WriteLock(ReentrantReadWriteLock lock) {
			sync = lock.sync;
		}
		
		@Override
		public void lock() {
			sync.acquire(1);
		}
		
		@Override
		public void lockInterruptibly() throws InterruptedException {
			sync.acquireInterrutibly(1);
		}
		
		@Override
		public boolean tryLock() {
			return sync.tryWriteLock();
		}
		
		@Override
		public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
			return sync.tryAcquireNanos(1, unit.toNanos(timeout));
		}
		
		@Override
		public void unlock() {
			sync.release(1);
		}
		
		@Override
		public Condition newCondition() {
			return sync.newCondition();
		}
		
		@Override
		public String toString() {
            Thread o = sync.getOwner();
            return super.toString() + ((o == null) ?
                                       "[Unlocked]" :
                                       "[Locked by thread " + o.getName() + "]");
        }
		
		public boolean isHeldByCurrentThread() {
			return sync.isHeldExclusively();
		}
		
		public int getHoldCount() {
			return sync.getWriteHoldCount();
		}

	}
	
	public final boolean isFair() {
		return sync instanceof FairSync;
	}
	
	protected Thread getOwner() {
		return sync.getOwner();
	}
	
	public int getReadLockCount() {
		return sync.getReadLockCount();
	}
	
	public boolean isWriteLocked() {
		return sync.isWriteLocked();
	}
	
	public boolean isWriteLockedByCurrentThread() {
		return sync.isHeldExclusively();
	}
	
	public int getWriteHoldCount() {
		return sync.getWriteHoldCount();
	}
	
	public int getReadHoldCount() {
		return sync.getReadHoldCount();
	}
	
	protected Collection<Thread> getQueuedWriterThreads() {
		return sync.getExclusiveQueuedThreads();
	}
	
	protected Collection<Thread> getQueuedReaderThreads() {
		return sync.getSharedQueuedThreads();
	}
	
	public final boolean hasQueuedThreads() {
		return sync.hasQueuedThreads();
	}
	
	public final boolean hasQueuedThread(Thread thread) {
		return sync.isQueued(thread);
	}
	
	public final int getQueueLength() {
		return sync.getQueueLength();
	}
	
	protected Collection<Thread> getQueuedThreads() {
		return sync.getQueuedThreads();
	}
	
	public boolean hasWaiters(Condition condition) {
		if(condition == null) {
			throw new NullPointerException();
		}
		if(!(condition instanceof AbstractQueuedSynchronizer)) {
			throw new IllegalArgumentException("not owner");
		}
		return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
	}
	
	public int getWaitQueueLength(Condition condition) {
		if(condition == null) {
			throw new NullPointerException();
		}
		if(!(condition instanceof AbstractQueuedSynchronizer)) {
			throw new IllegalArgumentException("not owner");
		}
		return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
	}
	
	public Collection<Thread> getWaitingThreads(Condition condition) {
		if(condition == null) {
			throw new NullPointerException();
		}
		if(!(condition instanceof AbstractQueuedSynchronizer)) {
			throw new IllegalArgumentException("not owner");
		}
		return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
	}
	
	public String toString() {
        int c = sync.getCount();
        int w = Sync.exclusiveCount(c);
        int r = Sync.sharedCount(c);

        return super.toString() +
            "[Write locks = " + w + ", Read locks = " + r + "]";
    }
}
