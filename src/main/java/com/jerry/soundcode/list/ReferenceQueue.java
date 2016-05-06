package com.jerry.soundcode.list;

import com.jerry.soundcode.ref.FinalReference;
import com.jerry.soundcode.ref.Reference;

public class ReferenceQueue<T> {
	
	public ReferenceQueue() {}
	
	@SuppressWarnings("rawtypes")
	private static class Null extends ReferenceQueue {
		@SuppressWarnings("unused")
		boolean enqueue(ReferenceQueue r) {
			return false;
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static ReferenceQueue NULL  = new Null();
	@SuppressWarnings("rawtypes")
	static ReferenceQueue ENQUEUED = new Null();
	
	static private class Lock {};
	
	private Lock lock = new Lock();
	private volatile Reference<? extends T> head = null;
	private long queueLength = 0;
	
	@SuppressWarnings({ "rawtypes", "restriction", "unchecked" })
	public boolean enqueue(Reference r) {
		synchronized (r) {
			if(r.queue == ENQUEUED) {
				return false;
			}
			
			synchronized (lock) {
				r.queue = ENQUEUED;
				r.next = (head == null) ? r : head;
				head = r;
				queueLength ++;
				if(r instanceof FinalReference) {
//					sun.misc.VM.addFinalRefCount(1);
				}
				lock.notifyAll();
				return true;
			}
				
		}
	}
	
	@SuppressWarnings({ "unchecked", "restriction" })
	private Reference<? extends T> reallyPoll() {
		if(head != null) {
			Reference<? extends T> r = head;
			head = (r.next == r) ? null : r.next;
			r.queue = NULL;
			r.next = r;
			queueLength--;
			if(r instanceof FinalReference) {
//				sun.misc.VM.addFinalRefCount(-1);
			}
			return r;
		}
		return null;
	}
	
	public Reference<? extends T> poll() {
		if(head == null) {
			return null;
		}
		synchronized (lock) {
			return reallyPoll();
		}
	}
	
	public Reference<? extends T> remove(long timeout) throws InterruptedException {
		if(timeout < 0) {
			throw new IllegalArgumentException("Negative timeout value");
		}
		
		synchronized (lock) {
			Reference<? extends T> r = reallyPoll();
			if(r != null) 
				return r;
			for(;;) {
				lock.wait(timeout);
				r = reallyPoll();
				if(r != null) 
					return r;
				if(timeout != 0)
					return null;
			}
		}
	}
	
	public Reference<? extends T> remove() throws InterruptedException {
		return remove(0);
	}
}
