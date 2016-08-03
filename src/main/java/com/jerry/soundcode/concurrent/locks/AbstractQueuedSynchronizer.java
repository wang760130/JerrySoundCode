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
	
	// 保存着线程引用和线程状态的容器
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
	
	// 共享资源
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
	
	/**
	 * 1. 使用当前线程构造Node；
	 * 对于一个节点需要做的是将当节点前驱节点指向尾节点（current.prev = tail），尾节点指向它（tail = current），
	 * 原有的尾节点的后继节点指向它（t.next = current）而这些操作要求是原子的。上面的操作是利用尾节点的设置来保证的，也就是compareAndSetTail来完成的。
	 * 2. 先行尝试在队尾添加；
	 * 如果尾节点已经有了，然后做如下操作：
	 * (1)分配引用T指向尾节点；
	 * (2)将节点的前驱节点更新为尾节点（current.prev = tail）；
	 * (3)如果尾节点是T，那么将当尾节点设置为该节点（tail = current，原子更新）；
	 * (4)T的后继节点指向当前节点（T.next = current）。
	 * 注意第3点是要求原子的。
	 * 这样可以以最短路径O(1)的效果来完成线程入队，是最大化减少开销的一种方式。
	 * 3. 如果队尾添加失败或者是第一个入队的节点。
	 * 如果是第1个节点，也就是sync队列没有初始化，那么会进入到enq这个方法，进入的线程可能有多个，或者说在addWaiter中没有成功入队的线程都将进入enq这个方法。
	 * 可以看到enq的逻辑是确保进入的Node都会有机会顺序的添加到sync队列中，而加入的步骤如下：
	 * (1)如果尾节点为空，那么原子化的分配一个头节点，并将尾节点指向头节点，这一步是初始化；
	 * (2)然后是重复在addWaiter中做的工作，但是在一个while(true)的循环中，直到当前节点入队为止。
	 * 进入sync队列之后，接下来就是要进行锁的获取，或者说是访问控制了，只有一个线程能够在同一时刻继续的运行，而其他的进入等待状态。而每个线程都是一个独立的个体，它们自省的观察，当条件满足的时候（自己的前驱是头结点并且原子性的获取了状态），那么这个线程能够继续运行。
	 */
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
	
	/**
	 * 该方法取出了当前节点的next引用，然后对其线程(Node)进行了唤醒，这时就只有一个或合理个数的线程被唤醒，被唤醒的线程继续进行对资源的获取与争夺。
	 * 回顾整个资源的获取和释放过程：
	 * 在获取时，维护了一个sync队列，每个节点都是一个线程在进行自旋，而依据就是自己是否是首节点的后继并且能够获取资源；
	 * 在释放时，仅仅需要将资源还回去，然后通知一下后继节点并将其唤醒。
	 * 这里需要注意，队列的维护（首节点的更换）是依靠消费者（获取时）来完成的，也就是说在满足了自旋退出的条件时的一刻，这个节点就会被设置成为首节点。
	 */
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
	
	/**
	 * 1. 获取当前节点的前驱节点；
	 * 需要获取当前节点的前驱节点，而头结点所对应的含义是当前站有锁且正在运行。
	 * 2. 当前驱节点是头结点并且能够获取状态，代表该当前节点占有锁；
	 * 如果满足上述条件，那么代表能够占有锁，根据节点对锁占有的含义，设置头结点为当前节点。
	 * 3. 否则进入等待状态。
	 * 如果没有轮到当前节点运行，那么将当前线程从线程调度器上摘下，也就是进入等待状态。
	 * 这里针对acquire做一下总结：
	 * 1. 状态的维护；
	 * 需要在锁定时，需要维护一个状态(int类型)，而对状态的操作是原子和非阻塞的，通过同步器提供的对状态访问的方法对状态进行操纵，并且利用compareAndSet来确保原子性的修改。
	 * 2. 状态的获取；
	 * 一旦成功的修改了状态，当前线程或者说节点，就被设置为头节点。
	 * 3. sync队列的维护。
	 * 在获取资源未果的过程中条件不符合的情况下(不该自己，前驱节点不是头节点或者没有获取到资源)进入睡眠状态，停止线程调度器对当前节点线程的调度。
	 * 这时引入的一个释放的问题，也就是说使睡眠中的Node或者说线程获得通知的关键，就是前驱节点的通知，而这一个过程就是释放，释放会通知它的后继节点从睡眠中返回准备运行。
	 */
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
	
	// 释放锁，将状态设置为0
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
	
	/**
	 * 1. 尝试获取（调用tryAcquire更改状态，需要保证原子性）；
	 * 在tryAcquire方法中使用了同步器提供的对state操作的方法，利用compareAndSet保证只有一个线程能够对状态进行成功修改，而没有成功修改的线程将进入sync队列排队。
	 * 2. 如果获取不到，将当前线程构造成节点Node并加入sync队列；
	 * 进入队列的每个线程都是一个节点Node，从而形成了一个双向队列，类似CLH队列，这样做的目的是线程间的通信会被限制在较小规模（也就是两个节点左右）。
	 * 3. 再次尝试获取，如果没有获取到那么将当前线程从线程调度器上摘下，进入等待状态。
	 */
	public final void acquire(int arg) {
		if(!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg)) {
			// 尝试获取（调用tryAcquire更改状态，需要保证原子性）
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
	
	/**
	 * 在unlock方法的实现中，使用了同步器的release方法。相对于在之前的acquire方法中可以得出调用acquire，
	 * 保证能够获取到锁（成功获取状态），而release则表示将状态设置回去，也就是将资源释放，或者说将锁释放。
	 * 
	 * 1. 尝试释放状态；
	 * tryRelease能够保证原子化的将状态设置回去，当然需要使用compareAndSet来保证。如果释放状态成功过之后，将会进入后继节点的唤醒过程。
	 * 2. 唤醒当前节点的后继节点所包含的线程。
	 * 通过LockSupport的unpark方法将休眠中的线程唤醒，让其继续acquire状态
	 */
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
	
	/**
	 * 该方法提供获取状态能力，当然在无法获取状态的情况下会进入sync队列进行排队，这类似acquire，
	 * 但是和acquire不同的地方在于它能够在外界对当前线程进行中断的时候提前结束获取状态的操作，
	 * 换句话说，就是在类似synchronized获取锁时，外界能够对当前线程进行中断，并且获取锁的这个操作能够响应中断并提前返回。
	 * 一个线程处于synchronized块中或者进行同步I/O操作时，对该线程进行中断操作，这时该线程的中断标识位被设置为true，但是线程依旧继续运行。
	 * 如果在获取一个通过网络交互实现的锁时，这个锁资源突然进行了销毁，
	 * 那么使用acquireInterruptibly的获取方式就能够让该时刻尝试获取锁的线程提前返回。
	 * 而同步器的这个特性被实现Lock接口中的lockInterruptibly方法。
	 * 根据Lock的语义，在被中断时，lockInterruptibly将会抛出InterruptedException来告知使用者。
	 */
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
