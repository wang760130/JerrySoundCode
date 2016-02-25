package com.jerry.soundcode.concurrent.locks;

import java.io.Serializable;

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
	

}
