package com.jerry.soundcode.concurrent.collection;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import com.jerry.soundcode.concurrent.locks.LockSupport;
import com.jerry.soundcode.list.AbstractQueue;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.Iterator;
import com.jerry.soundcode.thread.Thread;

public class SynchronousQueue<E> extends AbstractQueue<E> 
	implements BlockingQueue<E>, Serializable {

	private static final long serialVersionUID = 1L;

	static abstract class Transferer {
		abstract Object transfer(Object e, boolean timed, long nanos);
	}
	
	static final int NCPUS = Runtime.getRuntime().availableProcessors();
	
	static final int maxTimedSpins = (NCPUS < 2 ) ? 0 : 32;
	
	static final int maxUntimeSpins = maxTimedSpins * 16;
	
	static final long spinForTimeoutThreshold = 1000L;
	
	static final class TransferStack extends Transferer {
		
		static final int REQUEST = 0;
		static final int DATA = 1;
		static final int FULFILLING = 2;
		
		static boolean isFulfilling(int m) {
			return (m & FULFILLING) != 0;
		}
		
		static final class SNode {
			volatile SNode next;
			volatile SNode match;
			volatile Thread waiter;
			
			Object item;
			int mode;
			
			SNode(Object item) {
				this.item = item;
			}
			
			static final AtomicReferenceFieldUpdater<SNode, SNode> nextUpdater = AtomicReferenceFieldUpdater.newUpdater(SNode.class, SNode.class, "next");
				
			boolean casNext(SNode cmp, SNode val) {
				return (cmp == next && nextUpdater.compareAndSet(this, cmp, val));
			}
			
			static final AtomicReferenceFieldUpdater<SNode, SNode> matchUpdater = AtomicReferenceFieldUpdater.newUpdater(SNode.class, SNode.class, "match");
			
			boolean tryMatch(SNode s) {
				if(match == null && matchUpdater.compareAndSet(this, null, s)) {
					Thread w = waiter;
					if(w != null) {
						waiter = null;
						LockSupport.unpark(w);
					}
					return true;
				}
				return match == s;
			}
			
			void tryCancel() {
				matchUpdater.compareAndSet(this, null, this);
			}
			
			boolean isCancelled() {
				return match == this;
			}
		}
		
		volatile SNode head;
		
		static final AtomicReferenceFieldUpdater<TransferStack, SNode> headUpdater = AtomicReferenceFieldUpdater.newUpdater(TransferStack.class, SNode.class, "head");
		
		boolean casHead(SNode h, SNode nh) {
			return h == head && headUpdater.compareAndSet(this, h, nh);
		}
		
		static SNode snode(SNode s, Object e, SNode next, int mode) {
			if(s == null) {
				s = new SNode(e);
			}
			s.mode = mode;
			s.next = next;
			return s;
		}
		
		@Override
		Object transfer(Object e, boolean timed, long nanos) {
			SNode s = null;
			int mode = (e == null) ? REQUEST : DATA;
			
			for(;;) {
				SNode h = head;
				if(h == null || h.mode == mode) {
					if(timed && nanos <= 0) {
						if(h != null && h.isCancelled()) {
							casHead(h, h.next);
						} else {
							return null;
						}
					} else if(casHead(h, s = snode(s, e, h, mode))) {
						SNode m = awaitFulfill(s, timed, nanos);
						if(m == s) {
							clean(s);
							return null;
						}
						if((h = head) != null && h.next == s) {
							casHead(h, s.next);
						}
						return mode == REQUEST ? m.item : s.item;
					}
				} else if(!isFulfilling(h.mode)) {
					if(h.isCancelled()) {
						casHead(h, h.next);
					} else if(casHead(h, s = snode(s, e, h, FULFILLING | mode))) {
						for(;;) {
							SNode m = s.next;
							if(m == null) {
								casHead(s, null);
								s = null;
								break;
							}
							SNode mn = m.next;
							if(m.tryMatch(s)) {
								casHead(s, mn);
								return (mode == REQUEST) ? m.item : s.item;
							} else {
								s.casNext(m, mn);
							}
						}
					}
				} else {
					SNode m = h.next;
					if(m == null) {
						casHead(h, null);
					} else {
						SNode mn = m.next;
						if(m.tryMatch(h)) {
							casHead(h, mn);
						} else {
							h.casNext(m, mn);
						}
					}
				}
			}
		}

		SNode awaitFulfill(SNode s, boolean timed, long nanos) {
			long lastTime = (timed) ? System.nanoTime() : 0;
			Thread w = Thread.currentThread();
			
			SNode h = head;
			int spins = (shouldSpin(s) ? (timed ? maxTimedSpins : maxUntimeSpins) : 0);
			
			for(;;) {
				if(w.isInterrupted()) {
					s.tryCancel();
				}
				
				SNode m = s.match;
				if(m != null) {
					return m;
				}
				
				if(timed) {
					long now = System.nanoTime();
					nanos -= now - lastTime;
					lastTime = now;
					if(nanos <= 0) {
						s.tryCancel();
						continue;
					}
				}
				
				if(spins > 0) {
					spins = shouldSpin(s) ? (spins - 1) : 0;
				} else if(s.waiter == null) {
					s.waiter = w;
				} else if(!timed) {
					LockSupport.park(this);
				} else if(nanos > spinForTimeoutThreshold) {
					LockSupport.parkNanos(this, nanos);
				}
			}
		}
		
		boolean shouldSpin(SNode s) {
			SNode h = head;
			return (h == s || h == null || isFulfilling(h.mode));
		}
		

		private void clean(SNode s) {
			s.item = null;
			s.waiter = null;
			
			SNode past = s.next;
			if(past != null && past.isCancelled()) {
				past = past.next;
			}
			
			SNode p ;
			while((p = head) != null && p != past && p.isCancelled()) {
				casHead(p, p.next);
			}
			
			while(p != null && p != past) {
				SNode n = p.next;
				if(n != null && n.isCancelled()) {
					p.casNext(n, n.next) ;
 				} else {
 					p = n;
 				}
			}
		}
	}
	
	static final class TransferQueue extends Transferer {
		static final class QNode {
			volatile QNode next;
			volatile Object item;
			volatile Thread waiter;
			
			final boolean isData;
			
			QNode(Object item, boolean isData) {
				this.item = item;
				this.isData = isData;
			}
			
			static final AtomicReferenceFieldUpdater<QNode, QNode> nextUpdater = AtomicReferenceFieldUpdater.newUpdater(QNode.class, QNode.class, "next");
			
			boolean casNext(QNode cmp, QNode val) {
				return (next == cmp && nextUpdater.compareAndSet(this, cmp, val));
			}
			
			static final AtomicReferenceFieldUpdater<QNode, Object> itemUpdater = AtomicReferenceFieldUpdater.newUpdater(QNode.class, Object.class, "item");
			
			boolean casItem(Object cmp, Object val) {
				return (item == cmp && itemUpdater.compareAndSet(this, cmp, val));
			}
			
			void tryCancel(Object cmp) {
				itemUpdater.compareAndSet(this, cmp, this);
			}
			
			boolean isOffList() {
				return next == this;
			}
		}
		
		transient volatile QNode head;
		transient volatile QNode tail;
		
		transient volatile QNode cleanMe;
		
		public TransferQueue() {
			QNode h = new QNode(null, false);
			head = h;
			tail = h;
		}
		
		static final AtomicReferenceFieldUpdater<TransferQueue, QNode> headUpdater = AtomicReferenceFieldUpdater.newUpdater(TransferQueue.class, QNode.class, "head");

		void advanceHead(QNode h, QNode nh) {
			if(h == head && headUpdater.compareAndSet(this, h, nh)) {
				h.next = h;
			}
		}
		
		static final AtomicReferenceFieldUpdater<TransferQueue, QNode> tailUpdater = AtomicReferenceFieldUpdater.newUpdater(TransferQueue.class, QNode.class, "tail");
		
		void advanceTail(QNode t, QNode nt) {
			if(tail == t) {
				tailUpdater.compareAndSet(this, t, nt);
			}
		}
		
		
		
		@Override
		Object transfer(Object e, boolean timed, long nanos) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	@Override
	public E poll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public E peek() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean offer(E t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean offer(E t, long timeout, TimeUnit unit)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void put(E t) throws InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public E take() throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int remainingCapacity() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int drainTo(Collection<? super E> c) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Iterator<E> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	
	
}
