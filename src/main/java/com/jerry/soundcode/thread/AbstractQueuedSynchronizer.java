package com.jerry.soundcode.thread;


import com.jerry.soundcode.concurrent.atomic.Unsafe;
import com.jerry.soundcode.concurrent.locks.LockSupport;

public class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer {

	private static final long serialVersionUID = 1L;

	protected AbstractQueuedSynchronizer() {}
	
	static final class Node {
		static final int CANCELLED = 1;
		static final int SIGNAL = -1;
		static final int CONDITION = -2;
		
		static final int PROPAGETE = -3;
		static final Node SHARED = new Node();
		
		static final Node EXCLUSIVE = null;
		
		volatile int waitStatus;
		
		volatile Node prev;
		
		volatile Node next;
		
		volatile Thread thread;
		
		Node nextWaiter;
		
		final boolean isShared() {
			return nextWaiter == SHARED;
		}
		
		final Node predecessor() throws NullPointerException {
			Node p = prev;
			if(p == null) {
				throw new NullPointerException();
			} else {
				return p;
			}
		}
		
		Node() {}
		
		Node(Thread thread, Node mode) {
			this.nextWaiter = mode;
			this.thread = thread;
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
	
	static final long spinForTimeoutThreshold = 1000L;
	
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
		Node node  = new Node(Thread.currentThread(), mode);
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
					if(!compareAndSetWaitStatus(h, Node.SIGNAL, 0)) {
						continue;
					}
					unparkSuccessor(h);
				} else if(ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGETE)) {
					continue;
				}
			}
			
			if(h == head) {
				break;
			}
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
            stateOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("next"));
        } catch (Exception ex) { throw new Error(ex); }
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
