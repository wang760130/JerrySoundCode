package com.jerry.soundcode.concurrent.locks;

import java.io.Serializable;

import com.jerry.soundcode.concurrent.atomic.Unsafe;

public abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer implements Serializable {

	private static final long serialVersionUID = 1L;

	protected AbstractQueuedSynchronizer() {}
	
	static final class Node {
		static final int CANCELLED = 1;
		
		static final int SIGNAL = -1;
		
		static final int CONDITION = -2;
		
		static final int PROPAGATE = -3;
		
		static final Node SHARED = new Node();
		
		static final Node EXCLUSIVE = null;
		
		volatile int waitStatus;
		
		volatile Node prev;
		
		volatile Node next;
		
		volatile Thread thread;
		
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
	
	private Node adWaiter(Node mode) {
		Node node = new Node(Thread.currentThread(), mode);			
		
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
	
	private final static boolean compareAndSetNex(Node node, Node expect, Node update) {
		return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
	}
	
	
}
