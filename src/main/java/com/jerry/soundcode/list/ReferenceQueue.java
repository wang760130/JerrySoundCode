package com.jerry.soundcode.list;

public class ReferenceQueue<T> {
	
	public ReferenceQueue() {}
	
	private static class Null extends ReferenceQueue {
		boolean enqueue(ReferenceQueue r) {
			return false;
		}
	}
	
	static ReferenceQueue NULL  = new Null();
	static ReferenceQueue ENQUEUED = new Null();
	
	static private class Lock {};
	
	private Lock lock = new Lock();
	private volatile Reference<? extends T> head = null;
	private long queueLength = 0;
	
	boolean enqueue(Reference r) {
		synchronized (r) {
//			if(r.queue == ENQUEUED) {
//				return false;
//			}
				
		}
		
		return false;
	}
}
