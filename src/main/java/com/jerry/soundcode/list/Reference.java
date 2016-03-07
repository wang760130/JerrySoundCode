package com.jerry.soundcode.list;

public abstract class Reference<T> {

	private T referent;
	
	ReferenceQueue<? super T> queue;
	
	Reference next;
	
	transient private Reference<T> discovered;
	
	static private class Lock{};
	
	private static Lock lock = new Lock();
	
	private static Reference pending = null;
	
	private static class ReferenceHandler extends Thread {
		
		ReferenceHandler(ThreadGroup g, String name) {
			super(g, name);
		}
		
		@Override
		public void run() {
			for(;;) {
				Reference r;
				
				synchronized (lock) {
					if(pending != null) {
						r = pending;
						Reference rn = r.next;
						pending = (rn == r) ? null : rn;
						r.next = r;
					} else {
						try {
							lock.wait();
						} catch (InterruptedException e) {
						}
						continue;
					}
				}
				
				// Fast path for cleaners
//				if(r instanceof sun.misc.Cleaner) {
//					((sun.misc.Cleaner) r).clean();
//					continue;
//				}
				
				ReferenceQueue q = r.queue;
				if(q != ReferenceQueue.NULL) {
					q.enqueue(r);
				}
			}
		}
	}
	
	static {
		ThreadGroup tg = Thread.currentThread().getThreadGroup();
		for(ThreadGroup tgn = tg; tgn != null; tg = tgn, tgn = tg.getParent()) {
			Thread handler = new ReferenceHandler(tg, "Reference Handler");
			
			handler.setPriority(Thread.MAX_PRIORITY);
			handler.setDaemon(true);
			handler.start();
		}
	}
	
	public T get() {
		return this.referent;
	}
	
	public void clear() {
		this.referent = null;
	}
	
	public boolean isEnqueued() {
		synchronized (this) {
			return (this.queue != ReferenceQueue.NULL) && (this.next != null);
		}
	}
	
	public boolean enqueue() {
		return this.queue.enqueue(this);
	}
	
	public Reference(T referent) {
		this(referent, null);
	}

	Reference(T referent2, ReferenceQueue<? extends T> queue) {
		this.referent = referent;
		this.queue = (queue == null) ? ReferenceQueue.NULL : queue;
	}
}
