package com.jerry.soundcode.concurrent.locks;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import com.jerry.soundcode.thread.Thread;
import com.jerry.soundcode.concurrent.atomic.Unsafe;
import com.jerry.soundcode.list.ArrayList;
import com.jerry.soundcode.list.Collection;

/**
 * 提供了一个基于FIFO队列，可以用于构建锁或者其他相关同步装置的基础框架。
 * 该同步器（以下简称同步器）利用了一个int来表示状态，期望它能够成为实现大部分同步需求的基础。使用的方法是继承，
 * 子类通过继承同步器并需要实现它的方法来管理其状态，管理的方式就是通过类似acquire和release的方式来操纵状态。
 * AbstractQueuedSynchronizer是CountDownLatch/FutureTask/ReentrantLock/RenntrantReadWriteLock/Semaphore的基础
 */
public abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer implements Serializable {

	private static final long serialVersionUID = 1L;

	protected AbstractQueuedSynchronizer() {}
	
	static final class Node {
		
		// 表示当前的线程被取消
		static final int CANCELLED = 1;
		
		// 表示当前节点的后继节点包含的线程需要运行，也就是unpark
		static final int SIGNAL = -1;
		
		// 表示当前节点在等待condition，也就是在condition队列中
		static final int CONDITION = -2;
		
		// 表示当前场景下后续的acquireShared能够得以执行
		static final int PROPAGATE = -3;
		
		static final Node SHARED = new Node();
		
		static final Node EXCLUSIVE = null;
		
		// 节点的等待状态，一个节点可能位于以下几种状态
		volatile int waitStatus;
		
		// 前驱节点，比如当前节点被取消，那就需要前驱节点和后继节点来完成连接。
		volatile Node prev;
		
		// 后继节点
		volatile Node next;
		
		// 入队列时的当前线程
		volatile Thread thread;
		
		// 存储condition队列中的后继节点。
		Node nextWaiter;
		
		final boolean isShared() {
			return next == SHARED;
		}
		
		final Node predecccessor() throws NullPointerException{
			Node p = prev;
			if(p == null) {
				throw new NullPointerException();
			} else {
				return p;
			}
		}
		
		Node() {}
		
		Node(Thread thrad, Node node) {
			this.nextWaiter = node;
			this.thread = thrad;
		}
		
		Node(Thread thread, int waitStatus) {
			this.waitStatus = waitStatus;
			this.thread = thread;
		}
	}
	
	private transient volatile Node head;
	
	private transient volatile Node tail;
	
	private volatile int state;
	
	protected final int getState() {
		return state;
	}
	
	protected final void setState(int newState) {
		state = newState;
	}
	
	protected final boolean compareAndSetState(int expect, int update) {
		return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
	}
	
	static final long spinForTimeoutThresholf = 1000L;
	
	private Node enq(final Node node) {
		for(;;) {
			Node t = tail;
			if(t == null) {
				Node h = new Node();
				h.next = node;
				node.prev = h;
				if(compareAndSetHead(h)) {
					tail = node;
					return h;
				}
			} else {
				node.prev = t;
				if(compareAndSetTail(t, node)) {
					t.next = node;
					return t;
				}
			}
		}
	}
	
	private Node addWaiter(Node mode) {
		Node node = new Node(Thread.currentThread(), mode);			
		
		// 快速尝试在尾部添加
		Node pred = tail;
		if(pred != null) {
			node.prev = pred;
			if(compareAndSetTail(pred, node)) {
				pred.next = node;
				return node;
			}
		}
		enq(node);
		return node;
	}
	
	private void setHead(Node node) {
		head = node;
		node.thread = null;
		node.prev = null;
	}
	
	private void unparkSuccessor(Node node) {
		int ws = node.waitStatus;
		if(ws < 0) {
			compareAndSetWaitStatus(node, ws, 0);
		}
		
		Node s = node.next;
		if(s == null || s.waitStatus > 0) {
			s = null;
			for(Node t = tail; t != null && t != node; t = t.prev) {
				if(t.waitStatus <= 0) {
					s = t;
				}
			}
		}
		
		if(s != null) {
			LockSupport.unpark(s.thread);
		}
	}
	
	private void doReleaseShared() {
		for(;;) {
			Node h = head;
			if(h != null && h != tail) {
				int ws = h.waitStatus;
				if(ws == Node.SIGNAL) {
					if(!compareAndSetWaitStatus(h, Node.SIGNAL,0)) {
						continue;
					}
					unparkSuccessor(h);
				} else if(ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE)) {
					continue;
				}
			}
			
			if(h == head) {
				break;
			}
		}
	}
	
	private void setHeadAndPropageate(Node node, int propagate) {
		Node h = head;
		setHead(node);
		
		if(propagate > 0 || h == null || h.waitStatus < 0) {
			Node s = node.next;
			if(s == null || s.isShared()) {
				doReleaseShared();
			}
		}
	}
	
	private void cancelAcquire(Node node) {
		if(node == null) {
			return ;
		}
		
		node.thread = null;
		
		Node pred = node.prev;
		while(pred.waitStatus > 0) {
			node.prev = pred = pred.prev;
			Node predNext = pred.next;
			node.waitStatus = Node.CANCELLED;
			
			if(node == tail && compareAndSetTail(node, pred)) {
				compareAndSetNext(pred, predNext, null);
			} else {
				int ws;
				if(pred != head && ((ws = pred.waitStatus) == Node.SIGNAL ||
						(ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) && 
						pred.thread != null) {
					Node next = node.next;
					if(next != null && next.waitStatus <= 0) {
						compareAndSetNext(pred, predNext, next);
					} else {
						unparkSuccessor(node);
					}
				}
				
				node.next = node;
			}
		}
	}
	
	private static boolean shouldParAfterFailedAcquire(Node pred, Node node) {
		int ws = pred.waitStatus;
		
		if(ws == Node.SIGNAL) {
			return true;
		} 
		
		if(ws > 0) {
			do {
				node.prev = pred = pred.prev;
			} while (pred.waitStatus > 0);
			pred.next = node;
		} else {
			compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
		}
		return false;
	}
	
	private static void selfInterrupt() {
		Thread.currentThread().interrupt();
	}
	
	private final boolean parkAndCheckInterrupt() {
		LockSupport.park(this);
		return Thread.interrupted();
	}
	
	final boolean acquireQueued(final Node node, int arg) {
		try {
			boolean interrupted = false;
			for(;;) {
				final Node p = node.predecccessor();
				if(p == head && tryAcquire(arg)) {
					setHead(node);
					p.next = null;
					return interrupted;
				}
				
				if(shouldParAfterFailedAcquire(p, node)
						&& parkAndCheckInterrupt())
					interrupted = true;
			}
		} catch (RuntimeException e) {
			cancelAcquire(node);
			throw e;
		}
		
	}
	
	private void doAcquireInterruptibly(int arg) throws InterruptedException {
		 
		final Node node = addWaiter(Node.EXCLUSIVE);
		try {
			for(;;) {
				final Node p = node.predecccessor();
				if(p == head && tryAcquire(arg)) {
					setHead(node);
					p.next = null;
					return ;
				}
				
				if(shouldParAfterFailedAcquire(p, node) && 
						parkAndCheckInterrupt()) {
					break;
				}
			}
		} catch (RuntimeException e) {
			cancelAcquire(node);
			throw e;
		}
		cancelAcquire(node);
		throw new InterruptedException();
	}
	
	private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
		long lastTime = System.nanoTime();
		final Node node = addWaiter(Node.EXCLUSIVE);
		
		for(;;) {
			final Node p = node.predecccessor();
			if(p == head && tryAcquire(arg)) {
				setHead(node);
				p.next = null;
				return true;
			}
			if(nanosTimeout <= 0) {
				cancelAcquire(node);
				return false;
			}
			if(nanosTimeout > spinForTimeoutThresholf && 
					shouldParAfterFailedAcquire(p, node)) {
				LockSupport.parkNanos(this, nanosTimeout);
			}
			
			long now = System.nanoTime();
			nanosTimeout -= now - lastTime;
			if(Thread.interrupted()) {
				break;
			}
			
		}
		cancelAcquire(node);
		throw new InterruptedException();
	}
	
	private void doAcquireShared(int arg) {
		final Node node = addWaiter(Node.SHARED);
		
		try {
			boolean interrupted = false;
			for(;;) {
				final Node p = node.predecccessor();
				if(p == head) {
					int r = tryAcquireShared(arg);
					if(r >= 0) {
						setHeadAndPropageate(node, r);
						p.next = null;
						if(interrupted) {
							selfInterrupt();
						}
						return ;
					}
					
					if(shouldParAfterFailedAcquire(p, node) &&
							parkAndCheckInterrupt()) {
						interrupted = true;
					}
				}
			}  
		} catch (RuntimeException e) {
			cancelAcquire(node);
			throw e;
		}
	}
	
	private void doAcquireSharedInterruptibly(int arg) throws InterruptedException {
		final Node node = addWaiter(Node.SHARED);
		
		try {
			for(;;) {
				final Node p = node.predecccessor();
				if(p == head) {
					int r = tryAcquireShared(arg);
					if(r >= 0) {
						setHeadAndPropageate(node, r);
						p.next = null;
						return ;
					}
				}
				
				if(shouldParAfterFailedAcquire(p, node) &&
						parkAndCheckInterrupt()) {
					break;
				}
			}
		} catch(RuntimeException e) {
			cancelAcquire(node);
			throw e;
		}
		cancelAcquire(node);
		throw new InterruptedException();
	}
	
	private boolean doAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
		
		long lastTime = System.nanoTime();
		final Node node = addWaiter(Node.SHARED);
		
		for(;;) {
			final Node p = node.predecccessor();
			if(p == head) {
				int r = tryAcquireShared(arg);
				if(r >= 0) {
					setHeadAndPropageate(node, r);
					p.next = null;
					return true;
				}
			}
			
			if(nanosTimeout <= 0) {
				cancelAcquire(node);
				return false;
			}
			
			if(nanosTimeout > spinForTimeoutThresholf &&
					shouldParAfterFailedAcquire(p, node)) {
				LockSupport.parkNanos(this, nanosTimeout);
			}
			
			long now = System.nanoTime();
			nanosTimeout -= now - lastTime;
			lastTime = now;
			if(Thread.interrupted()) {
				break;
			}
		}
		cancelAcquire(node);
		throw new InterruptedException();
	}
	
	// 排它的获取这个状态。这个方法的实现需要查询当前状态是否允许获取，然后再进行获取（使用compareAndSetState来做）状态。
	protected boolean tryAcquire(int arg) {
		throw new UnsupportedOperationException();
	}
	
	// 释放状态
	protected boolean tryRelease(int arg) {
		throw new UnsupportedOperationException();
	}
	
	// 共享的模式下获取状态
	protected int tryAcquireShared(int arg) {
		throw new UnsupportedOperationException();
	}
	
	// 共享的模式下释放状态
	protected boolean tryReleaseShared(int arg) {
		throw new UnsupportedOperationException();
	}
	
	// 在排它模式下，状态是否被占用
	protected boolean isHeldExclusively() {
		throw new UnsupportedOperationException();
	}
	
	public final void acquire(int arg) {
		if(!tryAcquire(arg) && 
				acquireQueued(addWaiter(Node.EXCLUSIVE), arg)) {
			selfInterrupt();
		}
	}
	
	public final void acquireInterrutibly(int arg) throws InterruptedException {
		if(Thread.interrupted())
			throw new InterruptedException();
		if(!tryAcquire(arg)) {
			doAcquireInterruptibly(arg);
		}
	}
	
	public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
		if(Thread.interrupted()) {
			throw new InterruptedException();
		}
		
		return tryAcquire(arg) || doAcquireNanos(arg, nanosTimeout);
	}
	
	public final boolean release(int arg) {
		if(tryRelease(arg)) {
			Node h = head;
			if(h != null && h.waitStatus != 0) {
				unparkSuccessor(h);
			}
			return true;
		}
		return false;
	}
	
	public final void acquireShared(int arg) {
		 if (tryAcquireShared(arg) < 0) {
			 doAcquireShared(arg);
		 }
	}
	
	public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
		if(Thread.interrupted()) {
			throw new InterruptedException();
		}
		if(tryAcquireShared(arg) < 0) {
			doAcquireSharedInterruptibly(arg);
		}
	}
	
	public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
		if(Thread.interrupted()) {
			throw new InterruptedException();
		}
		
		return tryAcquireShared(arg) >= 0 || doAcquireSharedNanos(arg, nanosTimeout);
	}
	
	public final boolean releaseShared(int arg) {
		if(tryReleaseShared(arg)) {
			doReleaseShared();
			return true;
		}
		return false;
	}
	
	public final boolean hasQueuedThreads() {
		return head != tail;
	}
	
	public final boolean hasContended() {
		return head != null;
	}
	
	public final Thread getFirstQueuedThread() {
		return (head == tail) ? null : fullGetFirstQueuedThread();
	}
	
	private Thread fullGetFirstQueuedThread() {
		Node h, s;
		Thread st;
		
		if(((h = head) != null && (s = h.next) != null && 
				s.prev == head && (st = s.thread) != null || 
				((h = head) != null && (s = h.next) != null && 
				s.prev == head && (st = s.thread) != null))) {
			return st;
		}
		
		Node t = tail;
		Thread firstThrad = null;
		while(t != null && t != head) {
			Thread tt = t.thread;
			if(tt != null) {
				firstThrad = tt;
			}
			t = t.prev;
		}
		
		return firstThrad;
	}
	
	public final boolean isQueued(Thread thread) {
		if(thread == null) {
			throw new NullPointerException();
		}
		
		for(Node p = tail; p != null; p = p.prev) {
			if(p.thread == thread) {
				return true;
			}
		}
		return false;
	}
	
	final boolean apparentlyFirstQueuedIsExclusive() {
		Node h, s;
		return ((h = head) != null && (s = h.next) != null &&
				s.nextWaiter != Node.SHARED);
	}
	
	final boolean isFirst(Thread current) {
		Node h, s;
		return ((h = head) == null || 
				((s = h.next) != null && s.thread == current) ||
				fullIsFirst(current));
	}
	
	final boolean fullIsFirst(Thread current) {
		Node h, s;
		Thread firstThread = null;
		
		if(((h = head) != null && (s = h.next) != null &&
				s.prev == head && (firstThread = s.thread) != null))
			return firstThread == current;
		
		Node t = tail;
		while(t != null && t != head) {
			Thread tt = t.thread;
			if(tt != null) {
				firstThread = tt;
			}
			t = t.prev;
		}
		
		return firstThread == current || firstThread == null;
	}
	
	public final int getQueueLength() {
		int n = 0;
		for(Node p = tail; p != null; p = p.prev) {
			if(p.thread != null) {
				++n;
			}
		}
		return n;
	}
	
	public final Collection<Thread> getQueuedThreads() {
		ArrayList<Thread> list = new ArrayList<Thread>();
		for(Node p = tail; p != null; p = p.prev) {
			Thread t = p.thread;
			if(t != null) {
				list.add(t);
			}
		}
		return list;
	}
	
	public final Collection<Thread> getExclusiveQueuedThreads() {
		ArrayList<Thread> list = new ArrayList<Thread>();
		for(Node p = tail; p != null; p = p.prev) {
			if(!p.isShared()) {
				Thread t = p.thread;
				if(t != null) {
					list.add(t);
				}
			}
		}
		return list;
	}
	
	public final Collection<Thread> getSharedQueuedThreads() {
		ArrayList<Thread> list = new ArrayList<Thread>();
		for(Node p = tail; p != null; p = p.prev) {
			if(p.isShared()) {
				Thread t = p.thread;
				if(t != null) {
					list.add(t);
				}
			}
		}
		return list;
	}
	
	public String toString() {
        int s = getState();
        String q  = hasQueuedThreads()? "non" : "";
        return super.toString() +
            "[State = " + s + ", " + q + "empty queue]";
    }
	
	final boolean isOnSysncQueue(Node node) {
		if(node.waitStatus == Node.CONDITION || node.prev == null) {
			return false;
		}
		
		if(node.next != null) {
			return true;
		}
		
		return findNodeFromTail(node);
	}
	
	private boolean findNodeFromTail(Node node) {
		Node t = tail;
		for(;;) {
			if(t == node) {
				return true;
			}
			if(t == null) {
				return false;
			}
			t = t.prev;
		}
	}
	
	final boolean transferForSignal(Node node) {
		if(!compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
			return false;
		}
		
		Node p = enq(node);
		int ws = p.waitStatus;
		if(ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL)) {
			LockSupport.unpark(node.thread);
		}
		
		return true;
	}
	
	final boolean transferAfterCancelledWait(Node node) {
		if(compareAndSetWaitStatus(node, Node.CANCELLED, 0)) {
			enq(node);
			return true;
		}
		
		while(!isOnSysncQueue(node)) {
			Thread.yield();
		}
		
		return false;
	}
	
	final int fullyRelease(Node node) {
		try {
			int savedState = getState();
			if(release(savedState)) {
				return savedState;
			}
		} catch (RuntimeException e) {
			node.waitStatus = Node.CANCELLED;
			throw e;
		}
		node.waitStatus = Node.CANCELLED;
		throw new IllegalMonitorStateException();
	}
	
	public final boolean owns(ConditionObject condition) {
		if(condition == null) {
			throw new NullPointerException();
		}
		return condition.isOwnedBy(this);
	}
	
	public final boolean hasWaiters(ConditionObject condition) {
		if(!owns(condition)) {
			 throw new IllegalArgumentException("Not owner");
		}
		return condition.hasWaiters();
	}
	
	public final int getWaitQueueLength(ConditionObject condition) {
		if(!owns(condition)) {
			throw new IllegalArgumentException("Not owner");
		}
		return condition.getWaitQueueLength();
	}
	
	public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
		if(!owns(condition)) {
			throw new IllegalArgumentException("Not owner");
		}
		return condition.getWaitingThreads();
	}
	
	public class ConditionObject implements Condition, Serializable {

		private static final long serialVersionUID = 1L;
		
		private transient Node firstWaiter;
		private transient Node lastWaiter;
		
		public ConditionObject() {}
		
		private Node addConditionWaiter() {
			Node t = lastWaiter;
			if(t != null && t.waitStatus != Node.CANCELLED) {
				unlinkCancelledWaiters();
				t = lastWaiter;
			}
			
			Node node = new Node(Thread.currentThread(), Node.CONDITION);
			
			if(t == null) {
				firstWaiter = node;
			} else {
				t.nextWaiter = node;
			}
			lastWaiter = node;
			return node;
		}
		
		private void doSingnal(Node first) {
			do {
				if((firstWaiter = first.nextWaiter) == null) {
					lastWaiter = null;
				}
				first.nextWaiter = null;
			} while(!transferForSignal(first) && (first = firstWaiter) != null);
		}
		
		@SuppressWarnings("unused")
		private void doSignalAll(Node first) {
			lastWaiter = firstWaiter = null;
			do {
				Node next = first.nextWaiter;
				first.nextWaiter = null;
				transferForSignal(first);
				first = next;
			} while(first != null);
		}
		
		private void unlinkCancelledWaiters() {
			Node t = firstWaiter;
			Node trail = null;
			while(t != null) {
				Node next = t.nextWaiter;
				if(t.waitStatus != Node.CONDITION) {
					t.nextWaiter = null;
					if(trail == null) {
						firstWaiter = next;
					} else {
						trail.nextWaiter = next;
					}
					if(next == null) {
						lastWaiter = trail;
					}
				} else {
					trail = t;
				}
				t = next;
			}
		}

		@Override
		public void signal() {
			if(!isHeldExclusively()) {
				throw new IllegalMonitorStateException();
			}
			Node first = firstWaiter;
			if(first != null) {
				doSingnal(first);
			}
		}

		@Override
		public void signalAll() {
			if(!isHeldExclusively()) {
				throw new IllegalMonitorStateException();
			}
			Node first = firstWaiter;
			if(first != null) {
				doSingnal(first);
			}
		}
		
		@Override
		public final void awaitUninterruptibly() {
			Node node = addConditionWaiter();
			int savedState = fullyRelease(node);
			boolean interrupted = false;
			while(!isOnSysncQueue(node)) {
				LockSupport.park(this);
				if(Thread.interrupted()) {
					interrupted = true;
				}
			}
			if(acquireQueued(node, savedState) || interrupted) {
				selfInterrupt();
			}
		}
		
		private static final int REINTERRUPT = 1;
		private static final int THROW_IE = -1;

		private int checkInterruptWhileWating(Node node) {
			return (Thread.interrupted()) ? ((transferAfterCancelledWait(node))? THROW_IE : REINTERRUPT) : 0;
		}
		
		private void reportInterruptAfterWait(int interruptMode) throws InterruptedException {
			if(interruptMode == THROW_IE) {
				throw new InterruptedException();
			} else if(interruptMode == REINTERRUPT) {
				selfInterrupt();
			}
		}
		
		@Override
		public void await() throws InterruptedException {
			if(Thread.interrupted()) {
				throw new InterruptedException();
			}
			
			Node node = addConditionWaiter();
			int saveedState = fullyRelease(node);
			int interruptMode = 0;
			
			while(!isOnSysncQueue(node)) {
				LockSupport.park(this);
				if((interruptMode = checkInterruptWhileWating(node)) != 0) {
					break;
				}
			}
			
			if(acquireQueued(node, saveedState) && interruptMode != THROW_IE) {
				interruptMode = REINTERRUPT;
			}
			if(node.nextWaiter != null) {
				unlinkCancelledWaiters();
			}
			if(interruptMode != 0) {
				reportInterruptAfterWait(interruptMode);
			}
		}

		
		@Override
		public long awaitNanos(long nanosTimeout) throws InterruptedException {
			Node node = addConditionWaiter();
			int savedState = fullyRelease(node);
			long lastTime = System.nanoTime();
			int interruptMode = 0;
			
			while(!isOnSysncQueue(node)) {
				if(nanosTimeout <= 0L) {
					transferAfterCancelledWait(node);
					break;
				}
				LockSupport.parkNanos(this, nanosTimeout);
				if((interruptMode = checkInterruptWhileWating(node)) != 0) {
					break;
				}
				
				long now = System.nanoTime();
				nanosTimeout -= now - lastTime;
				lastTime = now;
			}
			
			if(acquireQueued(node, savedState) && interruptMode != THROW_IE) {
				interruptMode = REINTERRUPT;
			}
			if(node.nextWaiter != null) {
				unlinkCancelledWaiters();
			}
			if(interruptMode != 0) {
				reportInterruptAfterWait(interruptMode);
			}
			
			return nanosTimeout - (System.nanoTime() - lastTime);
		}

		@Override
		public boolean awaitUntil(Date deadline) throws InterruptedException {
			if(deadline == null) {
				throw new NullPointerException();
			}
			
			long abstime = deadline.getTime();
			if(Thread.interrupted()) {
				throw new InterruptedException();
			}
			
			Node node = addConditionWaiter();
			int savedState = fullyRelease(node);
			boolean timedout = false;
			int interruptMode = 0;
			
			while(!isOnSysncQueue(node)) {
				if(System.currentTimeMillis() > abstime) {
					timedout = transferAfterCancelledWait(node);
					break;
				}
				
				LockSupport.parkUntil(this, abstime);
				if((interruptMode = checkInterruptWhileWating(node)) != 0) {
					break;
				}
			}
			
			if(acquireQueued(node, savedState) && interruptMode != THROW_IE) {
				interruptMode = REINTERRUPT;
			}
			
			if(node.nextWaiter != null) {
				unlinkCancelledWaiters();
			}
			
			if(interruptMode != 0) {
				reportInterruptAfterWait(interruptMode);
			}
			
			return !timedout;
		}

		
		@Override
		public boolean await(long time, TimeUnit unit)
				throws InterruptedException {
			
			if(unit == null) {
				throw new NullPointerException();
			}
			
			long nanosTimeout = unit.toNanos(time);
			if(Thread.interrupted()) {
				throw new InterruptedException();
			}
			
			Node node = addConditionWaiter();
			int savedState = fullyRelease(node);
			long lastTime = System.nanoTime();
			boolean timedout = false;
			int interruptMode = 0;
			
			while(!isOnSysncQueue(node)) {
				if(nanosTimeout <= 0L) {
					timedout = transferAfterCancelledWait(node);
					break;
				}
				
				LockSupport.parkNanos(this, nanosTimeout);
				
				if((interruptMode = checkInterruptWhileWating(node)) != 0) {
					break;
				}
				
				long now = System.nanoTime();
				nanosTimeout -= now - lastTime;
				lastTime = now;
			}
			
			if(acquireQueued(node, savedState) && interruptMode != THROW_IE) {
				interruptMode = REINTERRUPT;
			}
			if(node.nextWaiter != null) {
				unlinkCancelledWaiters();
			}
			
			if(interruptMode != 0) {
				reportInterruptAfterWait(interruptMode);
			}
			
			return !timedout;
		}
		
		final boolean isOwnedBy(AbstractOwnableSynchronizer sync) {
			return sync == AbstractQueuedSynchronizer.this;
		}
		
		protected final boolean hasWaiters() {
			if(!isHeldExclusively()) {
				throw new IllegalMonitorStateException();
			}
			for(Node w = firstWaiter; w != null; w = w.nextWaiter) {
				if(w.waitStatus == Node.CONDITION) {
					return true;
				}
			}
			return false;
		}
		
		protected final int getWaitQueueLength() {
			if(!isHeldExclusively()) {
				throw new IllegalMonitorStateException();
			}
			int n = 0;
			for(Node w = firstWaiter; w != null; w = w.nextWaiter) {
				if(w.waitStatus == Node.CONDITION) {
					++n;
				}
			}
			return n;
		}
		
		protected final Collection<Thread> getWaitingThreads() {
			if(!isHeldExclusively()) {
				throw new IllegalMonitorStateException();
			}
			
			ArrayList<Thread> list = new ArrayList<Thread>();
			for(Node w = firstWaiter; w != null; w = w.nextWaiter) {
				if(w.waitStatus == Node.CONDITION) {
					Thread t = w.thread;
					if(t != null) {
						list.add(t);
					}
				}
			}
			return list;
		}

	}
	
	private static final Unsafe unsafe = Unsafe.getUnsafe();
	private static final long stateOffset;
	private static final long headOffset;
	private static final long tailOffset;
	private static final long waitStatusOffset;
	private static final long nextOffset;
	
	static {
		try {
			stateOffset = unsafe.objectFieldOffset(AbstractOwnableSynchronizer.class.getDeclaredField("state"));
			headOffset = unsafe.objectFieldOffset(AbstractOwnableSynchronizer.class.getDeclaredField("head"));
			tailOffset = unsafe.objectFieldOffset(AbstractOwnableSynchronizer.class.getDeclaredField("tail"));
			waitStatusOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("waitStatus"));
			nextOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("next"));
			
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	private final boolean compareAndSetHead(Node update) {
		return unsafe.compareAndSwapObject(this, headOffset, null, update);
	}
	
	private final boolean compareAndSetTail(Node expect, Node update) {
		return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
	}
	
	private final static boolean compareAndSetWaitStatus(Node node, int expect, int update) {
		return unsafe.compareAndSwapInt(node, waitStatusOffset, expect, update);
	}
	
	private final static boolean compareAndSetNext(Node node, Node expect, Node update) {
		return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
	}
	
	
}
